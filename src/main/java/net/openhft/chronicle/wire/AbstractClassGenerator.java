/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.wire.utils.JavaSourceCodeFormatter;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;
import net.openhft.compiler.CachedCompiler;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.SourceVersion;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract generator for classes at runtime based on meta-data.
 * This class leverages the CachedCompiler to dynamically compile and load classes.
 *
 * @param <M> Represents the meta-data associated with the class being generated.
 */
@SuppressWarnings("unchecked")
public abstract class AbstractClassGenerator<M extends AbstractClassGenerator.MetaData<M>> {

    // TODO Use Wires.loadFromJava() instead of a public static final
    public static final CachedCompiler CACHED_COMPILER = new CachedCompiler(Jvm.isDebug() ? new File(OS.getTarget(), "generated-test-sources") : null, null);

    // Flag to determine if the generated source code should be displayed.
    private static final boolean DUMP_CODE = Jvm.getBoolean("dumpCode");

    // Formatter for generating Java source code.
    protected final SourceCodeFormatter sourceCode = new JavaSourceCodeFormatter();

    // Set of imports to be included in the generated code.
    protected SortedSet<String> importSet = new TreeSet<>();

    // The associated meta-data used for code generation.
    private final M metaData;

    // For some internal purpose; its exact use isn't clarified from the provided code.
    private int maxCode = 6;

    /**
     * Constructor for initializing the generator with its associated meta-data.
     *
     * @param metaData Meta-data related to the class to be generated.
     */
    protected AbstractClassGenerator(M metaData) {
        this.metaData = metaData;
    }

    /**
     * Retrieves the meta-data associated with this generator.
     *
     * @return Meta-data related to the class being generated.
     */
    public M metaData() {
        return metaData;
    }

    /**
     * Attempts to acquire the generated class. If the class is not yet generated or loaded,
     * it will trigger the generation and loading of the class.
     *
     * @param classLoader The class loader to be used for loading the generated class.
     * @param <T> The type of the generated class.
     * @return The generated and loaded class.
     */
    public synchronized <T> Class<T> acquireClass(ClassLoader classLoader) {
        // Full class name (including package) for the generated class.
        String fullName = metaData.packageName() + "." + className();
        try {
            // Try loading the class if it's already been generated and loaded.
            return (Class<T>) classLoader.loadClass(fullName);

        } catch (ClassNotFoundException cnfe) {
            // If not found, continue to generate and compile the class.
        }
        try {
            // If source code hasn't been generated yet, generate it.
            if (sourceCode.length() == 0) {
                SourceCodeFormatter mainCode = new JavaSourceCodeFormatter();
                generateMainCode(mainCode);

                // Append package and import statements.
                sourceCode.append("" +
                        "package " + metaData.packageName() + ";\n" +
                        "\n");
                String extendsClassName = nameForClass(extendsClass());
                String implementsSet = metaData.interfaces().stream()
                        .map(this::nameForClass)
                        .sorted()
                        .collect(Collectors.joining(", "));

                for (String import0 : importSet) {
                    sourceCode.append("" +
                            "import " + import0 + ";\n");
                }
                // can't add classes to imports from this point.
                importSet = Collections.unmodifiableSortedSet(importSet);
                sourceCode.append("\n");

                withLineNumber(sourceCode)
                        .append("public class ").append(className());
                String genericType = generateGenericType();
                if (genericType != null && !genericType.isEmpty())
                    sourceCode.append('<').append(genericType).append('>');
                if (extendsClass() != Object.class)
                    sourceCode.append(" extends ")
                            .append(extendsClassName);
                if (implementsSet.length() > 0) {
                    sourceCode.append(" implements ")
                            .append(implementsSet);
                }
                sourceCode.append(" {\n")
                        .append(mainCode)
                        .append("}\n");
                if (DUMP_CODE)
                    Jvm.startup().on(AbstractClassGenerator.class, sourceCode.toString());
            }

            // Compile and load the generated class.
            return (Class<T>) CACHED_COMPILER.loadFromJava(classLoader, fullName, sourceCode.toString());
        } catch (Throwable e) {
            // If there's any error during generation, compile, or load, throw an exception.
            throw Jvm.rethrow(new ClassNotFoundException(e.getMessage() + '\n' + sourceCode, e));
        }
    }

