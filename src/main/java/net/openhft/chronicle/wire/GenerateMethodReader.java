/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.util.Annotations;
import net.openhft.chronicle.core.util.GenericReflection;
import net.openhft.chronicle.core.util.IgnoresEverything;
import net.openhft.chronicle.wire.utils.JavaSourceCodeFormatter;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static net.openhft.chronicle.core.util.GenericReflection.*;
import static net.openhft.chronicle.wire.GenerateMethodWriter.isSynthetic;

/**
 * Responsible for code generation and its runtime compilation of custom {@link MethodReader}s.
 */
public class GenerateMethodReader {
    private static final boolean DUMP_CODE = Jvm.getBoolean("dumpCode");
    private static final Set<Class<?>> IGNORED_INTERFACES = new LinkedHashSet<>();

    static {
        // make sure Wires static block called and classpath set up
        Wires.init();
        Collections.addAll(IGNORED_INTERFACES,
                BytesMarshallable.class,
                DocumentContext.class,
                ReadDocumentContext.class,
                WriteDocumentContext.class,
                ExcerptListener.class,
                FieldInfo.class,
                FieldNumberParselet.class,
                SelfDescribingMarshallable.class,
                BytesMarshallable.class,
                Marshallable.class,
                MarshallableIn.class,
                MarshallableOut.class,
                MethodWriter.class,
                SourceContext.class
        );
    }

    private final WireType wireType;
    private final Object[] metaDataHandler;
    private final Object[] instances;
    private final MethodReaderInterceptorReturns interceptor;
    private final Map<String, String> handledMethodNames = new HashMap<>();
    private final Set<String> handledMethodSignatures = new HashSet<>();
    private final Set<Class<?>> handledInterfaces = new HashSet<>();
    private final SourceCodeFormatter sourceCode = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter fields = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter eventNameSwitchBlock = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter eventNameSwitchBlockMeta = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter eventIdSwitchBlock = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter eventIdSwitchBlockMeta = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter numericConverters = new JavaSourceCodeFormatter();
    private final String generatedClassName;
    private final Set<String> fieldNames = new LinkedHashSet<>();
    private boolean methodFilterPresent;
    private boolean isSourceCodeGenerated;
    private boolean hasChainedCalls;

    public GenerateMethodReader(WireType wireType, MethodReaderInterceptorReturns interceptor, Object[] metaDataHandler, Object... instances) {
        this.wireType = wireType;
        this.interceptor = interceptor;
        this.metaDataHandler = metaDataHandler;
        this.instances = instances;
        this.generatedClassName = generatedClassName0();
    }

    private static String signature(Method m, Class type) {
        return GenericReflection.getReturnType(m, type) + " " + m.getName() + " " + Arrays.toString(GenericReflection.getParameterTypes(m, type));
    }

