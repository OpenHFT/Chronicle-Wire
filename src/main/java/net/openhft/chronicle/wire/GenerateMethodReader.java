/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.util.Annotations;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.utils.JavaSourceCodeFormatter;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.lang.String.format;
import static net.openhft.compiler.CompilerUtils.CACHED_COMPILER;

/**
 * Responsible for code generation and its runtime compilation of custom {@link MethodReader}s.
 */
public class GenerateMethodReader {
    private static final boolean DUMP_CODE = Jvm.getBoolean("dumpCode");

    private final WireType wireType;
    private final Object[] instances;

    private final Set<String> handledMethodNames = new HashSet<>();
    private final Set<Class<?>> handledInterfaces = new HashSet<>();
    private final Set<String> handledChainedInterfaces = new HashSet<>();

    private final SourceCodeFormatter sourceCode = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter fields = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter eventNameSwitchBlock = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter eventIdSwitchBlock = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter numericConverters = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter chainedCallResults = new JavaSourceCodeFormatter();

    private boolean methodFilterPresent;
    private boolean isSourceCodeGenerated;

    public GenerateMethodReader(WireType wireType, Object... instances) {
        this.wireType = wireType;
        this.instances = instances;
    }

    /**
     * Generates and compiles in runtime code of a custom {@link MethodReader}.
     *
     * @return {@link MethodReader} implementation for specified {@link #instances}.
     */
    public Class<?> createClass() {
        if (!isSourceCodeGenerated)
            generateSourceCode();

        if (DUMP_CODE)
            System.out.println(sourceCode);

        final ClassLoader classLoader = instances[0].getClass().getClassLoader();

        try {
            return CACHED_COMPILER.loadFromJava(classLoader,
                    packageName() + '.' + generatedClassName(), sourceCode.toString());
        } catch (LinkageError e) {
            try {
                return Class.forName(packageName() + '.' + generatedClassName(), true, classLoader);
            } catch (ClassNotFoundException x) {
                throw Jvm.rethrow(x);
            }
        } catch (Throwable e) {
            throw Jvm.rethrow(new ClassNotFoundException(e.getMessage() + '\n' + sourceCode, e));
        }
    }

    /**
     * Generates source code of {@link MethodReader} for specified {@link #instances}.
     */
    private void generateSourceCode() {
        for (int i = 0; i < instances.length; i++) {
            final Class<?> aClass = instances[i].getClass();

            boolean methodFilter = instances[i] instanceof MethodFilterOnFirstArg;
            methodFilterPresent |= methodFilter;

            for (Class<?> anInterface : ReflectionUtil.interfaces(aClass)) {
                handleInterface(anInterface, "instance" + i, methodFilter);
            }
        }

        if (!packageName().isEmpty())
            sourceCode.append(format("package %s;\n", packageName()));

        sourceCode.append("import net.openhft.chronicle.bytes.MethodReader;\n" +
                "import net.openhft.chronicle.core.Jvm;\n" +
                "import net.openhft.chronicle.core.util.ObjectUtils;\n" +
                "import net.openhft.chronicle.wire.*;\n" +
                "\n" +
                "import java.util.function.Supplier;\n\n");

        sourceCode.append(format("public class %s extends AbstractGeneratedMethodReader {\n", generatedClassName()));

        sourceCode.append("// instances on which parsed calls are invoked\n");

        for (int i = 0; i < instances.length; i++) {
            sourceCode.append(format("private final Object instance%d;\n", i));
        }

        sourceCode.append("\n");
        sourceCode.append(fields);

        if (methodFilterPresent) {
            sourceCode.append("// flag for handling ignoreMethodBasedOnFirstArg\n");
            sourceCode.append("private boolean ignored;\n\n");
        }

        if (numericConverters.length() > 0) {
            sourceCode.append("// numeric converters\n");
            sourceCode.append(numericConverters);
            sourceCode.append("\n");
        }

        if (!handledChainedInterfaces.isEmpty()) {
            sourceCode.append("// chained call results\n");
            sourceCode.append(chainedCallResults);
            sourceCode.append("\n");
        }

        sourceCode.append(format("public %s(MarshallableIn in, WireParselet debugLoggingParselet," +
                "Supplier<MethodReader> delegateSupplier, Object... instances) {\n" +
                "super(in, debugLoggingParselet, delegateSupplier);\n", generatedClassName()));

        for (int i = 0; i < instances.length - 1; i++)
            sourceCode.append(format("instance%d = instances[%d];\n", i, i));

        sourceCode.append(format("instance%d = instances[%d];\n}\n\n", instances.length - 1, instances.length - 1));

        sourceCode.append("@Override\n" +
                "protected boolean readOneCall(WireIn wireIn) {\n" +
                "String lastEventName = \"\";\n" +
                "if (wireIn.bytes().peekUnsignedByte() == BinaryWireCode.FIELD_NUMBER) {\n" +
                "int methodId = (int) wireIn.readEventNumber();\n" +
                "switch (methodId) {\n");

        sourceCode.append(eventIdSwitchBlock);

        sourceCode.append("default:\n" +
                "return false;\n" +
                "}\n" +
                "}\n" +
                "else {\n" +
                "lastEventName = wireIn.readEvent(String.class);\n" +
                "}\n" +
                "ValueIn valueIn = wireIn.getValueIn();\n" +
                "try {\n" +
                "if (Jvm.isDebug())\n" +
                "debugLoggingParselet.accept(lastEventName, valueIn);\n" +
                "switch (lastEventName) {\n" +
                "case MethodReader.HISTORY:\n" +
                "valueIn.marshallable(messageHistory);\n" +
                "break;\n\n");

        sourceCode.append(eventNameSwitchBlock);

        sourceCode.append("default:\n" +
                "return false;\n" +
                "}\n" +
                "return true;\n" +
                "} \n" +
                "catch (Exception e) {\n" +
                "Jvm.warn().on(this.getClass(), \"Failure to dispatch message, " +
                "will retry to process without generated code: \" + lastEventName + \"()\", e);\n" +
                "return false;\n" +
                "}\n" +
                "}\n" +
                "}\n");

        isSourceCodeGenerated = true;
    }

