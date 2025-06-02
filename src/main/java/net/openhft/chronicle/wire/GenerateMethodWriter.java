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

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.Syncable;
import net.openhft.chronicle.core.util.GenericReflection;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.utils.JavaSourceCodeFormatter;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.*;
import static net.openhft.chronicle.core.util.GenericReflection.erase;
import static net.openhft.chronicle.core.util.GenericReflection.getParameterTypes;
/**
 * This is the GenerateMethodWriter class responsible for generating method writer code.
 * It provides utility methods and configurations to facilitate the dynamic generation of method writers.
 */
@SuppressWarnings("deprecation")
public class GenerateMethodWriter {

    // Constants for class names to be used in the generated method writer code
    public static final String UPDATE_INTERCEPTOR = UpdateInterceptor.class.getSimpleName();

    // Constant for the DocumentContext class's simple name
    private static final String DOCUMENT_CONTEXT = DocumentContext.class.getSimpleName();

    // Constant for the WriteDocumentContext class's simple name
    private static final String WRITE_DOCUMENT_CONTEXT = WriteDocumentContext.class.getSimpleName();

    // Constant for the MarshallableOut class's simple name
    private static final String MARSHALLABLE_OUT = MarshallableOut.class.getSimpleName();

    // Constant for the MethodId class's simple name
    private static final String METHOD_ID = MethodId.class.getSimpleName();

    // Constant for the ValueOut class's simple name
    private static final String VALUE_OUT = ValueOut.class.getSimpleName();

    // Constant for the Closeable class's simple name
    private static final String CLOSEABLE = Closeable.class.getSimpleName();

    // Constant field name for the UpdateInterceptor class
    private static final String UPDATE_INTERCEPTOR_FIELD = "updateInterceptor";

    // Indicates whether to dump the generated code or not
    static final boolean DUMP_CODE = Jvm.getBoolean("dumpCode");

    // Map to hold template methods for generating method writer code
    private static final Map<String, Map<List<Class<?>>, String>> TEMPLATE_METHODS = new LinkedHashMap<>();
    private static final int SYNTHETIC = 0x00001000;

    static {
        // Ensure Wires static block is called and classpath is set up
        Wires.init();

        // Define template methods for the generation process
        TEMPLATE_METHODS.put("close",
                singletonMap(singletonList(void.class), "public void close() {\n" +
                        "   if (this.closeable != null) {\n" +
                        "        this.closeable.close();\n" +
                        "   }\n" +
                        "}\n"));
        TEMPLATE_METHODS.put("recordHistory",
                singletonMap(singletonList(boolean.class), "public boolean recordHistory() {\n" +
                        "    return out.get().recordHistory();\n" +
                        "}\n"));
        List<Class<?>> dcBoolean = Stream.of(DocumentContext.class, boolean.class).collect(Collectors.toList());
        TEMPLATE_METHODS.put("acquireWritingDocument",
                singletonMap(dcBoolean, "public " + DOCUMENT_CONTEXT + " acquireWritingDocument(boolean metaData){\n" +
                        "    return out.get().acquireWritingDocument(metaData);\n" +
                        "}\n"));
        Map<List<Class<?>>, String> wd = new LinkedHashMap<>();
        wd.put(singletonList(DocumentContext.class), "public " + DOCUMENT_CONTEXT + " writingDocument(){\n" +
                "    return out.get().writingDocument();\n" +
                "}\n");
        wd.put(dcBoolean, "public " + DOCUMENT_CONTEXT + " writingDocument(boolean metaData){\n" +
                "return out.get().writingDocument(metaData);\n" +
                "}\n");
        TEMPLATE_METHODS.put("writingDocument", wd);
    }

    // Indicates if metadata is included in the generated method writer
    private final boolean metaData;

    // Indicates if a method ID is used in the generated method writer
    private final boolean useMethodId;

    // Package name for the generated method writer
    private final String packageName;

    // Set of interfaces to be implemented by the generated method writer
    private final Set<Class<?>> interfaces;

    // Name of the generated class
    private final String className;

    // Class loader used for the generated method writer
    private final ClassLoader classLoader;

    // Wire type for serialization in the generated method writer
    private final WireType wireType;

    // Generic event type used in the generated method writer
    private final String genericEvent;

    // Indicates if the update interceptor is used in the generated method writer
    private final boolean useUpdateInterceptor;

