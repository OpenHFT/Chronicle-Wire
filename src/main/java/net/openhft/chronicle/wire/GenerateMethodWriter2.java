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
import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Collections.*;

/**
 * The {@code GenerateMethodWriter2} class is responsible for generating method writers based on the provided metadata.
 * <p>
 * It extends the {@link AbstractClassGenerator} with metadata type {@link GMWMetaData}. This class internally maintains
 * a set of template methods that help in the method writer generation. These templates are based on certain method names
 * and parameter types, which define their structure.
 */
@SuppressWarnings("this-escape")
@Deprecated(/* to be moved in x.27 */)
public class GenerateMethodWriter2 extends AbstractClassGenerator<GenerateMethodWriter2.GMWMetaData> {

    // The simple name of the DocumentContext class used for template methods.
    private static final String DOCUMENT_CONTEXT = DocumentContext.class.getSimpleName();

    /**
     * Holds the predefined templates for methods. The outer map's key is the method name, while the inner map
     * has the method parameter types as the key and the method body as the value.
     */
    private static final Map<String, Map<List<Class<?>>, String>> TEMPLATE_METHODS = new LinkedHashMap<>();

    static {
        // Initialize the TEMPLATE_METHODS with predefined method structures.
        TEMPLATE_METHODS.put("close",
                singletonMap(singletonList(void.class), "" +
                        "public void close() {\n" +
                        "   if (this.closeable != null) {\n" +
                        "        this.closeable.close();\n" +
                        "   }\n" +
                        "}\n"));
        TEMPLATE_METHODS.put("recordHistory",
                singletonMap(singletonList(boolean.class), "" +
                        "public boolean recordHistory() {\n" +
                        "    return this.outSupplier.get().recordHistory();\n" +
                        "}\n"));
        List<Class<?>> dcBoolean = Stream.of(DocumentContext.class, boolean.class).collect(Collectors.toList());
        TEMPLATE_METHODS.put("acquireWritingDocument",
                singletonMap(dcBoolean, "" +
                        "public " + DOCUMENT_CONTEXT + " acquireWritingDocument(boolean metaData) {\n" +
                        "    return this.outSupplier.get().acquireWritingDocument(metaData);\n" +
                        "}\n"));
        Map<List<Class<?>>, String> wd = new LinkedHashMap<>();
        wd.put(singletonList(DocumentContext.class), "" +
                "public " + DOCUMENT_CONTEXT + " writingDocument() {\n" +
                "    return this.outSupplier.get().writingDocument();\n" +
                "}\n");
        wd.put(dcBoolean, "" +
                "public " + DOCUMENT_CONTEXT + " writingDocument(boolean metaData) {\n" +
                "    return this.outSupplier.get().writingDocument(metaData);\n" +
                "}\n");
        TEMPLATE_METHODS.put("writingDocument", wd);
    }

    // Maintains a mapping of method writers based on their respective classes.
    private final Map<Class<?>, String> methodWritersMap = new LinkedHashMap<>();

    /**
     * Default constructor that initializes the metadata and sets up necessary imports.
     */
    public GenerateMethodWriter2() {
        super(new GMWMetaData());
        // add to imports
        nameForClass(DocumentContext.class);
        importSet.add("net.openhft.chronicle.bytes.*");
        importSet.add("net.openhft.chronicle.wire.*");
    }

    /**
     * Retrieves the method template for a given method name, return type, and parameter types.
     * <p>
     * The method looks up the template from the predefined {@code TEMPLATE_METHODS}. If no matching template is
     * found, it returns {@code null}.
     *
     * @param name The method name to look up
     * @param returnType The return type of the method
     * @param pts The parameter types of the method
     * @return The method template as a string, or {@code null} if not found
     */
    private static String templateFor(String name, Class<?> returnType, Class<?>[] pts) {
        Map<List<Class<?>>, String> map = TEMPLATE_METHODS.get(name);
        if (map == null)
            return null;
        List<Class<?>> sig = new ArrayList<>();
        sig.add(returnType);
        addAll(sig, pts);
        return map.get(sig);
    }

