/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.util.GenericReflection;
import net.openhft.chronicle.core.util.IgnoresEverything;
import net.openhft.chronicle.wire.utils.JavaSourceCodeFormatter;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static net.openhft.chronicle.core.util.GenericReflection.*;
import static net.openhft.chronicle.wire.GenerateMethodWriter.isSynthetic;

/**
 * Responsible for code generation and its runtime compilation of custom {@link MethodReader}s.
 * The class dynamically generates Java source code based on the provided configurations and compiles them at runtime.
 * It offers the flexibility to create custom MethodReaders tailored to specific needs without manual coding.
 */
public class GenerateMethodReader {

     // Configuration flag for dumping the generated code.
    private static final boolean DUMP_CODE = Jvm.getBoolean("dumpCode");
    // Set of interfaces that are not meant to be processed.
    private static final Set<Class<?>> IGNORED_INTERFACES = new LinkedHashSet<>();

    static {
        // Initialization: ensuring Wires classes are loaded and classpath is set up
        Wires.init();
        // Populate IGNORED_INTERFACES with predefined interfaces
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

    // Configuration for the type of wire to use for serialization/deserialization.
    private final WireType wireType;

     // Handlers for metadata during the method reader generation.
    private final Object[] metaDataHandler;

    // Instances of the classes/interfaces for which method readers are to be generated.
    private final Object[] instances;

    // Interceptor for handling method returns during the method reader generation.
    private final MethodReaderInterceptorReturns interceptor;
    // A mapping to ensure unique method names for the generated code.
    private final Map<String, String> handledMethodNames = new HashMap<>();
    // A set to store method signatures that have been processed.
    private final Set<String> handledMethodSignatures = new HashSet<>();
    // A set to store interfaces that have been processed.
    private final Set<Class<?>> handledInterfaces = new HashSet<>();
    // Buffers for holding generated Java source code.
    private final SourceCodeFormatter sourceCode = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter fields = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter eventNameSwitchBlock = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter eventNameSwitchBlockMeta = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter eventIdSwitchBlock = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter eventIdSwitchBlockMeta = new JavaSourceCodeFormatter();
    private final SourceCodeFormatter numericConverters = new JavaSourceCodeFormatter();
    // Name of the class that will be generated.
    private final String generatedClassName;
    // Set of field names to ensure uniqueness in the generated code.
    private final Set<String> fieldNames = new LinkedHashSet<>();

    // Flag indicating the presence of a method filter in the generated code.
    private boolean methodFilterPresent;

    // Flag indicating whether the source code has been generated.
    private boolean isSourceCodeGenerated;

    // Flag indicating if there are chained method calls in the generated code.
    private boolean hasChainedCalls;

    /**
     * Constructs a new instance of GenerateMethodReader.
     * Initializes the required configurations, metadata handlers, and instances which are essential for code generation.
     *
     * @param wireType Configuration for serialization/deserialization
     * @param interceptor An instance of MethodReaderInterceptorReturns
     * @param metaDataHandler Array of meta-data handlers
     * @param instances Instances that dictate the structure of the generated MethodReader
     */
    public GenerateMethodReader(WireType wireType, MethodReaderInterceptorReturns interceptor, Object[] metaDataHandler, Object... instances) {
        this.wireType = wireType;
        this.interceptor = interceptor;
        this.metaDataHandler = metaDataHandler;
        this.instances = instances;
        this.generatedClassName = generatedClassName0();
    }

    /**
     * Computes the signature of a given method.
     * The signature comprises the return type, method name, and parameter types.
     *
     * @param m The method for which the signature is to be computed
     * @param type The type under consideration
     * @return A string representing the method's signature
     */
    private static String signature(Method m, Class type) {
        return GenericReflection.getReturnType(m, type) + " " + m.getName() + " " + Arrays.toString(GenericReflection.getParameterTypes(m, type));
    }

    /**
     * Checks if the given class has an "INSTANCE" field.
     * Useful to verify if a class adheres to certain patterns or conventions.
     *
     * @param aClass The class to be checked
     * @return true if the class has an "INSTANCE" field, false otherwise
     */
    static boolean hasInstance(Class<?> aClass) {
        try {
            aClass.getField("INSTANCE");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    /**
     * Generates and compiles the source code of a custom {@link MethodReader} at runtime.
     * It uses the configurations and instances provided during initialization.
     * If there are issues during compilation, it provides detailed error messages for easier debugging.
     *
     * @return A new class representing the custom MethodReader
     */
    public Class<?> createClass() {
        // If source code isn't already generated, generate it.
        if (!isSourceCodeGenerated)
            generateSourceCode();

        final ClassLoader classLoader = instances[0].getClass().getClassLoader();
        final String fullClassName = packageName() + "." + generatedClassName();

        try {
            return Wires.loadFromJava(classLoader, fullClassName, sourceCode.toString());
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
     * Responsible for generating the source code of a {@link MethodReader} based on specified {@link #instances}.
     * This method encapsulates the logic for building the source code dynamically, by inspecting provided interfaces,
     * filtering ignored interfaces, and appending necessary components to the code.
     */
    private void generateSourceCode() {
        // Clear previously handled interfaces and method signatures for clean generation.
        handledInterfaces.clear();
        handledMethodNames.clear();
        handledMethodSignatures.clear();

        // Handle meta data handlers and their associated interfaces.
        for (int i = 0; metaDataHandler != null && i < metaDataHandler.length; i++) {
            final Class<?> aClass = metaDataHandler[i].getClass();

            // Process each interface of the meta data handler.
            for (Class<?> anInterface : ReflectionUtil.interfaces(aClass)) {
                if (Jvm.dontChain(anInterface))
                    continue;
                handleInterface(anInterface, "metaInstance" + i, false, eventNameSwitchBlockMeta, eventIdSwitchBlockMeta);
            }
        }

        // Clear previously handled interfaces and method signatures again to process instance interfaces.
        handledInterfaces.clear();
        handledMethodNames.clear();
        handledMethodSignatures.clear();

        // Handle instances and their associated interfaces.
        for (int i = 0; i < instances.length; i++) {
            final Class<?> aClass = instances[i].getClass();

            // Check if the instance has method filters.
            boolean methodFilter = instances[i] instanceof MethodFilterOnFirstArg;
            methodFilterPresent |= methodFilter;

            // Process each interface of the instance.
            for (Class<?> anInterface : ReflectionUtil.interfaces(aClass)) {
                if (IGNORED_INTERFACES.contains(anInterface))
                    continue;
                handleInterface(anInterface, "instance" + i, methodFilter, eventNameSwitchBlock, eventIdSwitchBlock);
            }
        }

        // Start constructing the source code.
        if (!packageName().isEmpty())
            sourceCode.append(format("package %s;\n", packageName()));

        // Import statements required for the generated code.
        sourceCode.append("" +
                "import net.openhft.chronicle.core.Jvm;\n" +
                "import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;\n" +
                "import net.openhft.chronicle.core.util.ObjectUtils;\n" +
                "import net.openhft.chronicle.bytes.*;\n" +
                "import net.openhft.chronicle.wire.*;\n" +
                "import net.openhft.chronicle.wire.utils.*;\n" +
                "import net.openhft.chronicle.wire.BinaryWireCode;\n" +
                "\n" +
                "import java.util.Map;\n" +
                "import java.lang.reflect.Method;\n" +
                "\n");

        // Declare the generated class extending AbstractGeneratedMethodReader.
        sourceCode.append(format("public class %s extends AbstractGeneratedMethodReader {\n", generatedClassName()));

        // Inline comment for instances section.
        sourceCode.append("// instances on which parsed calls are invoked\n");

        for (int i = 0; metaDataHandler != null && i < metaDataHandler.length; i++) {
            sourceCode.append(format("private final Object metaInstance%d;\n", i));
        }
        for (int i = 0; i < instances.length; i++) {
            sourceCode.append(format("private final Object instance%d;\n", i));
        }
        sourceCode.append("private final WireParselet defaultParselet;\n");
        sourceCode.append("\n");

        // Check and declare an interceptor if one is present.
        if (hasRealInterceptorReturns()) {
            sourceCode.append("// method reader interceptor\n");
            sourceCode.append("private final MethodReaderInterceptorReturns interceptor;\n");
            sourceCode.append("\n");
        }

        // Appending fields to the source code.
        sourceCode.append(fields);

        // If a method filter is present, add an ignored flag.
        if (methodFilterPresent) {
            sourceCode.append("// flag for handling ignoreMethodBasedOnFirstArg\n");
            sourceCode.append("private boolean ignored;\n\n");
        }

        // Add numeric converters if any are present.
        if (numericConverters.length() > 0) {
            sourceCode.append("// numeric converters\n");
            sourceCode.append(numericConverters);
            sourceCode.append("\n");
        }

        // Add result for chained calls if any are present.
        if (hasChainedCalls) {
            sourceCode.append("// chained call result\n");
            sourceCode.append("private Object chainedCallReturnResult;");
            sourceCode.append("\n");
        }

        // Define the constructor of the generated class.
        sourceCode.append(format("public %s(MarshallableIn in, " +
                "WireParselet defaultParselet, " +
                "WireParselet debugLoggingParselet, " +
                "MethodReaderInterceptorReturns interceptor, " +
                "Object[] metaInstances, " +
                "Object[] instances) {\n" +
                "super(in, debugLoggingParselet);\n" +
                "this.defaultParselet = defaultParselet;\n", generatedClassName()));

        // Set interceptor if one is present.
        if (hasRealInterceptorReturns())
            sourceCode.append("this.interceptor = interceptor;\n");

        // Initialize metaInstance objects.
        for (int i = 0; metaDataHandler != null && i < metaDataHandler.length; i++)
            sourceCode.append(format("metaInstance%d = metaInstances[%d];\n", i, i));

        // Initialize instance objects.
        for (int i = 0; i < instances.length - 1; i++)
            sourceCode.append(format("instance%d = instances[%d];\n", i, i));

        sourceCode.append(format("instance%d = instances[%d];\n}\n\n", instances.length - 1, instances.length - 1));

        // Override the restIgnored method if chained calls are present.
        if (hasChainedCalls) {
            sourceCode.append("@Override\n" +
                            "public boolean restIgnored() {\n" +
                            "  return chainedCallReturnResult instanceof ")
                    .append(IgnoresEverything.class.getName())
                    .append(";\n" +
                            "}\n");
        }

        // Define the readOneGenerated method for the MethodReader.
        sourceCode.append("@Override\n" +
                "protected MethodReaderStatus readOneGenerated(WireIn wireIn) {\n" +
                "ValueIn valueIn = wireIn.getValueIn();\n" +
                "String lastEventName = \"\";\n" +
                "if (wireIn.bytes().peekUnsignedByte() == BinaryWireCode.FIELD_NUMBER) {\n" +
                "int methodId = (int) wireIn.readEventNumber();\n" +
                "switch (methodId) {\n");

        // Append method ID switch logic.
        addMethodIdSwitch(MethodReader.HISTORY, MethodReader.MESSAGE_HISTORY_METHOD_ID, eventIdSwitchBlock);
        sourceCode.append(eventIdSwitchBlock);

        // Handle default case and read the event name.
        sourceCode.append("default:\n" +
                // Comment explaining the garbage-free nature for low methodIds.
                "lastEventName = Integer.toString(methodId);\n" +
                "break;\n" +
                "}\n" +
                "}\n" +
                "else {\n" +
                "lastEventName = wireIn.readEvent(String.class);\n" +
                "}\n" +
                // Try-catch block for reading method names.
                "try {\n" +
                "if (Jvm.isDebug())\n" +
                "debugLoggingParselet.accept(lastEventName, valueIn);\n" +
                "if (lastEventName == null)\n" +
                "throw new IllegalStateException(\"Failed to read method name or ID\");\n" +
                "switch (lastEventName) {\n");
        // History case logic.
        if (!eventNameSwitchBlock.contains("case \"history\":"))
            sourceCode.append("case MethodReader.HISTORY:\n" +
                    "valueIn.marshallable(messageHistory);\n" +
                    "return MethodReaderStatus.HISTORY;\n\n");

        // Append to the source code based on event name.
        sourceCode.append(eventNameSwitchBlock);

        // Handle default case if the event name isn't recognized.
        sourceCode.append("default:\n" +
                "defaultParselet.accept(lastEventName, valueIn);\n" +
                "return MethodReaderStatus.UNKNOWN;\n" +
                "}\n");

        // Check if we've handled the event successfully.
        if (eventNameSwitchBlock.contains("break;"))
            sourceCode.append("return MethodReaderStatus.KNOWN;\n");
        sourceCode.append("} \n" +
                "catch (InvocationTargetRuntimeException e) {\n" +
                "throw e;\n" +
                "}\n" +
                "}\n");

        // Handle meta-generated events using a similar logic.
        sourceCode.append("@Override\n" +
                "protected MethodReaderStatus readOneMetaGenerated(WireIn wireIn) {\n" +
                "ValueIn valueIn = wireIn.getValueIn();\n" +
                "String lastEventName = \"\";\n" +
                "if (wireIn.bytes().peekUnsignedByte() == BinaryWireCode.FIELD_NUMBER) {\n" +
                "int methodId = (int) wireIn.readEventNumber();\n" +
                "switch (methodId) {\n");

        sourceCode.append(eventIdSwitchBlockMeta);

        sourceCode.append("default:\n" +
                "valueIn.skipValue();\n" +
                "return MethodReaderStatus.UNKNOWN;\n" +
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
                "return MethodReaderStatus.HISTORY;\n\n");

        // Append to the source code based on meta event name.
        sourceCode.append(eventNameSwitchBlockMeta);

        // Handle default case if the meta event name isn't recognized.
        sourceCode.append("default:\n" +
                "defaultParselet.accept(lastEventName, valueIn);\n" +
                "return MethodReaderStatus.UNKNOWN;\n" +
                "}\n");
        // Check if we've handled the meta event successfully.
        if (eventNameSwitchBlockMeta.contains("break;"))
            sourceCode.append("return MethodReaderStatus.KNOWN;\n");
        sourceCode.append("} \n" +
                "catch (InvocationTargetRuntimeException e) {\n" +
                "throw e;\n" +
                "}\n" +
                "}\n}\n");

        // Set flag indicating source code has been generated.
        isSourceCodeGenerated = true;

        // Dump the generated source code to console if the flag is set.
        if (DUMP_CODE)
            System.out.println(sourceCode);
    }

    /**
     * This method is used to generate code for handling all method calls of a given interface.
     * It processes the methods recursively in case of chained methods.
     *
     * <p>It first checks if the given interface should be chained using {@code Jvm.dontChain()} method.
     * If not, it immediately returns without executing further. It also checks whether the interface
     * has already been processed. If yes, it immediately returns.
     *
     * <p>Then it proceeds to process all non-static, non-synthetic methods declared in the given
     * interface but not in {@code java.lang.Object}.
     * If a method has already been processed, it's skipped.
     *
     * <p>It also validates that the method isn't one of those defined in {@code java.lang.Object},
     * if it is, it's skipped.
     *
     * <p>If a method name has already been processed before, it throws an {@code IllegalStateException}.
     * This is because MethodReader does not support overloaded methods.
     *
     * <p>Finally, it calls {@code handleMethod()} on the current method if it passed all the above checks.
     *
     * @param anInterface          The interface being processed.
     * @param instanceFieldName    In the generated code, methods are executed on a field with this name.
     * @param methodFilter         Indicates if the passed interface is marked with {@link MethodFilterOnFirstArg}. If true, only certain methods are processed.
     * @ blocks based on method event IDs.
     * @param eventNameSwitchBlock The block of code that handles the switching of event names.
     * @param eventIdSwitchBlock   The block of code that handles the switching of event IDs.
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
     * This method generates code for handling the call of a specific method. It sets up necessary fields and structures,
     * prepares parameters, and constructs a switch block for method calls.
     *
     * <p>Initially, it ensures that the method is accessible and obtains its parameter types and return type.
     * It processes the method parameters and creates fields for storing them. It also checks if the return type of the method
     * is chainable and updates the state accordingly.
     *
     * <p>If a real interceptor is returned by the method, it creates an array field to store the interceptor's arguments
     * and also adds a static field to hold a reference to the method itself.
     *
     * <p>Furthermore, if the method is annotated with {@code MethodId}, it extracts the method ID from the annotation
     * and adds a switch case for this ID to the {@code eventIdSwitchBlock}.
     *
     * <p>Then, it builds a case for the method in the {@code eventNameSwitchBlock}. The structure of this case varies
     * depending on the number of parameters the method has and if it's marked with {@code MethodFilterOnFirstArg}.
     *
     * <p>If the method's return type is {@code DocumentContext}, it also adds code to copy the method's result to the wire
     * and close it.
     *
     * <p>Finally, if the method's return type is chainable, it calls {@code handleInterface()} on it.
     *
     * @param m                  The method for which code is generated.
     * @param anInterface        The interface containing the method.
     * @param instanceFieldName  In the generated code, this method is executed on a field with this name.
     * @param methodFilter       Indicates if the passed interface is marked with {@link MethodFilterOnFirstArg}. If true, only certain methods are processed.
     * @param eventIdSwitchBlock The block of code that handles the switching of event IDs.
     * @param eventNameSwitchBlock The block of code that handles the switching of event names.
     */
    private void handleMethod(Method m, Class<?> anInterface, String instanceFieldName, boolean methodFilter, SourceCodeFormatter eventNameSwitchBlock, SourceCodeFormatter eventIdSwitchBlock) {
        Jvm.setAccessible(m);

        // Retrieving parameter and return type information of the method
        Type[] parameterTypes = getParameterTypes(m, anInterface);

        Class<?> chainReturnType = erase(getReturnType(m, anInterface));

        // Initial setup for chainable return types
        if (chainReturnType != DocumentContext.class && (!chainReturnType.isInterface() || Jvm.dontChain(chainReturnType)))
            chainReturnType = null;

        // Field setup based on method parameters and interceptor
        if (parameterTypes.length > 0 || hasRealInterceptorReturns())
            fields.append(format("// %s\n", m.getName()));

        // Iterating through method parameters to setup fields
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

        // Handling chainable return types and interceptor returns
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

        // Checking and handling @MethodId annotation
        final MethodId methodIdAnnotation = Jvm.findAnnotation(m, MethodId.class);

        if (methodIdAnnotation != null) {
            int methodId = Maths.toInt32(methodIdAnnotation.value());
            addMethodIdSwitch(m.getName(), methodId, eventIdSwitchBlock);
        }

        String chainedCallPrefix = chainReturnType != null ? "chainedCallReturnResult = " : "";

        // Handling code generation for event name switch block
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

        // Handling chainable return types specifically for DocumentContext
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

        // Further handling of chainable return types
        if (chainReturnType != null)
            handleInterface(chainReturnType, "chainedCallReturnResult", false, eventNameSwitchBlock, eventIdSwitchBlock);
    }

    /**
     * Creates a switch block in the provided SourceCodeFormatter for a given method ID.
     * This method facilitates the dynamic selection of methods based on their assigned IDs, allowing
     * efficient routing and method invocation.
     *
     * @param methodName         The name of the method for which the switch case is generated.
     * @param methodId           The ID assigned to the method.
     * @param eventIdSwitchBlock Code block where the generated switch case is appended.
     */
    private void addMethodIdSwitch(String methodName, int methodId, SourceCodeFormatter eventIdSwitchBlock) {
        // Append the switch case based on method ID
        eventIdSwitchBlock.append(format("case %d:\n", methodId));
        // Set the last executed method's name
        eventIdSwitchBlock.append(format("lastEventName = \"%s\";\n", methodName));
        // End of the switch case
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

        // Array to store argument references used for method call
        String[] args = new String[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++)
            args[i] = m.getName() + "arg" + i;

        // Begin try block for method invocation
        res.append("try {\n");
        // Flag indicating a data event has been processed
        res.append("dataEventProcessed = true;\n");

        // called for no interceptor and a generating interceptor
        if (!hasRealInterceptorReturns()) {
            // Potential cast to the generating interceptor type
            GeneratingMethodReaderInterceptorReturns generatingInterceptor = interceptor != null ?
                    (GeneratingMethodReaderInterceptorReturns) interceptor : null;

            // If a generating interceptor is present, append the code before method call
            if (generatingInterceptor != null) {
                final String codeBefore = generatingInterceptor.codeBeforeCall(m, instanceFieldName, args);

                if (codeBefore != null)
                    res.append(codeBefore).append("\n");
            }

            // Generate the method call code
            res.append(format("%s((%s) %s).%s(%s);%n",
                    chainedCallPrefix, m.getDeclaringClass().getCanonicalName(), instanceFieldName, m.getName(),
                    String.join(", ", args)));

            // If a generating interceptor is present, append the code after method call
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

            // Determine if a return type cast is needed for the method
            String castPrefix = chainedCallPrefix.isEmpty() || returnType == null ?
                    "" : "(" + returnType.getCanonicalName() + ")";

            // Generate the code for interceptor invocation
            res.append(format("%s%sinterceptor.intercept(%smethod, %s, " +
                            "interceptor%sArgs, this::actualInvoke);\n",
                    chainedCallPrefix, castPrefix, m.getName(), instanceFieldName, m.getName()));
        }

        // Catch block for handling exceptions during method invocation
        res.append("} \n" +
                "catch (Exception e) {\n" +
                "throw new InvocationTargetRuntimeException(e);\n" +
                "}\n");

        return res.toString();
    }

    /**
     * Generates code for reading an argument of a method from a {@link ValueIn} object.
     * The argument's index and type, and whether it is read in a lambda function,
     * influence the generated code. If {@link LongConversion}
     * annotations are present on the argument, a converter field is registered.
     *
     * @param m Method for which an argument is read.
     * @param argIndex Index of an argument to be read.
     * @param inLambda {@code true} if argument is read in a lambda passed to a
     *                 {@link ValueIn#sequence(Object, BiConsumer)} call.
     * @param parameterTypes The types of the method parameters.
     * @return Code in the form of a String that retrieves the specified argument from {@link ValueIn} input.
     *
     * @see LongConversion
     * @see ValueIn
     */
    private String argumentRead(Method m, int argIndex, boolean inLambda, Type[] parameterTypes) {
        Class<?> numericConversionClass = null;

        // Numeric conversion is not supported for binary wire
        if (wireType == WireType.TEXT || wireType == WireType.YAML) {
            Annotation[] annotations = m.getParameterAnnotations()[argIndex];

            // Loop through annotations to identify and set the numeric conversion class.
            for (Annotation a : annotations) {
                if (a instanceof LongConversion) {
                    numericConversionClass = ((LongConversion) a).value();
                    break;
                } else {
                    // For other annotations, find if they have an associated LongConversion.
                    LongConversion lc = Jvm.findAnnotation(a.annotationType(), LongConversion.class);
                    if (lc != null) {
                        numericConversionClass = lc.value();
                        break;
                    }
                }
            }
        }

        // Determine the type of the argument being read.
        final Class<?> argumentType = erase(parameterTypes[argIndex]);
        String trueArgumentName = m.getName() + "arg" + argIndex;

        // Decide on the naming pattern based on whether the read is happening inside a lambda.
        String argumentName = (inLambda ? "f." : "") + trueArgumentName;

        String valueInName = inLambda ? "v" : "valueIn";

        // Generate code based on the type of the argument.
        if (boolean.class.equals(argumentType)) {
            return format("%s = %s.bool();\n", argumentName, valueInName);
        } else if (byte.class.equals(argumentType)) {
            // If numeric conversion is available and has a shared instance.
            if (numericConversionClass != null && hasInstance(numericConversionClass)) {
                return format("%s = (byte) %s.INSTANCE.parse(%s.text());\n", argumentName, numericConversionClass.getName(), valueInName);
            }
            // If numeric conversion is available and is an instance of LongConverter.
            else if (numericConversionClass != null && LongConverter.class.isAssignableFrom(numericConversionClass)) {
                // Register a converter for this type.
                numericConverters.append(format("private final %s %sConverter = ObjectUtils.newInstance(%s.class);\n",
                        numericConversionClass.getCanonicalName(), trueArgumentName, numericConversionClass.getCanonicalName()));

                return format("%s = (byte) %sConverter.parse(%s.text());\n", argumentName, argumentName, valueInName);
            } else // Default byte reading logic.
                return format("%s = %s.readByte();\n", argumentName, valueInName);
        } else if (char.class.equals(argumentType)) {
            // Handling character type arguments.
            return format("%s = %s.character();\n", argumentName, valueInName);
        } else if (short.class.equals(argumentType)) {
            // Generate code based on the type of the argument and presence of numeric conversion.
            if (numericConversionClass != null && hasInstance(numericConversionClass)) {
                return format("%s = (short) %s.INSTANCE.parse(%s.text());\n", argumentName, numericConversionClass.getName(), valueInName);

            } else if (numericConversionClass != null && LongConverter.class.isAssignableFrom(numericConversionClass)) {
                numericConverters.append(format("private final %s %sConverter = ObjectUtils.newInstance(%s.class);\n",
                        numericConversionClass.getCanonicalName(), trueArgumentName, numericConversionClass.getCanonicalName()));

                return format("%s = (short) %sConverter.parse(%s.text());\n", argumentName, argumentName, valueInName);
            } else
                return format("%s = %s.int16();\n", argumentName, valueInName);
        } else if (int.class.equals(argumentType)) {
            // Generate code based on the type of the argument and presence of numeric conversion.
            if (numericConversionClass != null && hasInstance(numericConversionClass)) {
                return format("%s = (int) %s.INSTANCE.parse(%s.text());\n", argumentName, numericConversionClass.getName(), valueInName);

            } else if (numericConversionClass != null && LongConverter.class.isAssignableFrom(numericConversionClass)) {
                numericConverters.append(format("private final %s %sConverter = ObjectUtils.newInstance(%s.class);\n",
                        numericConversionClass.getCanonicalName(), trueArgumentName, numericConversionClass.getCanonicalName()));

                return format("%s = (int) %sConverter.parse(%s.text());\n", argumentName, argumentName, valueInName);
            } else
                return format("%s = %s.int32();\n", argumentName, valueInName);
        } else if (long.class.equals(argumentType)) {
            // Generate code based on the type of the argument and presence of numeric conversion.
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
            // Handling float type arguments.
            return format("%s = %s.float32();\n", argumentName, valueInName);
        } else if (double.class.equals(argumentType)) {
            // Handling double type arguments.
            return format("%s = %s.float64();\n", argumentName, valueInName);
        } else if (Bytes.class.isAssignableFrom(argumentType)) {
            // Handling Bytes type arguments.
            return format("%s.bytes(%s);\n", valueInName, argumentName);
        } else if (CharSequence.class.isAssignableFrom(argumentType)) {
            // Handling CharSequence type arguments.
            return format("%s = %s.text();\n", argumentName, valueInName);
        } else {
            // Handling other object types.
            final String typeName = argumentType.getCanonicalName();
            return format("%s = %s.object(checkRecycle(%s), %s.class);\n", argumentName, valueInName, argumentName, typeName);
        }
    }

    /**
     * Checks if a real interceptor is present that returns.
     *
     * @return {@code true} if there's a real interceptor present, {@code false} otherwise.
     */
    private boolean hasRealInterceptorReturns() {
        return interceptor != null && !(interceptor instanceof GeneratingMethodReaderInterceptorReturns);
    }

    /**
     * Retrieves the package name for the generated class.
     * The package name is determined based on the class name of the first instance.
     *
     * @return The package name of the generated class.
     */
    public String packageName() {
        Class<?> firstClass = instances[0].getClass();
        String firstClassFullName = firstClass.getName();

        return ReflectionUtil.generatedPackageName(firstClassFullName);
    }

    /**
     * Gets the simple name of the generated class.
     *
     * @return The simple name of the generated class.
     */
    public String generatedClassName() {
        return generatedClassName;
    }

    /**
     * Constructs the generated class name using various components such as
     * the names of instances, metadata handlers, wire type, and potential interceptor.
     * Special characters, such as underscores and slashes, are handled to format the class name.
     *
     * @return The constructed name for the generated class.
     */
    @NotNull
    private String generatedClassName0() {
        final StringBuilder sb = new StringBuilder();

        // Append names of instances to the class name.
        for (Object i : instances)
            appendInstanceName(sb, i);

        // Append names of metadata handlers if available.
        if (metaDataHandler != null)
            for (Object i : metaDataHandler)
                appendInstanceName(sb, i);

        // Append wire type to the class name.
        if (wireType != null)
            sb.append(wireType.toString()
                    .replace("_", ""));

        // Append interceptor details to the class name.
        if (interceptor instanceof GeneratingMethodReaderInterceptorReturns)
            sb.append(((GeneratingMethodReaderInterceptorReturns) interceptor).generatorId());
        else if (hasRealInterceptorReturns())
            sb.append("Intercepting");

        sb.append("MethodReader");

        // Convert slashes for nested or inner classes.
        return sb.toString().replace("/", "$");
    }

    /**
     * Appends the simplified name of the instance's class to the provided StringBuilder.
     * The method takes into account various scenarios including:
     * - If the class is a proxy class.
     * - If the class is enclosed within another class.
     * - If the class is synthetic, anonymous, or local.
     * The method aims to provide a more concise and meaningful name for the instance's class
     * that can be used in contexts like generating a class name.
     *
     * @param sb The StringBuilder to which the instance name should be appended.
     * @param i  The instance for which the class name is determined.
     */
    private void appendInstanceName(StringBuilder sb, Object i) {
        Class<?> aClass = i.getClass();

        // Check if it's a proxy class.
        if (Proxy.isProxyClass(aClass)) {
            aClass = aClass.getInterfaces()[0];
        }

        // Check for enclosing class.
        if (aClass.getEnclosingClass() != null)
            sb.append(aClass.getEnclosingClass().getSimpleName());

        String name = Jvm.isLambdaClass(aClass)
                ? aClass.getInterfaces()[0].getName()
                : aClass.getName();

        final int packageDelimiterIndex = name.lastIndexOf('.');

        // Intentionally using this instead of class.simpleName() in order to support anonymous class
        String nameWithoutPackage = packageDelimiterIndex == -1 ? name : name.substring(packageDelimiterIndex + 1);

        // Further refine the name for specific synthetic class scenarios.
        if (aClass.isSynthetic() && !aClass.isAnonymousClass() && !aClass.isLocalClass()) {
            int lambdaSlashIndex = nameWithoutPackage.lastIndexOf("/");

            if (lambdaSlashIndex != -1)
                nameWithoutPackage = nameWithoutPackage.substring(0, lambdaSlashIndex);
        }

        // Append the refined name to the StringBuilder.
        sb.append(nameWithoutPackage);
    }
}