    /**
     * Generates a generic type for the class, if required.
     *
     * @return The generic type as a String, or null if there's none.
     */
    protected String generateGenericType() {
        return null;
    }

    /**
     * Specifies which class the generated class should extend.
     * By default, it extends the Object class.
     *
     * @return The superclass of the generated class.
     */
    protected Class<?> extendsClass() {
        return Object.class;
    }

    /**
     * Generates a name for the given class. This method also handles array types
     * and inner classes, ensuring that names are formatted correctly.
     *
     * @param clazz The class whose name should be generated.
     * @return A formatted name for the class.
     */
    public String nameForClass(Class<?> clazz) {
        // Handle array types recursively.
        if (clazz.isArray())
            return nameForClass(clazz.getComponentType()) + "[]";
        String s = clazz.getName().replace('$', '.');
        Package aPackage = clazz.getPackage();
        if (aPackage != null && !clazz.getName().contains("$")) {
            // Exclude common java.lang imports and handle others.
            if (!"java.lang".equals(aPackage.getName())
                    && !importSet.contains(aPackage.getName() + ".*")) {
                try {
                    if (!importSet.contains(s))
                        importSet.add(s);
                } catch (Exception e) {
                    Jvm.warn().on(getClass(), "Can't add an import for " + s);
                    throw e;
                }
            }
            return clazz.getSimpleName();
        }
        return s;
    }

    /**
     * Retrieves the maxCode value which seems to determine the maximum
     * number of characters or an internal limit for generated code.
     *
     * @return The maxCode value.
     */
    public int maxCode() {
        return maxCode;
    }

    /**
     * Sets the maxCode value.
     *
     * @param maxCode The desired maxCode value.
     * @return The current instance of the class generator for chaining calls.
     */
    public AbstractClassGenerator<M> maxCode(int maxCode) {
        this.maxCode = maxCode;
        return this;
    }

    /**
     * Generates a class name based on the metadata and possibly a hash value.
     * This ensures a unique name for each class based on its characteristics.
     *
     * @return The generated class name.
     */
    @NotNull
    protected String className() {
        if (maxCode() == 0)
            return metaData.baseClassName();

        // Generate a unique hash based on the meta-data.
        long h = HashWire.hash64(metaData);
        String code = Long.toUnsignedString(h, 36);
        if (code.length() > maxCode())
            code = code.substring(1, maxCode());
        char ch = 'A';
        ch += (char) ((h >>> 1) % 26);
        return metaData.baseClassName() + '$' + ch + code;
    }

    /**
     * Generates the main code for the class, including fields, constructors, methods, and more.
     *
     * @param mainCode The code formatter where the generated code is appended.
     */
    protected void generateMainCode(SourceCodeFormatter mainCode) {

        // Add an UpdateInterceptor field if required by the metadata.
        if (metaData.useUpdateInterceptor())
            mainCode.append("private transient final ").append(nameForClass(UpdateInterceptor.class)).append(" updateInterceptor;\n");

        generateFields(mainCode);
        mainCode.append('\n');

        generateConstructors(mainCode);

        generateMethods(mainCode);

        generateEnd(mainCode);
    }

    /**
     * Generates a field name based on the given class. If it's a primitive type, special naming is used.
     *
     * @param clazz The class for which the field name is being generated.
     * @return The generated field name.
     */
    protected String fieldCase(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            if (clazz == boolean.class)
                return "flag";
            return clazz.getName().substring(0, 1);
        }
        String simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    /**
     * Generates fields for the class.
     * This method is empty in the base class and can be overridden by subclasses to provide specific implementations.
     *
     * @param mainCode The code formatter where the generated code is appended.
     */
    protected void generateFields(SourceCodeFormatter mainCode) {
    }

    /**
     * Generates constructors for the class.
     * This method is empty in the base class and can be overridden by subclasses to provide specific implementations.
     *
     * @param mainCode The code formatter where the generated code is appended.
     */
    protected void generateConstructors(SourceCodeFormatter mainCode) {
    }