    /**
     * Converts a given {@link Class} type to a corresponding string representation.
     * <p>
     * This method provides string representations for various primitive types, {@link CharSequence}, and
     * {@link Marshallable}. If the type does not match any predefined types, it defaults to returning "object".
     *
     * @param type The class type to convert
     * @return The corresponding string representation of the type
     */
    private static CharSequence asString(Class<?> type) {
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

    @Override
    protected void generateFields(SourceCodeFormatter mainCode) {
        super.generateFields(mainCode);
        withLineNumber(mainCode)
                .append("private transient final Closeable closeable;\n");
        if (metaData().useUpdateInterceptor())
            mainCode.append("private transient final ").append(nameForClass(UpdateInterceptor.class)).append(" updateInterceptor;\n");

        mainCode.append("private transient ")
                .append(nameForClass(Supplier.class)).append("<").append(nameForClass(MarshallableOut.class)).append("> outSupplier;\n");
    }

    @Override
    protected void generateConstructors(SourceCodeFormatter mainCode) {
        super.generateConstructors(mainCode);
        withLineNumber(mainCode)
                .append("public ").append(className()).append("(")
                .append(nameForClass(Supplier.class)).append("<").append(nameForClass(MarshallableOut.class)).append("> outSupplier, ")
                .append(nameForClass(Closeable.class)).append(" closeable, ")
                .append(nameForClass(UpdateInterceptor.class)).append(" updateInterceptor) {\n" +
                        "this.outSupplier = outSupplier;\n" +
                        "this.closeable = closeable;\n");
        if (metaData().useUpdateInterceptor())
            mainCode.append("this.updateInterceptor = updateInterceptor;");
        mainCode.append("}\n");
    }

    @Override
    protected void generateMethod(Method method, StringBuilder params, List<String> paramList, SourceCodeFormatter mainCode) {
        String name = method.getName();
        Class<?> returnType = method.getReturnType();
        Class<?>[] parameterTypes = method.getParameterTypes();
        String template = templateFor(name, returnType, parameterTypes);
        if (template != null) {
            mainCode.append(template);
            return;
        }

        boolean terminating = returnType == Void.class || returnType == void.class || returnType.isPrimitive();
        String wdc = nameForClass(WriteDocumentContext.class);
        boolean passthrough = returnType == DocumentContext.class;
        withLineNumber(mainCode)
                .append("MarshallableOut _out_ = this.outSupplier.get();\n");
        if (!passthrough)
            withLineNumber(mainCode)
                    .append("try (");
        mainCode.append("final ").append(wdc).append(" _dc_ = (").append(wdc).append(") _out_.acquireWritingDocument(")
                .append(metaData().metaData())
                .append(")");
        if (passthrough)
            mainCode.append(";\n");
        else mainCode.append(") {\n");
        mainCode.append("_dc_.chainedElement(" + (!terminating && !passthrough) + ");\n");
        mainCode.append("if (_out_.recordHistory()) MessageHistory.writeHistory(_dc_);\n");

        int startJ = 0;

        final String eventName;
        if (parameterTypes.length > 0 && name.equals(metaData().genericEvent)) {
            // this is used when we are processing the genericEvent
            eventName = parameterTypes[0].getName();
            startJ = 1;
        } else {
            eventName = '\"' + name + '\"';
        }

        writeEventNameOrId(method, mainCode, eventName);

        if (parameterTypes.length == 0)
            mainCode.append(".text(\"\");\n");
        else
            writeArrayOfParameters(method, mainCode, startJ);
        mainCode.append("}\n");
        methodReturn(mainCode, method, metaData().interfaces());
    }

    @Override
    protected void generateEnd(SourceCodeFormatter mainCode) {
        super.generateEnd(mainCode);
        for (Map.Entry<Class<?>, String> e : methodWritersMap.entrySet()) {
            mainCode.append("private transient ThreadLocal<").append(nameForClass(e.getKey())).append("> ").append(e.getValue())
                    .append("= ThreadLocal.withInitial(() -> this.outSupplier.get().methodWriter(").append(nameForClass(e.getKey())).append(".class));\n");
        }
    }

    /**
     * Writes the event name or ID associated with the provided method.
     * If a {@code MethodId} annotation is present, the method writes the event using its value.
     * Otherwise, it simply writes the event name.
     *
     * @param method    The method whose event information needs to be written.
     * @param body      The code formatter to which the write operation should be appended.
     * @param eventName The name of the event.
     */
    private void writeEventNameOrId(final Method method, final SourceCodeFormatter body, final String eventName) {
        // Check if using method IDs is required and find any @MethodId annotation present
        final Optional<Annotation> methodId = metaData().useMethodIds()
                ? stream(method.getAnnotations()).filter(MethodId.class::isInstance).findFirst()
                : Optional.empty();

        // If a MethodId annotation is present, write the event with its value
        if (methodId.isPresent()) {
            long value = ((MethodId) methodId.get()).value();
            withLineNumber(body)
                    .append("_dc_.wire().writeEventId(").append(eventName).append(", ").append(String.valueOf(value)).append(")");

        } else {
            // Otherwise, simply write the event name
            withLineNumber(body)
                    .append("_dc_.wire().write(").append(eventName).append(")");
        }
    }

    /**
     * Writes the array of parameters for the provided method.
     * Handles primitive types and CharSequences directly, while delegating non-primitive types to {@code writeValue}.
     *
     * @param dm     The method whose parameters are to be written.
     * @param body   The code formatter to which the write operations should be appended.
     * @param startJ The starting index for parameters.
     */
    private void writeArrayOfParameters(final Method dm, final SourceCodeFormatter body, final int startJ) {
        // Check if there are multiple parameters
        boolean multipleParams = dm.getParameterTypes().length > startJ + 1;
        if (multipleParams)
            body.append(".array(v -> {\n");

        // Iterate over the method's parameters and write them
        Parameter[] parameters = dm.getParameters();
        for (int j = startJ; j < parameters.length; j++) {
            final Parameter p = parameters[j];
            // For primitive types and CharSequences, write directly
            if (p.getType().isPrimitive() || CharSequence.class.isAssignableFrom(p.getType())) {
                body.append(multipleParams ? "v." : ".").append(asString(p.getType())).append("(").append(p.getName()).append(");\n");
            } else
                // For non-primitive types, delegate to writeValue
                writeValue(dm, body, startJ, p);
        }

        // Close array writing if there were multiple parameters
        if (multipleParams)
            body.append("}, Object[].class);\n");
    }

    /**
     * Writes the value of a given parameter.
     * The parameter is written as an object with its associated class name and name.
     *
     * @param dm     The method to which the parameter belongs.
     * @param body   The code formatter to which the write operation should be appended.
     * @param startJ The starting index for parameters.
     * @param p      The parameter whose value needs to be written.
     */
    private void writeValue(final Method dm, final SourceCodeFormatter body, final int startJ, final Parameter p) {
        // Retrieve class name, replacing inner class '$' with '.'
        String className = p.getType().getTypeName().replace('$', '.');

        // Write the parameter value as an object
        body
                .append(dm.getParameterTypes().length > startJ + 1 ? "v." : ".")
                .append("object(")
                .append(className)
                .append(".class, ")
                .append(p.getName())
                .append(");\n");
    }

    /**
     * Handles the return value for a provided method.
     * Determines the return value based on the method's return type and available interfaces.
     * The method ensures that the correct return type or default value is appended to the result.
     *
     * @param result         The code formatter to which the return value should be appended.
     * @param method         The method whose return value needs to be determined.
     * @param interfaceClases Set of interfaces to determine if the return type matches any.
     */
    private void methodReturn(SourceCodeFormatter result, final Method method, final Set<Class<?>> interfaceClases) {
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class)
            return;

        // Check if returnType matches any of the interfaces provided
        if (interfaceClases.stream().anyMatch(i -> returnType == i || returnType.isAssignableFrom(i))) {
            withLineNumber(result).append("return this;\n");

        } else if (returnType.isInterface()) {
            // If the returnType is an interface, compute the corresponding method writer if absent
            methodWritersMap.computeIfAbsent(returnType, k -> "methodWriter" + k.getSimpleName() + "TL");
            withLineNumber(result).append("return methodWriter").append(returnType.getSimpleName()).append("TL.get();\n");
        } else if (!returnType.isPrimitive()) {
            // For non-primitive non-interface return types, return null
            withLineNumber(result).append("return null;\n");
        } else if (returnType == boolean.class) {
            // For primitive boolean type, return false
            withLineNumber(result).append("return false;\n");
        } else if (returnType == byte.class) {
            // For primitive byte type, return 0
            withLineNumber(result).append("return (byte)0;\n");
        } else {
            // For other primitive types, return 0
            withLineNumber(result).append("return 0;\n");
        }
    }

