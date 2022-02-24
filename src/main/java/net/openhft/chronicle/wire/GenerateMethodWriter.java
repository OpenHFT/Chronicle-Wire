package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.wire.internal.GenericReflection;
import net.openhft.chronicle.wire.utils.JavaSourceCodeFormatter;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
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
import static net.openhft.compiler.CompilerUtils.CACHED_COMPILER;

@SuppressWarnings("StringBufferReplaceableByString")
public class GenerateMethodWriter {

    public static final String UPDATE_INTERCEPTOR = UpdateInterceptor.class.getSimpleName();
    static final boolean DUMP_CODE = Jvm.getBoolean("dumpCode");
    private static final String DOCUMENT_CONTEXT = DocumentContext.class.getSimpleName();
    private static final String WRITE_DOCUMENT_CONTEXT = WriteDocumentContext.class.getSimpleName();
    private static final String MARSHALLABLE_OUT = MarshallableOut.class.getSimpleName();
    private static final String METHOD_ID = MethodId.class.getSimpleName();
    private static final String VALUE_OUT = ValueOut.class.getSimpleName();
    private static final String CLOSEABLE = Closeable.class.getSimpleName();
    private static final String UPDATE_INTERCEPTOR_FIELD = "updateInterceptor";
    private static final Map<String, Map<List<Class<?>>, String>> TEMPLATE_METHODS = new LinkedHashMap<>();