    /**
     * Generates code for handling all method calls of passed interface.
     * Called recursively for chained methods.
     *
     * @param anInterface       Processed interface.
     * @param instanceFieldName In generated code, methods are executed on field with this name.
     * @param methodFilter      <code>true</code> if passed interface is marked with {@link MethodFilterOnFirstArg}.
     */
    private void handleInterface(Class<?> anInterface, String instanceFieldName, boolean methodFilter) {
        if (!handledInterfaces.add(anInterface))
            return;

        for (@NotNull Method m : anInterface.getMethods()) {
            Class<?> declaringClass = m.getDeclaringClass();
            if (declaringClass == Object.class)
                continue;
            if (Modifier.isStatic(m.getModifiers()))
                continue;
            if ("ignoreMethodBasedOnFirstArg".equals(m.getName()))
                continue;

            try {
                // skip Object defined methods.
                Object.class.getMethod(m.getName(), m.getParameterTypes());
                continue;
            } catch (NoSuchMethodException e) {
                // not an Object method.
            }

            if (!handledMethodNames.add(m.getName())) {
                throw new IllegalStateException("MethodReader does not support overloaded methods. " +
                        "Method: " + m.toString());
            }

            handleMethod(m, anInterface, instanceFieldName, methodFilter);
        }
    }

    /**
     * Generates code for handling a method call.
     *
     * @param m                 Code for handling calls of this method is generated.
     * @param anInterface       Interface which method is processed.
     * @param instanceFieldName In generated code, method is executed on field with this name.
     * @param methodFilter      <code>true</code> if passed interface is marked with {@link MethodFilterOnFirstArg}.
     */
    private void handleMethod(Method m, Class<?> anInterface, String instanceFieldName, boolean methodFilter) {
        Jvm.setAccessible(m);

        Class<?>[] parameterTypes = m.getParameterTypes();

        Class<?> chainReturnType = m.getReturnType();
        if (!chainReturnType.isInterface() || Jvm.dontChain(chainReturnType))
            chainReturnType = null;

        if (parameterTypes.length > 0)
            fields.append(format("// %s\n", m.getName()));

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];

            final String typeName = parameterType.getCanonicalName();

