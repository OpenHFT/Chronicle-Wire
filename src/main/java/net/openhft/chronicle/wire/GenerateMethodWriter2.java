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

@SuppressWarnings("StringBufferReplaceableByString")
public class GenerateMethodWriter2 extends AbstractClassGenerator<GenerateMethodWriter2.GMWMetaData> {

    private static final String DOCUMENT_CONTEXT = DocumentContext.class.getSimpleName();
    private static final Map<String, Map<List<Class<?>>, String>> TEMPLATE_METHODS = new LinkedHashMap<>();

    static {
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
                        "public " + DOCUMENT_CONTEXT + " acquireWritingDocument(boolean metaData){\n" +
                        "    return this.outSupplier.get().acquireWritingDocument(metaData);\n" +
                        "}\n"));
        Map<List<Class<?>>, String> wd = new LinkedHashMap<>();
        wd.put(singletonList(DocumentContext.class), "" +
                "public " + DOCUMENT_CONTEXT + " writingDocument(){\n" +
                "    return this.outSupplier.get().writingDocument();\n" +
                "}\n");
        wd.put(dcBoolean, "" +
                "public " + DOCUMENT_CONTEXT + " writingDocument(boolean metaData){\n" +
                "return this.outSupplier.get().writingDocument(metaData);\n" +
                "}\n");
        TEMPLATE_METHODS.put("writingDocument", wd);
    }

    private final Map<Class<?>, String> methodWritersMap = new LinkedHashMap<>();

    public GenerateMethodWriter2() {
        super(new GMWMetaData());
        // add to imports
        nameForClass(DocumentContext.class);
        importSet.add("net.openhft.chronicle.bytes.*");
        importSet.add("net.openhft.chronicle.wire.*");
    }

    private static String templateFor(String name, Class<?> returnType, Class<?>[] pts) {
        Map<List<Class<?>>, String> map = TEMPLATE_METHODS.get(name);
        if (map == null)
            return null;
        List<Class<?>> sig = new ArrayList<>();
        sig.add(returnType);
        addAll(sig, pts);
        return map.get(sig);
    }

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
                .append("MarshallableOut out = this.outSupplier.get();\n");
        if (!passthrough)
            withLineNumber(mainCode)
                    .append("try (");
        mainCode.append("final ").append(wdc).append(" dc = (").append(wdc).append(") out.acquireWritingDocument(")
                .append(metaData().metaData())
                .append(")");
        if (passthrough)
            mainCode.append(";\n");
        else mainCode.append(") {\n");
        mainCode.append("dc.chainedElement(" + (!terminating && !passthrough) + ");\n");
        mainCode.append("if (out.recordHistory()) MessageHistory.writeHistory(dc);\n");

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

    private void writeEventNameOrId(final Method method, final SourceCodeFormatter body, final String eventName) {
        final Optional<Annotation> methodId = metaData().useMethodIds()
                ? stream(method.getAnnotations()).filter(MethodId.class::isInstance).findFirst()
                : Optional.empty();
        if (methodId.isPresent()) {
            long value = ((MethodId) methodId.get()).value();
            withLineNumber(body)
                    .append("dc.wire().writeEventId(").append(eventName).append(", ").append(String.valueOf(value)).append(")");

        } else {
            withLineNumber(body)
                    .append("dc.wire().write(").append(eventName).append(")");
        }
    }

    private void writeArrayOfParameters(final Method dm, final SourceCodeFormatter body, final int startJ) {
        boolean multipleParams = dm.getParameterTypes().length > startJ + 1;
        if (multipleParams)
            body.append(".array(v -> {\n");
        Parameter[] parameters = dm.getParameters();
        for (int j = startJ; j < parameters.length; j++) {
            final Parameter p = parameters[j];
            if (p.getType().isPrimitive() || CharSequence.class.isAssignableFrom(p.getType())) {
                body.append(multipleParams ? "v." : ".").append(asString(p.getType())).append("(").append(p.getName()).append(");\n");
            } else
                writeValue(dm, body, startJ, p);
        }

        if (multipleParams)
            body.append("}, Object[].class);\n");
    }

    private void writeValue(final Method dm, final SourceCodeFormatter body, final int startJ, final Parameter p) {
        String className = p.getType().getTypeName().replace('$', '.');

        body
                .append(dm.getParameterTypes().length > startJ + 1 ? "v." : ".")
                .append("object(")
                .append(className)
                .append(".class, ")
                .append(p.getName())
                .append(");\n");
    }

    private void methodReturn(SourceCodeFormatter result, final Method method, final Set<Class> interfaceClases) {
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class)
            return;

        if (interfaceClases.stream().anyMatch(i -> returnType == i || returnType.isAssignableFrom(i))) {
            withLineNumber(result).append("return this;\n");

        } else if (returnType.isInterface()) {
            methodWritersMap.computeIfAbsent(returnType, k -> "methodWriter" + k.getSimpleName() + "TL");
            withLineNumber(result).append("return methodWriter").append(returnType.getSimpleName()).append("TL.get();\n");
        } else if (!returnType.isPrimitive()) {
            withLineNumber(result).append("return null;\n");
        } else if (returnType == boolean.class) {
            withLineNumber(result).append("return false;\n");
        } else if (returnType == byte.class) {
            withLineNumber(result).append("return (byte)0;\n");
        } else {
            withLineNumber(result).append("return 0;\n");
        }
    }

    public static class GMWMetaData extends AbstractClassGenerator.MetaData<GMWMetaData> {
        private boolean metaData;
        private boolean useMethodIds;
        private String genericEvent;

        public boolean metaData() {
            return metaData;
        }

        public GMWMetaData metaData(boolean metaData) {
            this.metaData = metaData;
            return this;
        }

        public boolean useMethodIds() {
            return useMethodIds;
        }

        public GMWMetaData useMethodIds(boolean useMethodIds) {
            this.useMethodIds = useMethodIds;
            return this;
        }

        public String genericEvent() {
            return genericEvent;
        }

        public GMWMetaData genericEvent(String genericEvent) {
            this.genericEvent = genericEvent;
            return this;
        }
    }
}