    static boolean hasInstance(Class<?> aClass) {
        try {
            aClass.getField("INSTANCE");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    /**
     * Generates and compiles in runtime code of a custom {@link MethodReader}.
     *
     * @return {@link MethodReader} implementation for specified {@link #instances}.
     */
    public Class<?> createClass() {
        if (!isSourceCodeGenerated)
            generateSourceCode();

        final ClassLoader classLoader = instances[0].getClass().getClassLoader();
        final String fullClassName = packageName() + "." + generatedClassName();

        try {
            return Wires.CACHED_COMPILER.loadFromJava(classLoader, fullClassName, sourceCode.toString());
        } catch (AssertionError e) {
            if (e.getCause() instanceof LinkageError) {
                try {
                    return Class.forName(fullClassName, true, classLoader);
                } catch (ClassNotFoundException x) {
                    throw Jvm.rethrow(x);
                }
            }
            throw Jvm.rethrow(e);
        } catch (Throwable e) {
            throw Jvm.rethrow(new ClassNotFoundException(e.getMessage() + '\n' + sourceCode, e));
        }
    }

    /**
     * Generates source code of {@link MethodReader} for specified {@link #instances}.
     */
    private void generateSourceCode() {
        handledInterfaces.clear();
        handledMethodNames.clear();
        handledMethodSignatures.clear();

        for (int i = 0; metaDataHandler != null && i < metaDataHandler.length; i++) {
            final Class<?> aClass = metaDataHandler[i].getClass();

            for (Class<?> anInterface : ReflectionUtil.interfaces(aClass)) {
                if (anInterface.getAnnotation(DontChain.class) != null)
                    continue;
                handleInterface(anInterface, "metaInstance" + i, false, eventNameSwitchBlockMeta, eventIdSwitchBlockMeta);
            }
        }

        handledInterfaces.clear();
        handledMethodNames.clear();
        handledMethodSignatures.clear();

        for (int i = 0; i < instances.length; i++) {
            final Class<?> aClass = instances[i].getClass();

            boolean methodFilter = instances[i] instanceof MethodFilterOnFirstArg;
            methodFilterPresent |= methodFilter;

            for (Class<?> anInterface : ReflectionUtil.interfaces(aClass)) {
                if (IGNORED_INTERFACES.contains(anInterface))
                    continue;
                handleInterface(anInterface, "instance" + i, methodFilter, eventNameSwitchBlock, eventIdSwitchBlock);
            }
        }

        if (!packageName().isEmpty())
            sourceCode.append(format("package %s;\n", packageName()));

        sourceCode.append("" +
                "import net.openhft.chronicle.core.Jvm;\n" +
                "import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;\n" +
                "import net.openhft.chronicle.core.util.ObjectUtils;\n" +
                "import net.openhft.chronicle.bytes.*;\n" +
                "import net.openhft.chronicle.wire.*;\n" +
                "import net.openhft.chronicle.wire.BinaryWireCode;\n" +
                "\n" +
                "import java.util.Map;\n" +
                "import java.lang.reflect.Method;\n" +
                "\n");

        sourceCode.append(format("public class %s extends AbstractGeneratedMethodReader {\n", generatedClassName()));

        sourceCode.append("// instances on which parsed calls are invoked\n");

        for (int i = 0; metaDataHandler != null && i < metaDataHandler.length; i++) {
            sourceCode.append(format("private final Object metaInstance%d;\n", i));
        }
        for (int i = 0; i < instances.length; i++) {
            sourceCode.append(format("private final Object instance%d;\n", i));
        }
        sourceCode.append("private final WireParselet defaultParselet;\n");
        sourceCode.append("\n");

        if (hasRealInterceptorReturns()) {
            sourceCode.append("// method reader interceptor\n");
            sourceCode.append("private final MethodReaderInterceptorReturns interceptor;\n");
            sourceCode.append("\n");
        }

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

        if (hasChainedCalls) {
            sourceCode.append("// chained call result\n");
            sourceCode.append("private Object chainedCallReturnResult;");
            sourceCode.append("\n");
        }

        sourceCode.append(format("public %s(MarshallableIn in, " +
                "WireParselet defaultParselet, " +
                "WireParselet debugLoggingParselet, " +
                "MethodReaderInterceptorReturns interceptor, " +
                "Object[] metaInstances, " +
                "Object[] instances) {\n" +
                "super(in, debugLoggingParselet);\n" +
                "this.defaultParselet = defaultParselet;\n", generatedClassName()));

        if (hasRealInterceptorReturns())
            sourceCode.append("this.interceptor = interceptor;\n");

        for (int i = 0; metaDataHandler != null && i < metaDataHandler.length; i++)
            sourceCode.append(format("metaInstance%d = metaInstances[%d];\n", i, i));

        for (int i = 0; i < instances.length - 1; i++)
            sourceCode.append(format("instance%d = instances[%d];\n", i, i));

        sourceCode.append(format("instance%d = instances[%d];\n}\n\n", instances.length - 1, instances.length - 1));

        if (hasChainedCalls) {
            sourceCode.append("" +
                            "@Override\n" +
                            "public boolean restIgnored() {\n" +
                            "  return chainedCallReturnResult instanceof ")
                    .append(IgnoresEverything.class.getName())
                    .append(";\n" +
                            "}\n");
        }

        sourceCode.append("@Override\n" +
                "protected boolean readOneCall(WireIn wireIn) {\n" +
                "ValueIn valueIn = wireIn.getValueIn();\n" +
                "String lastEventName = \"\";\n" +
                "if (wireIn.bytes().peekUnsignedByte() == BinaryWireCode.FIELD_NUMBER) {\n" +
                "int methodId = (int) wireIn.readEventNumber();\n" +
                "switch (methodId) {\n");

        addMethodIdSwitch(MethodReader.HISTORY, MethodReader.MESSAGE_HISTORY_METHOD_ID, eventIdSwitchBlock);
        sourceCode.append(eventIdSwitchBlock);

        sourceCode.append("default:\n" +
                // below should be garbage-free if methodId is low. This will now drop through for defaultParselet
                "lastEventName = Integer.toString(methodId);\n" +
                "break;\n" +
                "}\n" +
                "}\n" +
                "else {\n" +
                "lastEventName = wireIn.readEvent(String.class);\n" +
                "}\n" +
                "try {\n" +
                "if (Jvm.isDebug())\n" +
                "debugLoggingParselet.accept(lastEventName, valueIn);\n" +
                "if (lastEventName == null)\n" +
                "throw new IllegalStateException(\"Failed to read method name or ID\");\n" +
                "switch (lastEventName) {\n" +
                "case MethodReader.HISTORY:\n" +
                "valueIn.marshallable(messageHistory);\n" +
                "break;\n\n");

        sourceCode.append(eventNameSwitchBlock);

        sourceCode.append("default:\n" +
                "defaultParselet.accept(lastEventName, valueIn);\n" +
                "return true;\n" +
                "}\n" +
                "return true;\n" +
                "} \n" +
                "catch (InvocationTargetRuntimeException e) {\n" +
                "throw e;\n" +
                "}\n" +
                "}\n");

        sourceCode.append("@Override\n" +
                "protected boolean readOneCallMeta(WireIn wireIn) {\n" +
                "ValueIn valueIn = wireIn.getValueIn();\n" +
                "String lastEventName = \"\";\n" +
                "if (wireIn.bytes().peekUnsignedByte() == BinaryWireCode.FIELD_NUMBER) {\n" +
                "int methodId = (int) wireIn.readEventNumber();\n" +
                "switch (methodId) {\n");

        sourceCode.append(eventIdSwitchBlockMeta);

        sourceCode.append("default:\n" +
                "valueIn.skipValue();\n" +
                "return true;\n" +
                "}\n" +
                "}\n" +
                "else {\n" +
                "lastEventName = wireIn.readEvent(String.class);\n" +
                "}\n" +
                "try {\n" +
                "if (Jvm.isDebug())\n" +
                "debugLoggingParselet.accept(lastEventName, valueIn);\n" +
                "if (lastEventName == null)\n" +
                "throw new IllegalStateException(\"Failed to read method name or ID\");\n" +
                "switch (lastEventName) {\n" +
                "case MethodReader.HISTORY:\n" +
                "valueIn.marshallable(messageHistory);\n" +
                "break;\n\n");

        sourceCode.append(eventNameSwitchBlockMeta);

        sourceCode.append("default:\n" +
                "valueIn.skipValue();\n" +
                "return true;\n" +
                "}\n" +
                "return true;\n" +
                "} \n" +
                "catch (InvocationTargetRuntimeException e) {\n" +
                "throw e;\n" +
                "}\n" +
                "}\n}\n");

        isSourceCodeGenerated = true;

        if (DUMP_CODE)
            System.out.println(sourceCode.toString());
    }

    /**
     * Generates code for handling all method calls of passed interface.
     * Called recursively for chained methods.
     *
     * @param anInterface          Processed interface.
     * @param instanceFieldName    In generated code, methods are executed on field with this name.
     * @param methodFilter         <code>true</code> if passed interface is marked with {@link MethodFilterOnFirstArg}.
     * @param eventNameSwitchBlock
     * @param eventIdSwitchBlock
     */
    private void handleInterface(Class<?> anInterface, String instanceFieldName, boolean methodFilter, SourceCodeFormatter eventNameSwitchBlock, SourceCodeFormatter eventIdSwitchBlock) {
        if (Jvm.dontChain(anInterface))
            return;
        if (!handledInterfaces.add(anInterface))
            return;

        for (@NotNull Method m : anInterface.getMethods()) {
            Class<?> declaringClass = m.getDeclaringClass();
            if (declaringClass == Object.class)
                continue;
            final int modifiers = m.getModifiers();
            if (Modifier.isStatic(modifiers) || isSynthetic(modifiers))
                continue;
            final String signature = signature(m, anInterface);
            if (!handledMethodSignatures.add(signature))
                continue;

            final String methodName = m.getName();
            try {
                // skip Object defined methods.
                Object.class.getMethod(methodName, m.getParameterTypes());
                continue;
            } catch (NoSuchMethodException e) {
                // not an Object method.
            }

            if (handledMethodNames.containsKey(methodName)) {
                throw new IllegalStateException("MethodReader does not support overloaded methods. " +
                        "Method: " + handledMethodNames.get(methodName) +
                        ", and: " + signature);
            }
            handledMethodNames.put(methodName, signature);

            handleMethod(m, anInterface, instanceFieldName, methodFilter, eventNameSwitchBlock, eventIdSwitchBlock);
        }
    }

    /**
     * Generates code for handling a method call.
     *
     * @param m                  Code for handling calls of this method is generated.
     * @param anInterface        Interface which method is processed.
     * @param instanceFieldName  In generated code, method is executed on field with this name.
     * @param methodFilter       <code>true</code> if passed interface is marked with {@link MethodFilterOnFirstArg}.
     * @param eventIdSwitchBlock
     */
    private void handleMethod(Method m, Class<?> anInterface, String instanceFieldName, boolean methodFilter, SourceCodeFormatter eventNameSwitchBlock, SourceCodeFormatter eventIdSwitchBlock) {
        Jvm.setAccessible(m);

        Type[] parameterTypes = getParameterTypes(m, anInterface);

        Class<?> chainReturnType = erase(getReturnType(m, anInterface));
        if (chainReturnType != DocumentContext.class && (!chainReturnType.isInterface() || Jvm.dontChain(chainReturnType)))
            chainReturnType = null;

        if (parameterTypes.length > 0 || hasRealInterceptorReturns())
            fields.append(format("// %s\n", m.getName()));

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = erase(parameterTypes[i]);

            final String typeName = parameterType.getCanonicalName();
            String fieldName = m.getName() + "arg" + i;
            if (fieldNames.add(fieldName)) {
                if (parameterType == Bytes.class)
                    fields.append(format("private Bytes %s = Bytes.allocateElasticOnHeap();\n", fieldName));
                else
                    fields.append(format("private %s %s;\n", typeName, fieldName));
            }
        }

        if (chainReturnType != null)
            hasChainedCalls = true;

        if (hasRealInterceptorReturns()) {
            fields.append(format("private final Object[] interceptor%sArgs = new Object[%d];\n",
                    m.getName(), parameterTypes.length));

            String parameterTypesArg = parameterTypes.length == 0 ? "" :
                    ", " + Arrays.stream(parameterTypes)
                            .map(t -> erase(t).getCanonicalName())
                            .map(s -> s + ".class")
                            .collect(Collectors.joining(", "));

            fields.append(format("private static final Method %smethod = lookupMethod(%s.class, \"%s\"%s);\n",
                    m.getName(), anInterface.getCanonicalName(), m.getName(), parameterTypesArg));
        }

        if (parameterTypes.length > 0 || hasRealInterceptorReturns())
            fields.append("\n");

        final MethodId methodIdAnnotation = Annotations.getAnnotation(m, MethodId.class);

        if (methodIdAnnotation != null) {
            int methodId = Maths.toInt32(methodIdAnnotation.value());
            addMethodIdSwitch(m.getName(), methodId, eventIdSwitchBlock);
        }

        String chainedCallPrefix = chainReturnType != null ? "chainedCallReturnResult = " : "";

        eventNameSwitchBlock.append(format("case \"%s\":\n", m.getName()));
        if (parameterTypes.length == 0) {
            eventNameSwitchBlock.append("valueIn.skipValue();\n");
            eventNameSwitchBlock.append(methodCall(m, instanceFieldName, chainedCallPrefix, chainReturnType));
        } else if (parameterTypes.length == 1) {
            eventNameSwitchBlock.append(argumentRead(m, 0, false, parameterTypes));
            eventNameSwitchBlock.append(methodCall(m, instanceFieldName, chainedCallPrefix, chainReturnType));
        } else {
            if (methodFilter) {
                eventNameSwitchBlock.append("ignored = false;\n");
                eventNameSwitchBlock.append("valueIn.sequence(this, (f, v) -> {\n");
                eventNameSwitchBlock.append(argumentRead(m, 0, true, parameterTypes));
                eventNameSwitchBlock.append(format("if (((MethodFilterOnFirstArg) f.%s)." +
                                "ignoreMethodBasedOnFirstArg(\"%s\", f.%sarg%d)) {\n",
                        instanceFieldName, m.getName(), m.getName(), 0));
                eventNameSwitchBlock.append("f.ignored = true;\n");

                for (int i = 1; i < parameterTypes.length; i++)
                    eventNameSwitchBlock.append("v.skipValue();\n");

                eventNameSwitchBlock.append("}\n");
                eventNameSwitchBlock.append("else {\n");

                for (int i = 1; i < parameterTypes.length; i++)
                    eventNameSwitchBlock.append(argumentRead(m, i, true, parameterTypes));

                eventNameSwitchBlock.append("}\n");
                eventNameSwitchBlock.append("});\n");
                eventNameSwitchBlock.append("if (!ignored) {\n");
            } else {
                eventNameSwitchBlock.append("valueIn.sequence(this, (f, v) -> { " +
                        "// todo optimize megamorphic lambda call\n");

                for (int i = 0; i < parameterTypes.length; i++)
                    eventNameSwitchBlock.append(argumentRead(m, i, true, parameterTypes));

                eventNameSwitchBlock.append("});\n");
            }

            eventNameSwitchBlock.append(methodCall(m, instanceFieldName, chainedCallPrefix, chainReturnType));

            if (methodFilter)
                eventNameSwitchBlock.append("}\n");
        }

        if (chainReturnType == DocumentContext.class) {
            eventNameSwitchBlock.append("wireIn.copyTo(((")
                    .append(DocumentContext.class.getName())
                    .append(") chainedCallReturnResult).wire());\n")

                    .append(Closeable.class.getName())
                    .append(".closeQuietly(chainedCallReturnResult);\n" +
                            "chainedCallReturnResult = null;\n");
            chainReturnType = null;
        }
        eventNameSwitchBlock.append("break;\n\n");

        if (chainReturnType != null)
            handleInterface(chainReturnType, "chainedCallReturnResult", false, eventNameSwitchBlock, eventIdSwitchBlock);
    }

    private void addMethodIdSwitch(String methodName, int methodId, SourceCodeFormatter eventIdSwitchBlock) {
        eventIdSwitchBlock.append(format("case %d:\n", methodId));
        eventIdSwitchBlock.append(format("lastEventName = \"%s\";\n", methodName));
        eventIdSwitchBlock.append("break;\n\n");
    }

    /**
     * Generates code that invokes passed method, saves method return value (in case it's a chained call)
     * and handles {@link MethodReaderInterceptorReturns} if it's specified.
     *
     * @param m                 Method that is being processed.
     * @param instanceFieldName In generated code, method is executed on field with this name.
     * @param chainedCallPrefix Prefix for method call statement, passed in order to save method result for chaining.
     * @return Code that performs a method call.
     */
    private String methodCall(Method m, String instanceFieldName, String chainedCallPrefix, @Nullable Class<?> returnType) {
        StringBuilder res = new StringBuilder();

        Class<?>[] parameterTypes = m.getParameterTypes();

        String[] args = new String[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++)
            args[i] = m.getName() + "arg" + i;

        res.append("try {\n");
        res.append("dataEventProcessed = true;\n");

        // called for no interceptor and a generating interceptor
        if (!hasRealInterceptorReturns()) {
            GeneratingMethodReaderInterceptorReturns generatingInterceptor = interceptor != null ?
                    (GeneratingMethodReaderInterceptorReturns) interceptor : null;

            if (generatingInterceptor != null) {
                final String codeBefore = generatingInterceptor.codeBeforeCall(m, instanceFieldName, args);

                if (codeBefore != null)
                    res.append(codeBefore).append("\n");
            }

            res.append(format("%s((%s) %s).%s(%s);\n",
                    chainedCallPrefix, m.getDeclaringClass().getCanonicalName(), instanceFieldName, m.getName(),
                    String.join(", ", args)));

            if (generatingInterceptor != null) {
                final String codeAfter = generatingInterceptor.codeAfterCall(m, instanceFieldName, args);

                if (codeAfter != null)
                    res.append(codeAfter).append("\n");
            }
        } else {
            // called for non generating interceptor
            for (int i = 0; i < parameterTypes.length; i++) {
                res.append(format("interceptor%sArgs[%d] = %sarg%d;\n", m.getName(), i, m.getName(), i));
            }

            String castPrefix = chainedCallPrefix.isEmpty() || returnType == null ?
                    "" : "(" + returnType.getCanonicalName() + ")";

            res.append(format("%s%sinterceptor.intercept(%smethod, %s, " +
                            "interceptor%sArgs, this::actualInvoke);\n",
                    chainedCallPrefix, castPrefix, m.getName(), instanceFieldName, m.getName()));
        }

        res.append("} \n" +
                "catch (Exception e) {\n" +
                "throw new InvocationTargetRuntimeException(e);\n" +
                "}\n");

        return res.toString();
    }

    /**
     * Generates code for reading an argument.
     * Side-effect: registers a converter as a field if {@link IntConversion} or {@link LongConversion} is used.
     *
     * @param m              Method for which an argument is read.
     * @param argIndex       Index of an argument.
     * @param inLambda       <code>true</code> if argument is read in lambda passed to a
     *                       {@link ValueIn#sequence(Object, BiConsumer)} call.
     * @param parameterTypes
     * @return Code that retrieves specified argument from {@link ValueIn} input.
     */
    private String argumentRead(Method m, int argIndex, boolean inLambda, Type[] parameterTypes) {
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
                } else {
                    LongConversion lc = Jvm.findAnnotation(a.annotationType(), LongConversion.class);
                    if (lc != null) {
                        numericConversionClass = lc.value();
                        break;
                    }
                }
            }
        }

        final Class<?> argumentType = erase(parameterTypes[argIndex]);
        String trueArgumentName = m.getName() + "arg" + argIndex;
        String argumentName = (inLambda ? "f." : "") + trueArgumentName;

        String valueInName = inLambda ? "v" : "valueIn";

        if (boolean.class.equals(argumentType)) {
            return format("%s = %s.bool();\n", argumentName, valueInName);
        } else if (byte.class.equals(argumentType)) {
            if (numericConversionClass != null && hasInstance(numericConversionClass)) {
                return format("%s = (byte) %s.INSTANCE.parse(%s.text());\n", argumentName, numericConversionClass.getName(), valueInName);

            } else if (numericConversionClass != null && LongConverter.class.isAssignableFrom(numericConversionClass)) {
                numericConverters.append(format("private final %s %sConverter = ObjectUtils.newInstance(%s.class);\n",
                        numericConversionClass.getCanonicalName(), trueArgumentName, numericConversionClass.getCanonicalName()));

                return format("%s = (byte) %sConverter.parse(%s.text());\n", argumentName, argumentName, valueInName);
            } else
                return format("%s = %s.readByte();\n", argumentName, valueInName);
        } else if (char.class.equals(argumentType)) {
            return format("%s = %s.character();\n", argumentName, valueInName);
        } else if (short.class.equals(argumentType)) {
            if (numericConversionClass != null && hasInstance(numericConversionClass)) {
                return format("%s = (short) %s.INSTANCE.parse(%s.text());\n", argumentName, numericConversionClass.getName(), valueInName);

            } else if (numericConversionClass != null && LongConverter.class.isAssignableFrom(numericConversionClass)) {
                numericConverters.append(format("private final %s %sConverter = ObjectUtils.newInstance(%s.class);\n",
                        numericConversionClass.getCanonicalName(), trueArgumentName, numericConversionClass.getCanonicalName()));

                return format("%s = (short) %sConverter.parse(%s.text());\n", argumentName, argumentName, valueInName);
            } else
                return format("%s = %s.int16();\n", argumentName, valueInName);
        } else if (int.class.equals(argumentType)) {
            if (numericConversionClass != null && hasInstance(numericConversionClass)) {
                return format("%s = (int) %s.INSTANCE.parse(%s.text());\n", argumentName, numericConversionClass.getName(), valueInName);

            } else if (numericConversionClass != null && LongConverter.class.isAssignableFrom(numericConversionClass)) {
                numericConverters.append(format("private final %s %sConverter = ObjectUtils.newInstance(%s.class);\n",
                        numericConversionClass.getCanonicalName(), trueArgumentName, numericConversionClass.getCanonicalName()));

                return format("%s = (int) %sConverter.parse(%s.text());\n", argumentName, argumentName, valueInName);
            } else if (numericConversionClass != null && IntConverter.class.isAssignableFrom(numericConversionClass)) {
                numericConverters.append(format("private final %s %sConverter = ObjectUtils.newInstance(%s.class);\n",
                        numericConversionClass.getCanonicalName(), trueArgumentName, numericConversionClass.getCanonicalName()));

                return format("%s = %sConverter.parse(%s.text());\n", argumentName, argumentName, valueInName);
            } else
                return format("%s = %s.int32();\n", argumentName, valueInName);
        } else if (long.class.equals(argumentType)) {
            if (numericConversionClass != null && hasInstance(numericConversionClass)) {
                return format("%s = %s.INSTANCE.parse(%s.text());\n", argumentName, numericConversionClass.getName(), valueInName);

            } else if (numericConversionClass != null && LongConverter.class.isAssignableFrom(numericConversionClass)) {
                numericConverters.append(format("private final %s %sConverter = ObjectUtils.newInstance(%s.class);\n",
                        numericConversionClass.getCanonicalName(), trueArgumentName, numericConversionClass.getCanonicalName()));

                return format("%s = %sConverter.parse(%s.text());\n", argumentName, argumentName, valueInName);
            } else {
                return format("%s = %s.int64();\n", argumentName, valueInName);
            }
        } else if (float.class.equals(argumentType)) {
            return format("%s = %s.float32();\n", argumentName, valueInName);
        } else if (double.class.equals(argumentType)) {
            return format("%s = %s.float64();\n", argumentName, valueInName);
        } else if (Bytes.class.isAssignableFrom(argumentType)) {
            return format("%s.bytes(%s);\n", valueInName, argumentName);
        } else if (CharSequence.class.isAssignableFrom(argumentType)) {
            return format("%s = %s.text();\n", argumentName, valueInName);
        } else {
            final String typeName = argumentType.getCanonicalName();
            return format("%s = %s.object(checkRecycle(%s), %s.class);\n", argumentName, valueInName, argumentName, typeName);
        }
    }

    private boolean hasRealInterceptorReturns() {
        return interceptor != null && !(interceptor instanceof GeneratingMethodReaderInterceptorReturns);
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
        return generatedClassName;
    }

    @NotNull
    private String generatedClassName0() {
        final StringBuilder sb = new StringBuilder();

        for (Object i : instances)
            appendInstanceName(sb, i);

        if (metaDataHandler != null)
            for (Object i : metaDataHandler)
                appendInstanceName(sb, i);

        if (wireType != null)
            sb.append(wireType.toString()
                    .replace("_", ""));

        if (interceptor instanceof GeneratingMethodReaderInterceptorReturns)
            sb.append(((GeneratingMethodReaderInterceptorReturns) interceptor).generatorId());
        else if (hasRealInterceptorReturns())
            sb.append("Intercepting");

        sb.append("MethodReader");
        return sb.toString().replace("/", "$");
    }

    private void appendInstanceName(StringBuilder sb, Object i) {
        final Class<?> aClass = i.getClass();

        if (aClass.getEnclosingClass() != null)
            sb.append(aClass.getEnclosingClass().getSimpleName());

        String name = aClass.getName();
        if (name.contains("$$Lambda$"))
            name = aClass.getInterfaces()[0].getName();

        final int packageDelimiterIndex = name.lastIndexOf('.');

        // Intentionally using this instead of class.simpleName() in order to support anonymous class
        String nameWithoutPackage = packageDelimiterIndex == -1 ? name : name.substring(packageDelimiterIndex + 1);

        if (aClass.isSynthetic() && !aClass.isAnonymousClass() && !aClass.isLocalClass()) {
            int lambdaSlashIndex = nameWithoutPackage.lastIndexOf("/");

            if (lambdaSlashIndex != -1)
                nameWithoutPackage = nameWithoutPackage.substring(0, lambdaSlashIndex);
        }

        sb.append(nameWithoutPackage);
    }
}