    static {
        // make sure Wires static block called and classpath set up
        Wires.init();

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
                        "    return out.get().recordHistory();\n" +
                        "}\n"));
        List<Class<?>> dcBoolean = Stream.of(DocumentContext.class, boolean.class).collect(Collectors.toList());
        TEMPLATE_METHODS.put("acquireWritingDocument",
                singletonMap(dcBoolean, "" +
                        "public " + DOCUMENT_CONTEXT + " acquireWritingDocument(boolean metaData){\n" +
                        "    return out.get().acquireWritingDocument(metaData);\n" +
                        "}\n"));
        Map<List<Class<?>>, String> wd = new LinkedHashMap<>();
        wd.put(singletonList(DocumentContext.class), "" +
                "public " + DOCUMENT_CONTEXT + " writingDocument(){\n" +
                "    return out.get().writingDocument();\n" +
                "}\n");
        wd.put(dcBoolean, "" +
                "public " + DOCUMENT_CONTEXT + " writingDocument(boolean metaData){\n" +
                "return out.get().writingDocument(metaData);\n" +
                "}\n");
        TEMPLATE_METHODS.put("writingDocument", wd);
    }

    private final boolean metaData;
    private final boolean useMethodId;

    private final String packageName;
    private final Set<Class> interfaces;
    private final String className;
    private final ClassLoader classLoader;
    private final WireType wireType;
    private final String genericEvent;
    private final boolean useUpdateInterceptor;
    private final ConcurrentMap<Class<?>, String> methodWritersMap = new ConcurrentHashMap<>();
    final private AtomicInteger indent = new AtomicInteger();

    private GenerateMethodWriter(final String packageName,
                                 final Set<Class> interfaces,
                                 final String className,
                                 final ClassLoader classLoader,
                                 final WireType wireType,
                                 final String genericEvent,
                                 final boolean metaData,
                                 final boolean useMethodId,
                                 final boolean useUpdateInterceptor) {

        this.packageName = packageName;
        this.interfaces = interfaces;
        this.className = className;
        this.classLoader = classLoader;
        this.wireType = wireType;
        this.genericEvent = genericEvent;
        this.metaData = metaData;
        this.useMethodId = useMethodId;
        this.useUpdateInterceptor = useUpdateInterceptor;
    }

    /**
     * @param interfaces an interface class
     * @return a proxy class from an interface class or null if it can't be created
     */
    @Nullable
    public static Class<?> newClass(String fullClassName,
                                    Set<Class> interfaces,
                                    ClassLoader classLoader,
                                    final WireType wireType,
                                    final String genericEvent,
                                    boolean metaData,
                                    boolean useMethodId,
                                    final boolean useUpdateInterceptor) {
        int lastDot = fullClassName.lastIndexOf('.');
        String packageName = "";
        String className = fullClassName;

        if (lastDot != -1) {
            packageName = fullClassName.substring(0, lastDot);
            className = fullClassName.substring(lastDot + 1);
        }

        return new GenerateMethodWriter(packageName,
                interfaces,
                className,
                classLoader,
                wireType,
                genericEvent,
                metaData, useMethodId, useUpdateInterceptor)
                .createClass();
    }

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

    private static CharSequence toString(Class type) {
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

    @NotNull
    private static String nameForClass(Class type) {
        return type.getName().replace('$', '.');
    }

    @NotNull
    private static String nameForClass(Set<String> importSet, Class type) {
        if (type.isArray())
            return nameForClass(importSet, type.getComponentType()) + "[]";
        String s = nameForClass(type);
        Package aPackage = type.getPackage();
        if (aPackage != null)
            if (importSet.contains(s)
                    || "java.lang".equals(aPackage.getName())
                    || (!type.getName().contains("$")
                    && importSet.contains(aPackage.getName() + ".*")))
                return type.getSimpleName();
        return s;
    }

    private static String signature(Method m) {
        return m.getReturnType() + " " + m.getName() + " " + Arrays.toString(m.getParameterTypes());
    }

    @NotNull
    private Appendable methodSignature(SortedSet<String> importSet, final Method dm) {

        SourceCodeFormatter result = new JavaSourceCodeFormatter(this.indent);
        String sep = "";
        for (Parameter p : dm.getParameters()) {
            result.append(sep);
            sep = ", ";
            IntConversion intConversion = p.getAnnotation(IntConversion.class);
            LongConversion longConversion = p.getAnnotation(LongConversion.class);

            if (intConversion != null)
                result.append("@IntConversion(")
                        .append(nameForClass(importSet, intConversion.value()))
                        .append(".class) ");
            else if (longConversion != null)
                result.append("@LongConversion(")
                        .append(nameForClass(importSet, longConversion.value()))
                        .append(".class) ");
            result.append("final ")
                    .append(nameForClass(importSet, p.getType()))
                    .append(' ')
                    .append(p.getName());
        }
        return result;
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    private Class<?> createClass() {

        SourceCodeFormatter interfaceMethods = new SourceCodeFormatter(1);
        SourceCodeFormatter imports = new JavaSourceCodeFormatter();

        try {
            imports.append("package " + packageName + ";\n\n");
            SortedSet<String> importSet = new TreeSet<>();
            importSet.add(IntConversion.class.getName());
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
            for (Class interfaceClazz : interfaces) {
                importSet.add(nameForClass(interfaceClazz));

                if (!interfaceClazz.isInterface())
                    throw new MethodWriterValidationException("expecting an interface instead of class=" + interfaceClazz.getName());

                // TODO: everything in this loop can be commented out and all tests pass
                for (Method dm : interfaceClazz.getMethods()) {
                    if (dm.isDefault() || Modifier.isStatic(dm.getModifiers()))
                        continue;
                    String template = templateFor(dm, interfaceClazz);
                    if (template != null)
                        continue;
                    for (Class pType : dm.getParameterTypes()) {
                        if (pType.isPrimitive() || pType.isArray() || pType.getPackage().getName().equals("java.lang"))
                            continue;
                        importSet.add(nameForClass(pType));
                    }
                }
            }
            importSet.removeIf(s -> s.startsWith("net.openhft.chronicle.bytes"));
            importSet.add("net.openhft.chronicle.bytes.*");
            importSet.removeIf(s -> s.startsWith("net.openhft.chronicle.wire"));
            importSet.add("net.openhft.chronicle.wire.*");

            for (String s : importSet) {
                imports.append("import ").append(s).append(";\n");
            }

            imports.append("\npublic final class ")
                    .append(className)
                    .append(" implements ");

            Set<String> handledMethodSignatures = new HashSet<>();
            Set<String> methodIds = new HashSet<>();

            for (Class interfaceClazz : interfaces) {

                String interfaceName = nameForClass(importSet, interfaceClazz);
                imports.append(interfaceName);
                imports.append(", ");

                if (!interfaceClazz.isInterface())
                    throw new MethodWriterValidationException("expecting an interface instead of class=" + interfaceClazz.getName());

                for (Method dm : interfaceClazz.getMethods()) {
                    if (Modifier.isStatic(dm.getModifiers()))
                        continue;

                    final Class<?> returnType = (Class<?>) GenericReflection.getReturnType(dm, interfaceClazz);
                    if (dm.isDefault() && (!returnType.equals(void.class) && !returnType.isInterface()))
                        continue;

                    if (!handledMethodSignatures.add(signature(dm)))
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

            if (DUMP_CODE)
                System.out.println(imports);

            return CACHED_COMPILER.loadFromJava(classLoader, packageName + '.' + className, imports.toString());

        } catch (AssertionError e) {
            if (e.getCause() instanceof LinkageError) {
                try {
                    return Class.forName(packageName + '.' + className, true, classLoader);
                } catch (ClassNotFoundException x) {
                    throw Jvm.rethrow(x);
                }
            }
            throw Jvm.rethrow(e);
        } catch (MethodWriterValidationException e) {
            throw e;
        } catch (Throwable e) {
            throw Jvm.rethrow(new ClassNotFoundException(e.getMessage() + '\n' + imports, e));
        }

    }

    private String templateFor(Method dm, Class interfaceType) {
        Map<List<Class<?>>, String> map = TEMPLATE_METHODS.get(dm.getName());
        if (map == null)
            return null;
        List<Class> sig = new ArrayList<>();
        sig.add((Class) GenericReflection.getReturnType(dm, interfaceType));
        addAll(sig, dm.getParameterTypes());
        return map.get(sig);
    }

    private void addMarshallableOut(SourceCodeFormatter codeFormatter) {
        codeFormatter.append("@Override\n");
        codeFormatter.append("public void marshallableOut(MarshallableOut out) {\n");
        codeFormatter.append("this.out = () -> out;");
        for (Map.Entry<Class<?>, String> e : methodWritersMap.entrySet()) {
            codeFormatter.append(format("\n    this.%s.remove();", e.getValue()));
        }
        codeFormatter.append("\n}\n");

    }

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
        }

        result.append("\n}\n\n");
        return result;
    }

    private CharSequence createMethod(SortedSet<String> importSet, final Method dm, final Class<?> interfaceClazz, Set<String> methodIds) {

        if (Modifier.isStatic(dm.getModifiers()))
            return "";

        if (dm.getParameterTypes().length == 0 && dm.isDefault())
            return "";

        int parameterCount = dm.getParameterCount();
        Parameter[] parameters = dm.getParameters();
        final int len = parameters.length;
        Class<?> returnType = (Class<?>) GenericReflection.getReturnType(dm, interfaceClazz);
        final String typeName = nameForClass(importSet, returnType);

        final StringBuilder body = new StringBuilder();
        String methodIDAnotation = "";
        if (useUpdateInterceptor) {
            if (parameterCount > 1)
                Jvm.debug().on(getClass(), "Generated code to call updateInterceptor for " + dm + " only using last argument");
            final String name;
            if (parameterCount > 0) {
                Class<?> type = parameters[parameterCount - 1].getType();
                if (type.isPrimitive())
                    Jvm.warn().on(getClass(), "Generated code to call updateInterceptor for " + dm + " will box and generate garbage");
                name = parameters[parameterCount - 1].getName();
            } else
                name = "null";
            body.append("// updateInterceptor\n"
                    + "if (! this." + UPDATE_INTERCEPTOR_FIELD +
                    ".update(\"" + dm.getName() + "\", " + name + ")) return" + returnDefault(returnType) + ";\n");
        }

        boolean terminating = returnType == Void.class || returnType == void.class || returnType.isPrimitive();
        boolean passthrough = returnType == DocumentContext.class;
        if (!passthrough)
            body.append("try (");
        body.append("final ")
                .append(WRITE_DOCUMENT_CONTEXT)
                .append(" dc = (")
                .append(WRITE_DOCUMENT_CONTEXT).append(") this.out.get().acquireWritingDocument(")
                .append(metaData)
                .append(")");
        if (passthrough)
            body.append(";\n");
        else
            body.append(") {\n");
        body.append("try {\n");
        body.append("dc.chainedElement(" + (!terminating && !passthrough) + ");\n");
        body.append("if (out.get().recordHistory()) MessageHistory.writeHistory(dc);\n");

        int startJ = 0;

        final String eventName;
        if (parameterCount > 0 && dm.getName().equals(genericEvent)) {
            // this is used when we are processing the genericEvent
            eventName = parameters[0].getName();
            startJ = 1;
        } else {
            eventName = '\"' + dm.getName() + '\"';
        }

        methodIDAnotation = writeEventNameOrId(dm, body, eventName);
        if (methodIDAnotation.length() > 0 && !methodIds.add(methodIDAnotation))
            throw new MethodWriterValidationException("Duplicate methodIds. Cannot add " + methodIDAnotation + " to " + methodIds);

        if (parameters.length > 0)
            writeArrayOfParameters(dm, len, body, startJ);

        if (dm.getParameterTypes().length == 0)
            body.append("valueOut.text(\"\");\n");

        body.append("} catch (Throwable t) {\n");
        body.append("dc.rollbackOnClose();\n");
        body.append("throw Jvm.rethrow(t);\n");
        body.append("}\n");
        if (!passthrough)
            body.append("}\n");

        return format("\n%s public %s %s(%s) {\n %s%s}\n",
                methodIDAnotation,
                typeName,
                dm.getName(),
                methodSignature(importSet, dm),
                body,
                methodReturn(importSet, dm, interfaceClazz));
    }

    private String returnDefault(final Class<?> returnType) {

        if (returnType == void.class)
            return "";

        if (returnType.isPrimitive() || returnType == Void.class)
            throw new UnsupportedOperationException("having a method of this return type=" + returnType + " is not supported by method writers");

        return " this";
    }

    private String writeEventNameOrId(final Method dm, final StringBuilder body, final String eventName) {
        String methodID = "";
        final Optional<Annotation> methodId = useMethodId ? stream(dm.getAnnotations()).filter(MethodId.class::isInstance).findFirst() : Optional.empty();
        if ((wireType != WireType.TEXT && wireType != WireType.YAML) && methodId.isPresent()) {

            long value = ((MethodId) methodId.get()).value();
            body.append(format("final " + VALUE_OUT + " valueOut = dc.wire().writeEventId(%s, %d);\n", eventName, value));
            methodID = format("@" + METHOD_ID + "(%d)\n", value);

        } else
            body.append(format("final " + VALUE_OUT + " valueOut = dc.wire().writeEventName(%s);\n", eventName));
        return methodID;
    }

    private void writeArrayOfParameters(final Method dm, final int len, final StringBuilder body, final int startJ) {
        if (dm.getParameterTypes().length > startJ + 1)
            body.append("valueOut.array(v -> {\n");
        for (int j = startJ; j < len; j++) {

            final Parameter p = dm.getParameters()[j];

            final Optional<String> intConversion = stream(p.getAnnotations())
                    .filter(a -> a.annotationType() == IntConversion.class)
                    .map(x -> (((IntConversion) x).value().getName()))
                    .findFirst();

            final Optional<String> longConversion = stream(p.getAnnotations())
                    .filter(a -> a.annotationType() == LongConversion.class)
                    .map(x -> (((LongConversion) x).value().getName()))
                    .findFirst();

            final String name = intConversion.orElseGet(() -> (longConversion.orElse("")));

            if (!name.isEmpty() && (WireType.TEXT == wireType || WireType.YAML == wireType))
                body.append(format("//todo improve this\nvalueOut.rawText(new %s().asText(%s));\n", name, p.getName()));
            else if (p.getType().isPrimitive() || CharSequence.class.isAssignableFrom(p.getType())) {
                if (longConversion.isPresent())
                    body.append(format("%s.writeLong(%s.INSTANCE, %s);\n", dm.getParameterTypes().length > startJ + 1 ? "v" : "valueOut", longConversion.get(), p.getName()));
                else if (intConversion.isPresent())
                    body.append(format("%s.writeInt(%s.INSTANCE, %s);\n", dm.getParameterTypes().length > startJ + 1 ? "v" : "valueOut", intConversion.get(), p.getName()));
                else
                    body.append(format("%s.%s(%s);\n", dm.getParameterTypes().length > startJ + 1 ? "v" : "valueOut", toString(p.getType()), p.getName()));
            } else
                writeValue(dm, body, startJ, p);
        }

        if (dm.getParameterTypes().length > startJ + 1)
            body.append("}, Object[].class);\n");
    }

    private void writeValue(final Method dm, final StringBuilder body, final int startJ, final Parameter p) {
        String className = p.getType().getTypeName().replace('$', '.');

        body
                .append(dm.getParameterTypes().length > startJ + 1 ? "v" : "valueOut")
                .append(".object(")
                .append(className)
                .append(".class, ")
                .append(p.getName())
                .append(");\n");
    }

    private StringBuilder methodReturn(Set<String> importSet, final Method dm, final Class<?> interfaceClazz) {
        final StringBuilder result = new StringBuilder();
        final Class<?> returnType = (Class<?>) GenericReflection.getReturnType(dm, interfaceClazz);

        if (returnType == Void.class || returnType == void.class)
            return result;

        if (returnType == DocumentContext.class) {
            result.append("return dc;\n");

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