    /**
     * Appends the line number and stack trace for better traceability in the generated code.
     *
     * @param mainCode The code formatter where the line information is appended.
     * @return The updated code formatter.
     */
    protected SourceCodeFormatter withLineNumber(SourceCodeFormatter mainCode) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        mainCode.append("//").append("\tat ").append(stackTrace[2].toString()).append("\n");
        return mainCode;
    }

    /**
     * Generates methods for the class based on the methods to be overridden.
     *
     * @param mainCode The code formatter where the generated code is appended.
     */
    private void generateMethods(SourceCodeFormatter mainCode) {
        for (Method m : methodsToOverride())
            generateMethod(m, mainCode);
    }

    /**
     * Generates the implementation for a specific method.
     *
     * @param method The method whose code is being generated.
     * @param mainCode The code formatter where the generated code is appended.
     */
    protected void generateMethod(Method method, SourceCodeFormatter mainCode) {
        String name = method.getName();

        // Start the method definition, appending method name and return type.
        withLineNumber(mainCode)
                .append("public ").append(nameForClass(method.getReturnType())).append(" ").append(name).append("(");

        // Append parameters to the method.
        Class<?>[] pts = method.getParameterTypes();
        String sep = "";
        List<String> paramList = new ArrayList<>();
        StringBuilder params = new StringBuilder();
        Parameter[] parameters = method.getParameters();
        for (int i = 0, ptsLength = pts.length; i < ptsLength; i++) {
            Class<?> pt = pts[i];
            mainCode.append(sep);
            params.append(sep);
            sep = ", ";
            String pname = parameters[i].getName();

            // Handle duplicate parameter names.
            if (paramList.contains(pname))
                pname += paramList.size();
            paramList.add(pname);
            params.append(pname);
            mainCode.append(nameForClass(pt)).append(' ').append(pname);
        }
        mainCode.append(") {\n");

        // Include the update interceptor check if needed.
        if (metaData.useUpdateInterceptor()) {
            withLineNumber(mainCode)
                    .append("// updateInterceptor\n")
                    .append("if (!this.updateInterceptor.update(\"").append(name).append("\", ").append(paramList.get(0)).append(")) {\n")
                    .append("return").append(returnDefault(method.getReturnType())).append(";\n")
                    .append("}\n");
        }

        // Generate the main body of the method.
        generateMethod(method, params, paramList, mainCode);
        mainCode.append("}\n");
    }

    /**
     * Stub method for subclasses to implement end code generation, if necessary.
     *
     * @param mainCode The code formatter where the generated code is appended.
     */
    protected void generateEnd(SourceCodeFormatter mainCode) {

    }

    /**
     * Return a default value based on the method's return type.
     *
     * @param returnType The method's return type.
     * @return A string representing the default return value.
     */
    private String returnDefault(final Class<?> returnType) {
        if (returnType == void.class)
            return "";

        if (returnType.isPrimitive())
            throw new UnsupportedOperationException("having a method of this return type=" + returnType + " is not supported by method writers");

        if (returnType.isInterface())
            return " this";
        return " null";
    }

    /**
     * Abstract method for subclasses to implement the body of the generated method.
     *
     * @param method The method whose body is being generated.
     * @param params The parameters of the method in a StringBuilder.
     * @param paramList The list of parameter names.
     * @param mainCode The code formatter where the generated code is appended.
     */
    protected abstract void generateMethod(Method method, StringBuilder params, List<String> paramList, SourceCodeFormatter mainCode);

    /**
     * Determines the set of methods that should be overridden based on the provided metadata.
     * <p>
     * The method retrieves abstract methods from the interfaces specified in the metadata and the superclass.
     * Any concrete method present in the superclass or interfaces is considered to have already been
     * overridden, so these methods are excluded from the result.
     *
     * @return A set of methods that need to be overridden.
     */
    @NotNull
    protected Set<Method> methodsToOverride() {
        Map<String, Method> sig2methodMap = new TreeMap<>();
        Set<String> overridenSet = new LinkedHashSet<>();

        // Populate the map with methods from interfaces.
        for (Class<?> clazz : metaData().interfaces()) {
            addMethodsFor(sig2methodMap, overridenSet, clazz);
        }

        // Populate the map with methods from the superclass.
        addMethodsFor(sig2methodMap, overridenSet, extendsClass());

        // Remove overridden methods.
        for (String sig : overridenSet) {
            sig2methodMap.remove(sig);
        }
        return new LinkedHashSet<>(sig2methodMap.values());
    }

    /**
     * Helper function to populate the given map with methods from the provided class.
     *
     * @param sig2methodMap Map with method signature as key and method as value.
     * @param overridenSet Set with signatures of overridden methods.
     * @param clazz The class whose methods should be processed.
     */
    private void addMethodsFor(Map<String, Method> sig2methodMap, Set<String> overridenSet, Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            String sig = method.getName() + Arrays.toString(method.getParameterTypes());

            // If the method is abstract, add it to the map.
            // Otherwise, it's considered overridden and is added to the set.
            if (Modifier.isAbstract(method.getModifiers())) {
                sig2methodMap.putIfAbsent(sig, method);
            } else {
                overridenSet.add(sig);
            }
        }
    }

    /**
     * Represents the metadata configuration required for the class generator.
     * <p>
     * The `MetaData` class serves as a blueprint for the characteristics of the class
     * to be generated. It provides specifications like the package name, base class name,
     * interfaces to be implemented, and the flag to use an update interceptor.
     *
     * @param <M> Represents the actual type extending this `MetaData` class, facilitating method chaining.
     */
    public abstract static class MetaData<M extends MetaData<M>> extends SelfDescribingMarshallable {
        private String packageName = "";
        private String baseClassName = "";
        private Set<Class<?>> interfaces = new LinkedHashSet<>();
        private boolean useUpdateInterceptor;

        /**
         * Retrieves the package name for the class to be generated.
         *
         * @return The package name.
         */
        public String packageName() {
            return packageName;
        }

        /**
         * Sets the package name for the class to be generated.
         *
         * @param packageName The desired package name.
         * @return An instance of the metadata for method chaining.
         */
        public M packageName(String packageName) {
            this.packageName = packageName;
            return (M) this;
        }

        /**
         * Retrieves the base name of the class to be generated.
         *
         * @return The base class name.
         */
        public String baseClassName() {
            return baseClassName;
        }

        /**
         * Sets the base class name for the class to be generated.
         * Ensures that the provided class name is a valid Java identifier.
         *
         * @param baseClassName The desired base class name.
         * @return An instance of the metadata for method chaining.
         * @throws IllegalArgumentException if the provided name isn't a valid class name.
         */
        public M baseClassName(String baseClassName) {
            if (!SourceVersion.isIdentifier(baseClassName))
                throw new IllegalArgumentException(baseClassName + " is not a valid class name");
            this.baseClassName = baseClassName;
            return (M) this;
        }

        /**
         * Retrieves the interfaces that the class to be generated should implement.
         *
         * @return A set of interfaces.
         */
        public Set<Class<?>> interfaces() {
            return interfaces;
        }

        /**
         * Sets the interfaces for the class to be generated.
         *
         * @param interfaces A set of interfaces the generated class should implement.
         * @return An instance of the metadata for method chaining.
         */
        public M interfaces(Set<Class<?>> interfaces) {
            this.interfaces = interfaces;
            return (M) this;
        }

        /**
         * Checks if the class to be generated should use an update interceptor.
         *
         * @return `true` if an update interceptor should be used, `false` otherwise.
         */
        public boolean useUpdateInterceptor() {
            return useUpdateInterceptor;
        }

        /**
         * Specifies whether the class to be generated should use an update interceptor.
         *
         * @param useUpdateInterceptor A flag indicating the use of an update interceptor.
         * @return An instance of the metadata for method chaining.
         */
        public M useUpdateInterceptor(boolean useUpdateInterceptor) {
            this.useUpdateInterceptor = useUpdateInterceptor;
            return (M) this;
        }
    }
}