    /**
     * Metadata associated with the {@code GenerateMethodWriter2} class.
     * Provides options like use of method IDs, metadata, and generic events.
     */
    public static class GMWMetaData extends AbstractClassGenerator.MetaData<GMWMetaData> {
        private boolean metaData;
        private boolean useMethodIds;
        private String genericEvent;

        /**
         * Gets the meta data usage status.
         *
         * @return {@code true} if metadata is to be used; {@code false} otherwise.
         */
        public boolean metaData() {
            return metaData;
        }

        /**
         * Sets the meta data usage status.
         *
         * @param metaData Whether to use metadata.
         * @return The updated metadata instance.
         */
        public GMWMetaData metaData(boolean metaData) {
            this.metaData = metaData;
            return this;
        }

        /**
         * Gets the method IDs usage status.
         *
         * @return {@code true} if method IDs are to be used; {@code false} otherwise.
         */
        public boolean useMethodIds() {
            return useMethodIds;
        }

        /**
         * Sets the method IDs usage status.
         *
         * @param useMethodIds Whether to use method IDs.
         * @return The updated metadata instance.
         */
        public GMWMetaData useMethodIds(boolean useMethodIds) {
            this.useMethodIds = useMethodIds;
            return this;
        }

        /**
         * Gets the generic event.
         *
         * @return The generic event string.
         */
        public String genericEvent() {
            return genericEvent;
        }

        /**
         * Sets the generic event.
         *
         * @param genericEvent The generic event string.
         * @return The updated metadata instance.
         */
        public GMWMetaData genericEvent(String genericEvent) {
            this.genericEvent = genericEvent;
            return this;
        }
    }
}
