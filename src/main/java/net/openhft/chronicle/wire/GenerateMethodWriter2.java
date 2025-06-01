/*
 * Copyright 2016-2025 chronicle.software
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
 * Lightweight generator for method writer proxies.
 * <p>
 * This implementation extends {@link AbstractClassGenerator} and emits a
 * minimal proxy that serialises method calls to a {@link MarshallableOut}
 * sink.  It is kept deliberately simple compared with
 * {@link GenerateMethodWriter} and is suited to binary wire use.
 */
@SuppressWarnings("this-escape")
public class GenerateMethodWriter2 extends AbstractClassGenerator<GenerateMethodWriter2.GMWMetaData> {

    // The simple name of the DocumentContext class used for template methods.
    private static final String DOCUMENT_CONTEXT = DocumentContext.class.getSimpleName();

    /**
     * Internal snippets for well-known interface methods.  The first key is the
     * method name and the second is the parameter list used to locate the code
     * body.
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

    /**
     * Cache for chained method writers.  Each entry maps an interface to the
     * generated {@code ThreadLocal} field name.
     */
    private final Map<Class<?>, String> methodWritersMap = new LinkedHashMap<>();

    /**
     * Creates a new generator with default {@link GMWMetaData} and registers
     * the imports required by the generated code.
     */
    public GenerateMethodWriter2() {
        super(new GMWMetaData());
        // add to imports
        nameForClass(DocumentContext.class);
        importSet.add("net.openhft.chronicle.bytes.*");
        importSet.add("net.openhft.chronicle.wire.*");
    }

    /**
     * Returns the canned code for the supplied signature or {@code null} when
     * no template exists.
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
     * Maps primitive and common types to the {@code ValueOut} method name used
     * when writing arguments.
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
    /**
     * Adds fields used by the generated proxy such as the {@code Closeable},
     * optional {@link UpdateInterceptor} and the supplier of
     * {@link MarshallableOut} instances.
     */
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
    /**
     * Emits a constructor that initialises the output supplier, the optional
     * interceptor and the associated {@link Closeable}.
     */
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
    /**
     * Generates the body of a proxied method.  It opens a
     * {@link WriteDocumentContext}, writes the event name or ID and serialises
     * the arguments.  The return type dictates whether the call is terminating
     * or part of a fluent API.
     */
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
    /**
     * Declares any {@code ThreadLocal} fields required for chained writers at
     * the end of code generation.
     */
    protected void generateEnd(SourceCodeFormatter mainCode) {
        super.generateEnd(mainCode);
        for (Map.Entry<Class<?>, String> e : methodWritersMap.entrySet()) {
            mainCode.append("private transient ThreadLocal<").append(nameForClass(e.getKey())).append("> ").append(e.getValue())
                    .append("= ThreadLocal.withInitial(() -> this.outSupplier.get().methodWriter(").append(nameForClass(e.getKey())).append(".class));\n");
        }
    }

    /**
     * Emits a call to write the event identifier.  When
     * {@link MethodId} is present and enabled the numeric id is used,
     * otherwise the textual name is written.
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
     * Serialises the method arguments.  Multiple parameters are wrapped in an
     * array block.
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
     * Writes a single parameter using {@code ValueOut.object}.
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
     * Emits the appropriate return statement for the generated method.
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
     * Meta-data controlling the generated writer behaviour.
     */
    public static class GMWMetaData extends AbstractClassGenerator.MetaData<GMWMetaData> {
        private boolean metaData;
        private boolean useMethodIds;
        private String genericEvent;

        /**
         * Whether each method call should include meta-data.
         */
        public boolean metaData() {
            return metaData;
        }

        /**
         * Enables or disables meta-data writing.
         */
        public GMWMetaData metaData(boolean metaData) {
            this.metaData = metaData;
            return this;
        }

        /**
         * Returns true when numeric {@link MethodId} values should be written.
         */
        public boolean useMethodIds() {
            return useMethodIds;
        }

        /**
         * Enables numeric {@link MethodId} handling.
         */
        public GMWMetaData useMethodIds(boolean useMethodIds) {
            this.useMethodIds = useMethodIds;
            return this;
        }

        /**
         * Name of the catch-all method for generic events.
         */
        public String genericEvent() {
            return genericEvent;
        }

        /**
         * Sets the method name to be treated as a generic event.
         */
        public GMWMetaData genericEvent(String genericEvent) {
            this.genericEvent = genericEvent;
            return this;
        }
    }
}