            if (parameterType.isPrimitive())
                fields.append(format("private %s %sarg%d;\n", typeName, m.getName(), i));
            else {
                fields.append(format("private %s %sarg%dtype = ObjectUtils.implementationToUse(%s.class);\n",
                        "Class", m.getName(), i, typeName));

                final Class<?> implToUse = ObjectUtils.implementationToUse(parameterType);

                if (ReadMarshallable.class.isAssignableFrom(implToUse)) {
                    fields.append(format("private %s %sarg%d = ObjectUtils.newInstance(%s.class);\n",
                            typeName, m.getName(), i, implToUse.getCanonicalName()));
                } else
                    fields.append(format("private %s %sarg%d;\n", typeName, m.getName(), i));
            }
        }

        if (chainReturnType != null && handledChainedInterfaces.add(chainReturnType.getSimpleName()))
            chainedCallResults.append(format("private %s res%s;\n", chainReturnType.getCanonicalName(), chainReturnType.getSimpleName()));

        if (parameterTypes.length > 0)
            fields.append("\n");

        final MethodId methodIdAnnotation = Annotations.getAnnotation(m, MethodId.class);

        if (methodIdAnnotation != null) {
            int methodId = Maths.toInt32(methodIdAnnotation.value());

            eventIdSwitchBlock.append(format("case %d:\n", methodId));
            eventIdSwitchBlock.append(format("lastEventName = \"%s\";\n", m.getName()));
            eventIdSwitchBlock.append("break;\n\n");
        }

        String chainedCallPrefix = chainReturnType != null ? format("res%s = ", chainReturnType.getSimpleName()) : "";

        eventNameSwitchBlock.append(format("case \"%s\":\n", m.getName()));
        if (parameterTypes.length == 0) {
            eventNameSwitchBlock.append("valueIn.skipValue();\n");
            eventNameSwitchBlock.append(format("%s((%s) %s).%s();\n",
                    chainedCallPrefix, anInterface.getCanonicalName(), instanceFieldName, m.getName()));
        } else if (parameterTypes.length == 1) {
            eventNameSwitchBlock.append(argumentRead(m, 0, false));
            eventNameSwitchBlock.append(format("%s((%s) %s).%s(%sarg%d);\n",
                    chainedCallPrefix, anInterface.getCanonicalName(), instanceFieldName, m.getName(), m.getName(), 0));
        } else {
            if (methodFilter) {
                eventNameSwitchBlock.append("ignored = false;\n");
                eventNameSwitchBlock.append("valueIn.sequence(this, (f, v) -> {\n");
                eventNameSwitchBlock.append(argumentRead(m, 0, true));
                eventNameSwitchBlock.append(format("if (((MethodFilterOnFirstArg) f.%s)." +
                                "ignoreMethodBasedOnFirstArg(\"%s\", f.%sarg%d)) {\n",
                        instanceFieldName, m.getName(), m.getName(), 0));
                eventNameSwitchBlock.append("f.ignored = true;\n");
                eventNameSwitchBlock.append("}\n");
                eventNameSwitchBlock.append("else {\n");

                for (int i = 1; i < parameterTypes.length; i++)
                    eventNameSwitchBlock.append(argumentRead(m, i, true));

                eventNameSwitchBlock.append("}\n");
                eventNameSwitchBlock.append("});\n");
                eventNameSwitchBlock.append("if (!ignored)\n");
            } else {
                eventNameSwitchBlock.append("valueIn.sequence(this, (f, v) -> { " +
                        "// todo optimize megamorphic lambda call\n");

                for (int i = 0; i < parameterTypes.length; i++)
                    eventNameSwitchBlock.append(argumentRead(m, i, true));

                eventNameSwitchBlock.append("});\n");
            }

            List<String> args = new ArrayList<>();
            for (int i = 0; i < parameterTypes.length; i++)
                args.add(m.getName() + "arg" + i);

            eventNameSwitchBlock.append(format("%s((%s) %s).%s(%s);\n",
                    chainedCallPrefix, anInterface.getCanonicalName(), instanceFieldName, m.getName(),
                    String.join(", ", args)));
        }

        eventNameSwitchBlock.append("break;\n\n");

        if (chainReturnType != null)
            handleInterface(chainReturnType, "res" + chainReturnType.getSimpleName(), false);
    }

    /**
     * Generates code for reading an argument.
     * Side-effect: registers a converter as a field if {@link IntConversion} or {@link LongConversion} is used.
     *
     * @param m        Method for which an argument is read.
     * @param argIndex Index of an argument.
     * @param inLambda <code>true</code> if argument is read in lambda passed to a
     *                 {@link ValueIn#sequence(Object, BiConsumer)} call.
     * @return Code that retrieves specified argument from {@link ValueIn} input.
     */
    private String argumentRead(Method m, int argIndex, boolean inLambda) {
        Class<?> numericConversionClass = null;

        // Numeric conversion is not supported for binary wire
        if (wireType == WireType.TEXT || wireType == WireType.YAML) {
            Annotation[] annotations = m.getParameterAnnotations()[argIndex];

            for (Annotation a : annotations) {
                if (a instanceof IntConversion) {
                    numericConversionClass = ((IntConversion) a).value();
                    break;
                } else if (a instanceof LongConversion) {
                    numericConversionClass = ((LongConversion) a).value();
                    break;
                }
            }
        }

        final Class<?> argumentType = m.getParameterTypes()[argIndex];
        String trueArgumentName = m.getName() + "arg" + argIndex;
        String argumentName = (inLambda ? "f." : "") + trueArgumentName;

        String valueInName = inLambda ? "v" : "valueIn";

        if (boolean.class.equals(argumentType)) {
            return format("%s = %s.bool();\n", argumentName, valueInName);
        } else if (byte.class.equals(argumentType)) {
            return format("%s = %s.readByte();\n", argumentName, valueInName);
        } else if (char.class.equals(argumentType)) {
            return format("%s = %s.character();\n", argumentName, valueInName);
        } else if (short.class.equals(argumentType)) {
            return format("%s = %s.int16();\n", argumentName, valueInName);
        } else if (int.class.equals(argumentType)) {
            if (numericConversionClass != null && IntConverter.class.isAssignableFrom(numericConversionClass)) {
                numericConverters.append(format("private final %s %sConverter = ObjectUtils.newInstance(%s.class);\n",
                        numericConversionClass.getCanonicalName(), trueArgumentName, numericConversionClass.getCanonicalName()));

                return format("%s = %sConverter.parse(%s.text());\n", argumentName, argumentName, valueInName);
            } else
                return format("%s = %s.int32();\n", argumentName, valueInName);
        } else if (long.class.equals(argumentType)) {
            if (numericConversionClass != null && LongConverter.class.isAssignableFrom(numericConversionClass)) {
                numericConverters.append(format("private final %s %sConverter = ObjectUtils.newInstance(%s.class);\n",
                        numericConversionClass.getCanonicalName(), trueArgumentName, numericConversionClass.getCanonicalName()));

                return format("%s = %sConverter.parse(%s.text());\n", argumentName, argumentName, valueInName);
            } else
                return format("%s = %s.int64();\n", argumentName, valueInName);
        } else if (float.class.equals(argumentType)) {
            return format("%s = %s.float32();\n", argumentName, valueInName);
        } else if (double.class.equals(argumentType)) {
            return format("%s = %s.float64();\n", argumentName, valueInName);
        } else if (CharSequence.class.isAssignableFrom(argumentType)) {
            return format("%s = %s.text();\n", argumentName, valueInName);
        } else {
            return format("%s = %s.object(checkRecycle(%s), %stype);\n", argumentName, valueInName, argumentName, argumentName);
        }
    }

    /**
     * @return Package name of a generated class.
     */
    public String packageName() {
        Class<?> firstClass = instances[0].getClass();
        String firstClassFullName = firstClass.getName();

        int lastDot = firstClassFullName.lastIndexOf('.');

        if (lastDot != -1)
            return firstClassFullName.substring(0, lastDot);
        else
            return "";
    }

    /**
     * @return Simple name of a generated class.
     */
    public String generatedClassName() {
        final StringBuilder sb = new StringBuilder();

        for (Object i : instances) {
            if (i.getClass().getEnclosingClass() != null)
                sb.append(i.getClass().getEnclosingClass().getSimpleName());
            sb.append(i.getClass().getSimpleName());
        }

        if (wireType != null)
            sb.append(wireType.toString().replace("_", ""));

        sb.append("MethodReader");
        return sb.toString();
    }
}
