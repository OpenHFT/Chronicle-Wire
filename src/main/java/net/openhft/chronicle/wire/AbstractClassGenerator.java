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

public abstract class AbstractClassGenerator<M extends AbstractClassGenerator.MetaData<M>> {
    public static final CachedCompiler CACHED_COMPILER = new CachedCompiler(Jvm.isDebug() ? new File(OS.getTarget(), "generated-test-sources") : null, null);
    private static final boolean DUMP_CODE = Jvm.getBoolean("dumpCode");
    protected final SourceCodeFormatter sourceCode = new JavaSourceCodeFormatter();
    protected SortedSet<String> importSet = new TreeSet<>();
    private final M metaData;
    private int maxCode = 6;

    protected AbstractClassGenerator(M metaData) {
        this.metaData = metaData;
    }

    public M metaData() {
        return metaData;
    }

    public synchronized <T> Class<T> acquireClass(ClassLoader classLoader) {
        String fullName = metaData.packageName() + "." + className();
        try {
            return (Class<T>) classLoader.loadClass(fullName);

        } catch (ClassNotFoundException cnfe) {
            // ignored
        }
        try {
            if (sourceCode.length() == 0) {
                SourceCodeFormatter mainCode = new JavaSourceCodeFormatter();
                generateMainCode(mainCode);
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
            return CACHED_COMPILER.loadFromJava(classLoader, fullName, sourceCode.toString());
        } catch (Throwable e) {
            throw Jvm.rethrow(new ClassNotFoundException(e.getMessage() + '\n' + sourceCode, e));
        }
    }

    protected String generateGenericType() {
        return null;
    }

    protected Class<?> extendsClass() {
        return Object.class;
    }

    public String nameForClass(Class<?> clazz) {
        if (clazz.isArray())
            return nameForClass(clazz.getComponentType()) + "[]";
        String s = clazz.getName().replace('$', '.');
        Package aPackage = clazz.getPackage();
        if (aPackage != null && !clazz.getName().contains("$")) {
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

    public int maxCode() {
        return maxCode;
    }

    public AbstractClassGenerator<M> maxCode(int maxCode) {
        this.maxCode = maxCode;
        return this;
    }

    @NotNull
    protected String className() {
        if (maxCode() == 0)
            return metaData.baseClassName();
        long h = HashWire.hash64(metaData);
        String code = Long.toUnsignedString(h, 36);
        if (code.length() > maxCode())
            code = code.substring(1, maxCode());
        char ch = 'A';
        ch += (char) ((h >>> 1) % 26);
        return metaData.baseClassName() + '$' + ch + code;
    }

    protected void generateMainCode(SourceCodeFormatter mainCode) {

        if (metaData.useUpdateInterceptor())
            mainCode.append("private transient final " + nameForClass(UpdateInterceptor.class) + " updateInterceptor;\n");

        generateFields(mainCode);
        mainCode.append('\n');

        generateConstructors(mainCode);

        generateMethods(mainCode);

        generateEnd(mainCode);
    }

    protected String fieldCase(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            if (clazz == boolean.class)
                return "flag";
            return clazz.getName().substring(0, 1);
        }
        String simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    protected void generateFields(SourceCodeFormatter mainCode) {
    }

    protected void generateConstructors(SourceCodeFormatter mainCode) {
    }

    protected SourceCodeFormatter withLineNumber(SourceCodeFormatter mainCode) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        mainCode.append("//").append("\tat ").append(stackTrace[2].toString()).append("\n");
        return mainCode;
    }

    private void generateMethods(SourceCodeFormatter mainCode) {
        for (Method m : methodsToOverride())
            generateMethod(m, mainCode);
    }

    protected void generateMethod(Method method, SourceCodeFormatter mainCode) {
        String name = method.getName();
        withLineNumber(mainCode)
                .append("public ").append(nameForClass(method.getReturnType())).append(" ").append(name).append("(");
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
            if (paramList.contains(pname))
                pname += paramList.size();
            paramList.add(pname);
            params.append(pname);
            mainCode.append(nameForClass(pt)).append(' ').append(pname);
        }
        mainCode.append(") {\n");
        if (metaData.useUpdateInterceptor()) {
            withLineNumber(mainCode)
                    .append("// updateInterceptor\n")
                    .append("if (!this.updateInterceptor.update(\"").append(name).append("\", ").append(paramList.get(0)).append(")) {\n")
                    .append("return").append(returnDefault(method.getReturnType())).append(";\n")
                    .append("}\n");
        }

        generateMethod(method, params, paramList, mainCode);
        mainCode.append("}\n");
    }

    protected void generateEnd(SourceCodeFormatter mainCode) {

    }

    private String returnDefault(final Class<?> returnType) {
        if (returnType == void.class)
            return "";

        if (returnType.isPrimitive())
            throw new UnsupportedOperationException("having a method of this return type=" + returnType + " is not supported by method writers");

        if (returnType.isInterface())
            return " this";
        return " null";
    }

    protected abstract void generateMethod(Method method, StringBuilder params, List<String> paramList, SourceCodeFormatter mainCode);

    @NotNull
    protected Set<Method> methodsToOverride() {
        Map<String, Method> sig2methodMap = new TreeMap<>();
        Set<String> overridenSet = new LinkedHashSet<>();
        for (Class<?> clazz : metaData().interfaces()) {
            addMethodsFor(sig2methodMap, overridenSet, clazz);
        }
        addMethodsFor(sig2methodMap, overridenSet, extendsClass());
        for (String sig : overridenSet) {
            sig2methodMap.remove(sig);
        }
        return new LinkedHashSet<>(sig2methodMap.values());
    }

    private void addMethodsFor(Map<String, Method> sig2methodMap, Set<String> overridenSet, Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            String sig = method.getName() + Arrays.toString(method.getParameterTypes());
            if (Modifier.isAbstract(method.getModifiers())) {
                sig2methodMap.putIfAbsent(sig, method);
            } else {
                overridenSet.add(sig);
            }
        }
    }

    public abstract static class MetaData<M extends MetaData<M>> extends SelfDescribingMarshallable {
        private String packageName = "";
        private String baseClassName = "";
        private Set<Class<?>> interfaces = new LinkedHashSet<>();
        private boolean useUpdateInterceptor;

        public String packageName() {
            return packageName;
        }

        public M packageName(String packageName) {
            this.packageName = packageName;
            return (M) this;
        }

        public String baseClassName() {
            return baseClassName;
        }

        public M baseClassName(String baseClassName) {
            if (!SourceVersion.isIdentifier(baseClassName))
                throw new IllegalArgumentException(baseClassName + " is not a valid class name");
            this.baseClassName = baseClassName;
            return (M) this;
        }

        public Set<Class<?>> interfaces() {
            return interfaces;
        }

        public M interfaces(Set<Class<?>> interfaces) {
            this.interfaces = interfaces;
            return (M) this;
        }

        public boolean useUpdateInterceptor() {
            return useUpdateInterceptor;
        }

        public M useUpdateInterceptor(boolean useUpdateInterceptor) {
            this.useUpdateInterceptor = useUpdateInterceptor;
            return (M) this;
        }
    }
}