    // Concurrent map to cache method writers
    private final ConcurrentMap<Class<?>, String> methodWritersMap = new ConcurrentHashMap<>();
    // AtomicInteger to manage indentation in the generated code
    final private AtomicInteger indent = new AtomicInteger();
    // Indicates if verbose types are used in the generated method writer
    private final boolean verboseTypes;

    /**
     * Constructor for the GenerateMethodWriter class.
     * Initializes all the required fields for the code generation process.
     *
     * @param packageName         The package name for the generated method writer.
     * @param interfaces          The interfaces to be implemented by the generated method writer.
     * @param className           The name of the generated class.
     * @param classLoader         The class loader to use.
     * @param wireType            The wire type for serialization.
     * @param genericEvent        The generic event type.
     * @param metaData            Indicates if metadata should be included.
     * @param useMethodId         Indicates if method ID should be used.
     * @param useUpdateInterceptor Indicates if the update interceptor should be used.
     * @param verboseTypes        Indicates if verbose types should be used.
     */
    private GenerateMethodWriter(final String packageName,
                                 final Set<Class<?>> interfaces,
                                 final String className,
                                 final ClassLoader classLoader,
                                 final WireType wireType,
                                 final String genericEvent,
                                 final boolean metaData,
                                 final boolean useMethodId,
                                 final boolean useUpdateInterceptor,
                                 final boolean verboseTypes) {

        this.packageName = packageName;
        this.interfaces = interfaces;
        this.className = className;
        this.classLoader = classLoader;
        this.wireType = wireType;
        this.genericEvent = genericEvent;
        this.metaData = metaData;
        this.useMethodId = useMethodId;
        this.useUpdateInterceptor = useUpdateInterceptor;
        this.verboseTypes = verboseTypes;
    }

    /**
     * Generates a proxy class based on the provided interface class.
     *
     * @param fullClassName         Fully qualified class name for the generated proxy class.
     * @param interfaces            A set of interface classes that the generated proxy class will implement.
     * @param classLoader           The class loader to use for generating the proxy class.
     * @param wireType              The wire type for serialization.
     * @param genericEvent          The generic event type.
     * @param metaData              Indicates if metadata should be included.
     * @param useMethodId           Indicates if method ID should be used.
     * @param useUpdateInterceptor  Indicates if the update interceptor should be used.
     * @param verboseTypes          Indicates if verbose types should be used.
     * @return                      A generated proxy class based on the provided interface class,
     *                              or null if it can't be created.
     */
    @Nullable
    public static Class<?> newClass(String fullClassName,
                                    Set<Class<?>> interfaces,
                                    ClassLoader classLoader,
                                    final WireType wireType,
                                    final String genericEvent,
                                    boolean metaData,
                                    boolean useMethodId,
                                    final boolean useUpdateInterceptor,
                                    boolean verboseTypes) {
        String packageName = ReflectionUtil.generatedPackageName(fullClassName);

        int lastDot = fullClassName.lastIndexOf('.');
        String className = lastDot == -1 ? fullClassName : fullClassName.substring(lastDot + 1);

        return new GenerateMethodWriter(packageName,
                interfaces,
                className,
                classLoader,
                wireType,
                genericEvent,
                metaData, useMethodId, useUpdateInterceptor, verboseTypes)
                .createClass();
    }

    /**
     * Acquires a {@link DocumentContext} instance, either by reusing the existing one from the thread-local
     * context holder if it's not closed or by creating a new one using the provided output.
     *
     * @param metaData            Indicates if metadata should be included in the {@link DocumentContext}.
     * @param documentContextTL   The thread-local holder of the {@link DocumentContext}.
     * @param out                 The output where the document will be written.
     * @return                    An instance of {@link DocumentContext}.
     */
    @SuppressWarnings("unused")
    public static DocumentContext acquireDocumentContext(boolean metaData,
                                                         ThreadLocal<DocumentContextHolder> documentContextTL,
                                                         MarshallableOut out) {
        DocumentContextHolder contextHolder = documentContextTL.get();
        if (!contextHolder.isClosed())
            return contextHolder;
        contextHolder.documentContext(
                out.writingDocument(metaData));
        return contextHolder;
    }

    /**
     * Converts a class type into a representative string, e.g., int to "int32", boolean to "bool", etc.
     * For types not explicitly mapped, a default representation is returned.
     *
     * @param type   The class type to be converted.
     * @return       A representative string for the given class type.
     */
    private static CharSequence toString(Class<?> type) {
        if (boolean.class.equals(type)) {
            return "bool";
        } else if (byte.class.equals(type)) {
            return "writeByte";
        } else if (char.class.equals(type)) {
            return "character";
        } else if (short.class.equals(type)) {
            return "int16";
        } else if (int.class.equals(type)) {
            return "int32";
        } else if (long.class.equals(type)) {
            return "int64";
        } else if (float.class.equals(type)) {
            return "float32";
        } else if (double.class.equals(type)) {
            return "float64";
        } else if (CharSequence.class.isAssignableFrom(type)) {
            return "text";
        } else if (Marshallable.class.isAssignableFrom(type)) {
            return "marshallable";
        }
        return "object";
    }

    /**
     * Retrieves the full canonical name of a class, with any '$' characters replaced by '.'.
     *
     * @param type   The class for which the name is required.
     * @return       The full name of the class with '$' characters replaced.
     */
    @NotNull
    private static String nameForClass(Class<?> type) {
        return type.getName().replace('$', '.');
    }

    /**
     * Retrieves the name for a class, considering any imports and package details.
     * If the type is part of the provided import set, or if it belongs to the "java.lang" package,
     * only the simple name of the class is returned. Otherwise, the full canonical name is returned.
     *
     * @param importSet   A set of class names or package patterns that have been imported.
     * @param type        The class for which the name is required.
     * @return            The appropriate name of the class based on the import context.
     */
    @NotNull
    private static String nameForClass(Set<String> importSet, Class<?> type) {
        if (type.isArray())
            return nameForClass(importSet, type.getComponentType()) + "[]";
        String s = nameForClass(type);
        String packageName = Jvm.getPackageName(type);
        if (importSet.contains(s)
                || "java.lang".equals(packageName)
                || (!type.getName().contains("$")
                && importSet.contains(packageName + ".*")))
            return type.getSimpleName();
        return s;
    }

    /**
     * Constructs a method signature string for a given method and type.
     * The signature string includes the return type, method name, and parameter types.
     *
     * @param m     The method for which the signature is to be generated.
     * @param type  The type that might contain the method.
     * @return      A string representing the method signature.
     */
    private static String signature(Method m, Type type) {
        final Type returnType = GenericReflection.getReturnType(m, type);
        final Type[] parameterTypes = getParameterTypes(m, type);
        return returnType + " " + m.getName() + " " + Arrays.toString(parameterTypes);
    }

    /**
     * Checks if a given modifier represents a synthetic entity.
     *
     * @param modifiers  The modifiers to check.
     * @return           True if the modifier is synthetic, false otherwise.
     */
    static boolean isSynthetic(int modifiers) {
        return (modifiers & SYNTHETIC) != 0;  // Check the SYNTHETIC flag in the modifiers
    }

    /**
     * Constructs the method signature for a method, considering its annotations,
     * parameters, and the provided import set.
     *
     * @param importSet       The set of imported classes or packages.
     * @param dm              The method for which the signature is required.
     * @param parameterTypes  The parameter types for the method.
     * @return                An {@link Appendable} containing the method signature.
     */
    @NotNull
    private Appendable methodSignature(SortedSet<String> importSet, final Method dm, Type[] parameterTypes) {

        SourceCodeFormatter result = new JavaSourceCodeFormatter(this.indent);
        String sep = "";
        Parameter[] parameters = dm.getParameters();

        // Iterate over each parameter of the method
        for (int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];
            result.append(sep);
            sep = ", ";
            LongConversion longConversion = Jvm.findAnnotation(p, LongConversion.class);

            if (longConversion != null)
                result.append("@LongConversion(")
                        .append(nameForClass(importSet, longConversion.value()))
                        .append(".class) ");

            // Append parameter type and name to the result
            result.append("final ")
                    .append(nameForClass(importSet, erase(parameterTypes[i])))
                    .append(' ')
                    .append(p.getName());
        }
        return result;
    }

    /**
     * Generates and loads a Java class based on provided interfaces. This method dynamically constructs
     * a new Java class that implements the specified interfaces and the {@link MethodWriter} interface.
     * The generated class will reside in the specified package and will import necessary dependencies
     * from various packages, especially from the `net.openhft.chronicle.bytes` and `net.openhft.chronicle.wire` packages.
     *
     * <p>The method primarily works in the following steps:
     * <ol>
     *     <li>Generates the class header, including the package declaration and required imports.</li>
     *     <li>Processes each method from the provided interfaces and generates an appropriate method body.</li>
     *     <li>Attempts to load the newly created class using the generated source code.</li>
     * </ol>
     *
     * <p><b>Debugging:</b> If the static field `DUMP_CODE` is set to {@code true}, the generated Java code
     * will be printed to the standard output. This can be useful for debugging purposes.
     *
     * @return The {@link Class} object representing the dynamically generated class.
     * @throws MethodWriterValidationException If there's an issue validating methods during the code generation.
     */
    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    private Class<?> createClass() {
        // For storing method implementations
        SourceCodeFormatter interfaceMethods = new SourceCodeFormatter(1);
        // For storing generated Java code
        SourceCodeFormatter imports = new JavaSourceCodeFormatter();

        try {
            // Define package for the generated class
            imports.append("package " + packageName + ";\n\n");

            // Collection of classes/packages that need to be imported in generated class
            SortedSet<String> importSet = new TreeSet<>();
            importSet.add(LongConversion.class.getName());
            importSet.add(GenerateMethodWriter.class.getName());
            importSet.add(MessageHistory.class.getName());
            importSet.add(MethodReader.class.getName());
            importSet.add(UpdateInterceptor.class.getName());
            importSet.add(MethodId.class.getName());
            importSet.add(GenerateMethodWriter.class.getName());
            importSet.add(DocumentContext.class.getName());
            importSet.add(WriteDocumentContext.class.getName());
            importSet.add(MethodWriterInvocationHandlerSupplier.class.getName());
            importSet.add(Jvm.class.getName());
            importSet.add(Closeable.class.getName());
            importSet.add(DocumentContextHolder.class.getName());
            importSet.add(java.lang.reflect.InvocationHandler.class.getName());
            importSet.add(java.lang.reflect.Method.class.getName());
            importSet.add(java.util.stream.IntStream.class.getName());
            importSet.add(java.util.ArrayList.class.getName());
            importSet.add(java.util.List.class.getName());
            importSet.add(Supplier.class.getName());

            // Iterate through all interfaces to extract required imports and validations
            for (Class<?> interfaceClazz : interfaces) {
                importSet.add(nameForClass(interfaceClazz));

                if (!interfaceClazz.isInterface())
                    throw new MethodWriterValidationException("expecting an interface instead of class=" + interfaceClazz.getName());

                // TODO: Need a test to show when an extra import is required.
                // Methods extraction and generation
                for (Method dm : interfaceClazz.getMethods()) {
                    if (dm.isDefault() || Modifier.isStatic(dm.getModifiers()))
                        continue;
                    String template = templateFor(dm, interfaceClazz);
                    if (template != null)
                        continue;
                    for (Type type : getParameterTypes(dm, interfaceClazz)) {
                        Class<?> pType = erase(type);
                        if (pType.isPrimitive() || pType.isArray() || Jvm.getPackageName(pType).equals("java.lang"))
                            continue;
                        importSet.add(nameForClass(pType));
                    }
                }
            }

            // Adding standard chronicle imports for byte and wire packages
            // and cleaning up specific imports in favour of wildcard imports
            importSet.removeIf(s -> s.startsWith("net.openhft.chronicle.bytes"));
            importSet.add("net.openhft.chronicle.bytes.*");
            importSet.removeIf(s -> s.startsWith("net.openhft.chronicle.wire"));
            importSet.add("net.openhft.chronicle.wire.*");

            for (String s : importSet) {
                imports.append("import ").append(s).append(";\n");
            }

            // Declare the new class implementing all interfaces and MethodWriter
            imports.append("\npublic final class ")
                    .append(className)
                    .append(" implements ");

            Set<String> handledMethodSignatures = new HashSet<>();
            Set<String> methodIds = new HashSet<>();

            for (Class<?> interfaceClazz : interfaces) {

                String interfaceName = nameForClass(importSet, interfaceClazz);
                imports.append(interfaceName);
                imports.append(", ");

                if (!interfaceClazz.isInterface())
                    throw new MethodWriterValidationException("expecting an interface instead of class=" + interfaceClazz.getName());

                for (Method dm : interfaceClazz.getMethods()) {
                    final int modifiers = dm.getModifiers();
                    if (Modifier.isStatic(modifiers) || isSynthetic(modifiers))
                        continue;

                    final Class<?> returnType = returnType(dm, interfaceClazz);
                    if (dm.isDefault() && (!returnType.equals(void.class) && !returnType.isInterface()))
                        continue;

                    final String signature = signature(dm, interfaceClazz);
                    if (!handledMethodSignatures.add(signature))
                        continue;

                    String template = templateFor(dm, interfaceClazz);
                    if (template == null) {
                        interfaceMethods.append(createMethod(importSet, dm, interfaceClazz, methodIds));
                    } else {
                        interfaceMethods.append(template);
                    }
                }
            }

            imports.append(MethodWriter.class.getSimpleName());
            imports.append(" {\n\n");
            constructorAndFields(importSet, className, imports);
            addMarshallableOut(imports);
            imports.append(interfaceMethods);
            imports.append("\n}\n");

            // Printing the generated code (only for debugging purposes)
            if (DUMP_CODE)
                System.out.println(imports);

            // Attempt to load the class using the generated code
            return Wires.loadFromJava(classLoader, packageName + '.' + className, imports.toString());

        } catch (AssertionError e) {
            // In case there is a linkage error, try loading the class directly
            if (e.getCause() instanceof LinkageError) {
                try {
                    return Class.forName(packageName + '.' + className, true, classLoader);
                } catch (ClassNotFoundException x) {
                    throw Jvm.rethrow(x);
                }
            }
            throw Jvm.rethrow(e);
        } catch (MethodWriterValidationException e) {
            throw e; // This seems to be a custom exception, rethrow it as is
        } catch (Throwable e) {
            // In case of other exceptions, wrap the cause and rethrow
            throw Jvm.rethrow(new ClassNotFoundException(e.getMessage() + '\n' + imports, e));
        }
    }

    /**
     * Determines the return type of the provided method relative to a given interface class.
     * It makes use of {@code GenericReflection} to get the return type and, if the return type isn't a class instance,
     * it attempts to resolve it as a generic declaration.
     *
     * @param dm            The method whose return type needs to be determined.
     * @param interfaceClazz The interface class relative to which the method's return type is evaluated.
     * @return The class type of the method's return type.
         */
    private Class<?> returnType(Method dm, Class<?> interfaceClazz) {
        Type returnType = GenericReflection.getReturnType(dm, interfaceClazz);
        if (!(returnType instanceof Class))
            returnType = (Type) ((TypeVariable<?>) returnType).getGenericDeclaration();
        return erase(returnType);
    }

    /**
     * Looks up a template for a given method and interface type.
     * The method searches for predefined templates using the method's name.
     *
     * @param dm           The method for which a template is required.
     * @param interfaceType The interface type in which the method is defined.
     * @return The template string if found, otherwise {@code null}.
     */
    private String templateFor(Method dm, Class<?> interfaceType) {
        Map<List<Class<?>>, String> map = TEMPLATE_METHODS.get(dm.getName());
        if (map == null)
            return null;
        List<Class<?>> sig = new ArrayList<>();
        sig.add(returnType(dm, interfaceType));
        for (Type type : getParameterTypes(dm, interfaceType)) {
            addAll(sig, erase(type));
        }
        return map.get(sig);
    }

    /**
     * Generates code for the `marshallableOut` method.
     * The method updates the internal state of the generated class.
     *
     * @param codeFormatter The formatter that helps structure the generated code.
     */
    private void addMarshallableOut(SourceCodeFormatter codeFormatter) {
        codeFormatter.append("@Override\n");
        codeFormatter.append("public void marshallableOut(MarshallableOut out) {\n");
        codeFormatter.append("this.out = () -> out;");
        for (Map.Entry<Class<?>, String> e : methodWritersMap.entrySet()) {
            codeFormatter.append(format("\n    this.%s.remove();", e.getValue()));
        }
        codeFormatter.append("\n}\n");

    }

    /**
     * Constructs the source code for class fields and the class constructor.
     * The method defines the state of the generated class and provides a constructor to initialize this state.
     *
     * @param importSet     The set of classes that need to be imported.
     * @param className     The name of the generated class.
     * @param result        The formatter that helps structure the generated source code.
     * @return The structured source code containing the fields and constructor for the generated class.
     */
    private CharSequence constructorAndFields(Set<String> importSet, final String className, SourceCodeFormatter result) {

        result.append("// result\n" +
                "private transient final Closeable closeable;\n");
        if (useUpdateInterceptor)
            result.append("private transient final " + UPDATE_INTERCEPTOR + " " + UPDATE_INTERCEPTOR_FIELD + ";\n");

        result.append("private transient Supplier<")
                .append(MARSHALLABLE_OUT)
                .append("> out;\n");
        for (Map.Entry<Class<?>, String> e : methodWritersMap.entrySet()) {
            result.append(format("private transient ThreadLocal<%s> %s;\n", nameForClass(importSet, e.getKey()), e.getValue()));
        }
        result.append('\n');

        result.append(format("// constructor\npublic %s(Supplier<" + MARSHALLABLE_OUT + "> out, "
                + CLOSEABLE + " closeable, " +
                UpdateInterceptor.class.getSimpleName() + " " + UPDATE_INTERCEPTOR_FIELD + ") {\n", className));

        if (useUpdateInterceptor)
            result.append("this." + UPDATE_INTERCEPTOR_FIELD + "= " + UPDATE_INTERCEPTOR_FIELD + ";\n");
        result.append("this.out = out;\n" +
                "this.closeable = closeable;");
        for (Map.Entry<Class<?>, String> e : methodWritersMap.entrySet()) {
            result.append(format("\n%s = ThreadLocal.withInitial(() -> out.get().methodWriter(%s.class));", e.getValue(), nameForClass(e.getKey())));
            result.append(format("\n%s = ThreadLocal.withInitial(() -> out.get().methodWriterBuilder(%s.class)" +
                    ".verboseTypes(%b).build());", e.getValue(), nameForClass(e.getKey()), verboseTypes));
        }

        result.append("\n}\n\n");
        return result;
    }

    /**
     * Creates the method writer code for a given method.
     *
     * @param importSet The set of imports required for the method
     * @param dm The method for which the writer code is to be generated
     * @param interfaceClazz The class of the interface being processed
     * @param methodIds The set of method IDs to track the generated methods
     * @return A CharSequence containing the generated method writer code
     */
    private CharSequence createMethod(SortedSet<String> importSet, final Method dm, final Class<?> interfaceClazz, Set<String> methodIds) {

        // Skip static methods
        if (Modifier.isStatic(dm.getModifiers()))
            return "";

        // For methods with no parameters and default implementations, return an empty string
        int parameterCount = dm.getParameterCount();
        if (parameterCount == 0 && dm.isDefault())
            return "";

        Parameter[] parameters = dm.getParameters();
        final int len = parameters.length;
        final Class<?> returnType = returnType(dm, interfaceClazz);

        // Get the type name for return type
        final String typeName = nameForClass(importSet, returnType);

        final StringBuilder body = new StringBuilder();
        String methodIDAnotation = "";
        final Type[] parameterTypes = getParameterTypes(dm, interfaceClazz);

        // UpdateInterceptor logic
        if (useUpdateInterceptor) {
            if (parameterCount > 1)
                Jvm.debug().on(getClass(), "Generated code to call updateInterceptor for " + dm + " only using last argument");
            final String name;
            if (parameterCount > 0) {
                Type type = parameterTypes[parameterCount - 1];
                if (type instanceof Class && ((Class<?>) type).isPrimitive())
                    Jvm.warn().on(getClass(), "Generated code to call updateInterceptor for " + dm + " will box and generate garbage");
                name = parameters[parameterCount - 1].getName();
            } else
                name = "null";
            body.append("// updateInterceptor\n"
                    + "if (! this." + UPDATE_INTERCEPTOR_FIELD +
                    ".update(\"" + dm.getName() + "\", " + name + ")) return" + returnDefault(returnType) + ";\n");
        }

        body.append("MarshallableOut out = this.out.get();\n");
        boolean terminating = returnType == Void.class || returnType == void.class || returnType.isPrimitive();
        boolean passthrough = returnType == DocumentContext.class;

        // MarshallableOut setup logic
        if (!passthrough)
            body.append("try (");
        body.append("final ")
                .append(WRITE_DOCUMENT_CONTEXT)
                .append(" _dc_ = (")
                .append(WRITE_DOCUMENT_CONTEXT).append(") out.acquireWritingDocument(")
                .append(metaData)
                .append(")");
        if (passthrough)
            body.append(";\n");
        else
            body.append(") {\n");
        body.append("try {\n");
        body.append("_dc_.chainedElement(" + (!terminating && !passthrough) + ");\n");
        body.append("if (out.recordHistory()) MessageHistory.writeHistory(_dc_);\n");

        int startJ = 0;

        final String eventName;
        // Logic for generic events
        if (parameterCount > 0 && dm.getName().equals(genericEvent)) {
            eventName = parameters[0].getName();
            startJ = 1;
        } else {
            eventName = '\"' + dm.getName() + '\"';
        }

        // Determine the appropriate event name or ID for the method
        methodIDAnotation = writeEventNameOrId(dm, body, eventName);

        // Check for duplicate method IDs and throw an exception if a duplicate is found
        if (methodIDAnotation.length() > 0 && !methodIds.add(methodIDAnotation))
            throw new MethodWriterValidationException("Duplicate methodIds. Cannot add " + methodIDAnotation + " to " + methodIds);

        // Write out the parameters for the method, if they exist
        if (parameters.length > 0)
            writeArrayOfParameters(dm, parameterTypes, len, body, startJ);

        // Handle the scenario where there are no parameters
        if (parameterCount == 0)
            body.append("_valueOut_.text(\"\");\n");

        // Handle exceptions during method execution
        body.append("} catch (Throwable _t_) {\n");
        body.append("_dc_.rollbackOnClose();\n");
        body.append("throw Jvm.rethrow(_t_);\n");
        body.append("}\n");

        // Synchronize the method if it belongs to the Syncable class
        if (dm.getDeclaringClass() == Syncable.class) {
            body.append(Syncable.class.getName()).append(".syncIfAvailable(out);\n");
        }

        // Close the method body if it's not a passthrough method
        if (!passthrough)
            body.append("}\n");

        // Return the formatted method writer code
        return format("\n%s public %s %s(%s) {\n %s%s}\n",
                methodIDAnotation,
                typeName,
                dm.getName(),
                methodSignature(importSet, dm, parameterTypes),
                body,
                methodReturn(dm, interfaceClazz));
    }

    /**
     * Determines the default return value for a given return type.
     *
     * @param returnType The return type of a method
     * @return A string representing the default value for the specified return type
     */
    private String returnDefault(final Class<?> returnType) {

        // Void return type scenario
        if (returnType == void.class)
            return "";

        // If the return type is primitive or Void class
        if (returnType.isPrimitive() || returnType == Void.class)
            return " " + ObjectUtils.defaultValue(returnType);

        // Default scenario
        return " this";
    }

    /**
     * Writes the event name or ID for a given method. Determines whether to use the event name
     * or ID based on the wire type and the presence of a MethodId annotation.
     *
     * @param dm        The method in question.
     * @param body      The StringBuilder used to build the body of the method writer.
     * @param eventName The name of the event.
     * @return The method ID as a string, or an empty string if there's none.
     */
    private String writeEventNameOrId(final Method dm, final StringBuilder body, final String eventName) {
        String methodID = "";
        final Optional<Annotation> methodId = useMethodId ? stream(dm.getAnnotations()).filter(MethodId.class::isInstance).findFirst() : Optional.empty();
        if ((wireType != WireType.TEXT && wireType != WireType.YAML) && methodId.isPresent()) {

            long value = ((MethodId) methodId.get()).value();
            body.append(format("final " + VALUE_OUT + " _valueOut_ = _dc_.wire().writeEventId(%s, %d);\n", eventName, value));
            methodID = format("@" + METHOD_ID + "(%d)\n", value);

        } else
            body.append(format("final " + VALUE_OUT + " _valueOut_ = _dc_.wire().writeEventName(%s);\n", eventName));
        return methodID;
    }

    /**
     * Writes out the parameters for a given method in the desired format. Handles
     * different parameter types and annotations to generate the appropriate method writer code.
     *
     * @param dm   The method whose parameters need to be written out.
     * @param parameterTypes An array of the types of the method's parameters.
     * @param len  The length of the parameters array.
     * @param body The StringBuilder used to build the body of the method writer.
     * @param startJ Starting index to loop through the parameters.
     */
    private void writeArrayOfParameters(final Method dm, Type[] parameterTypes, final int len, final StringBuilder body, final int startJ) {
        final int parameterCount = dm.getParameterCount();
        final boolean multipleArgs = parameterCount > startJ + 1;
        if (multipleArgs)
            body.append("_valueOut_.array(_v_ -> {\n");
        for (int j = startJ; j < len; j++) {

            final Parameter p = dm.getParameters()[j];

            final LongConversion longConversion = Jvm.findAnnotation(p, LongConversion.class);

            final String name = longConversion != null ? longConversion.value().getName() : "";

            // Append appropriate value to the body based on parameter type and annotations
            if (!name.isEmpty() && (WireType.TEXT == wireType || WireType.YAML == wireType))
                body.append(format("_valueOut_.rawText(%s.INSTANCE.asText(%s));\n", name, p.getName()));
            else if (p.getType().isPrimitive() || CharSequence.class.isAssignableFrom(p.getType())) {
                if (longConversion != null && (p.getType() == long.class || CharSequence.class.isAssignableFrom(p.getType())))
                    body.append(format("%s.writeLong(%s.INSTANCE, %s);\n", multipleArgs ? "_v_" : "_valueOut_", longConversion.value().getName(), p.getName()));
                else
                    body.append(format("%s.%s(%s);\n", multipleArgs ? "_v_" : "_valueOut_", toString(erase(parameterTypes[j])), p.getName()));
            } else
                writeValue(dm, erase(parameterTypes[j]), body, startJ, p);
        }

        // Close the parameter array if there are multiple arguments
        if (multipleArgs)
            body.append("}, Object[].class);\n");
    }

    /**
     * Writes the value of a parameter to the method body, handling various types of parameter values.
     *
     * @param dm       The method whose parameter value needs to be written.
     * @param type     The type of the parameter.
     * @param body     The StringBuilder used to build the body of the method writer.
     * @param startJ   The starting index to check if the current parameter is among multiple arguments.
     * @param p        The parameter whose value is being written.
     */
    private void writeValue(final Method dm, Class<?> type, final StringBuilder body, final int startJ, final Parameter p) {
        final String name = p.getName();
        String className = type.getTypeName().replace('$', '.');

        final String vOut = dm.getParameterCount() > startJ + 1 ? "_v_" : "_valueOut_";
        String after = "";

        if (verboseTypes) {
            body
                    .append(vOut)
                    .append(".object(")
                    .append(name)
                    .append(");\n")
                    .append(after);
        } else {

            if (!type.isInterface() && Marshallable.class.isAssignableFrom(type) && !Serializable.class.isAssignableFrom(type) && !DynamicEnum.class.isAssignableFrom(type)) {
                body.append("if (").append(name).append(" != null && ").append(className).append(".class == ").append(name).append(".getClass()) {\n")
                        .append(vOut).append(".marshallable(").append(name).append(");\n")
                        .append("} else  {\n");
                after = "}\n";
            }
            body
                    .append(vOut)
                    .append(".object(")
                    .append(className)
                    .append(".class, ")
                    .append(name)
                    .append(");\n")
                    .append(after);
        }
    }

    /**
     * Generates the return statement for a given method based on its return type. This method
     * deals with various return types, such as Void, interface types, and primitive types.
     *
     * @param dm             The method for which the return statement is being generated.
     * @param interfaceClazz The class or interface being implemented.
     * @return A StringBuilder containing the return statement for the method.
     */
    private StringBuilder methodReturn(final Method dm, final Class<?> interfaceClazz) {
        final StringBuilder result = new StringBuilder();
        final Class<?> returnType = returnType(dm, interfaceClazz);

        if (returnType == Void.class || returnType == void.class)
            return result;

        if (returnType == DocumentContext.class) {
            result.append("return _dc_;\n");

        } else if (returnType.isAssignableFrom(interfaceClazz) || returnType == interfaceClazz) {
            result.append("return this;\n");

        } else if (returnType.isInterface()) {
            methodWritersMap.computeIfAbsent(returnType, k -> "methodWriter" + k.getSimpleName() + "TL");
            result.append("// method return\n");
            result.append(format("return methodWriter%sTL.get();\n", returnType.getSimpleName()));
        } else if (!returnType.isPrimitive()) {
            result.append("return null;\n");
        } else if (returnType == boolean.class) {
            result.append("return false;\n");
        } else if (returnType == byte.class) {
            result.append("return (byte)0;\n");
        } else {
            result.append("return 0;\n");
        }

        return result;
    }
}
