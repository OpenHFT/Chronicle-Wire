package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodWriterListener;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.compiler.CompilerUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static java.util.Arrays.stream;

public class GenerateMethodWriter {

    static final boolean DUMP_CODE = Boolean.getBoolean("dumpCode");
    private static final String SHARED_DOCUMENT_CONTEXT = SharedDocumentContext.class.getSimpleName();
    private static final String DOCUMENT_CONTEXT_HOLDER = DocumentContextHolder.class.getSimpleName();

    private static boolean recordHistory;
    private static AtomicInteger count = new AtomicInteger();
    private final boolean metaData;
    private final boolean useMethodId;
    private final String packageName;
    private final Set<Class> interfaces;
    private final String className;
    private final ClassLoader classLoader;
    private final WireType wireType;
    private final String genericEvent;
    AtomicInteger i = new AtomicInteger();
    MarshallableOut o;
    private ConcurrentMap<Class, Integer> methodWritersMap = new ConcurrentHashMap<>();
    private boolean hasMethodWriterListener;

    private GenerateMethodWriter(final String packageName,
                                 final Set<Class> interfaces,
                                 final String className,
                                 final ClassLoader classLoader,
                                 final WireType wireType,
                                 final String genericEvent,
                                 final boolean hasMethodWriterListener,
                                 final boolean metaData,
                                 final boolean useMethodId) {

        this.packageName = packageName;
        this.interfaces = interfaces;
        this.className = className;
        this.classLoader = classLoader;
        this.wireType = wireType;
        this.genericEvent = genericEvent;
        this.hasMethodWriterListener = hasMethodWriterListener;
        this.metaData = metaData;
        this.useMethodId = useMethodId;
    }

    /**
     * @param interfaces   an interface class
     * @param classLoader
     * @param wireType
     * @param genericEvent
     * @return a proxy class from an interface class or null if it can't be created
     */

    @Nullable
    public static Class from(String packageName,
                             Set<Class> interfaces,
                             String className,
                             ClassLoader classLoader,
                             final WireType wireType,
                             final String genericEvent,
                             boolean hasMethodWriterListener,
                             boolean metaData, boolean useMethodId) {

        return new GenerateMethodWriter(packageName,
                interfaces,
                className,
                classLoader,
                wireType,
                genericEvent,
                hasMethodWriterListener,
                metaData, useMethodId).createClass();
    }

    public static DocumentContext acquireDocumentContext(boolean metaData, ThreadLocal<DocumentContextHolder> documentContextTL, MarshallableOut out) {
        DocumentContextHolder contextHolder = documentContextTL.get();
        if (!contextHolder.isClosed())
            return contextHolder;

        contextHolder.documentContext(out.writingDocument(metaData));
        return contextHolder;
    }

    public static void addComment(Bytes<?> bytes, Object arg) {

        if (arg instanceof Marshallable)
            bytes.comment(arg.getClass().getSimpleName());
        else
            bytes.comment(String.valueOf(arg));
    }

    @NotNull
    private static StringBuilder methodSignature(final Method dm, final int len) {

        StringBuilder result = new StringBuilder();
        for (int j = 0; j < len; j++) {
            Parameter p = dm.getParameters()[j];
            String className = p.getType().getTypeName().replace('$', '.');

            Optional<String> intConversion = stream(p.getAnnotations()).filter(a -> a.annotationType() == IntConversion.class).map(x -> (((IntConversion) x).value().getName())).findFirst();
            Optional<String> longConversion = stream(p.getAnnotations()).filter(a -> a.annotationType() == LongConversion.class).map(x -> (((LongConversion) x).value().getName())).findFirst();

            if (intConversion.isPresent())
                result.append("@IntConversion(").append(intConversion.get()).append(".class) ");
            else longConversion.ifPresent(s -> result.append("@LongConversion(").append(s).append(".class) "));

            result.append("final ");
            result.append(className);

            result.append(' ')
                    .append(p.getName());
            if (j == len - 1)
                break;

            result.append(',');
        }
        return result;
    }

    private static CharSequence toString(Class type) {
        if (boolean.class.equals(type)) {
            return "bool";
        } else if (byte.class.equals(type)) {
            return "bytes";
        } else if (char.class.equals(type)) {
            return "character";
        } else if (short.class.equals(type)) {
            return "int16";
        } else if (int.class.equals(type)) {
            return "fixedInt32";
        } else if (long.class.equals(type)) {
            return "fixedInt64";
        } else if (float.class.equals(type)) {
            return "fixedFloat32";
        } else if (double.class.equals(type)) {
            return "fixedFloat64";
        } else if (CharSequence.class.isAssignableFrom(type)) {
            return "text";
        } else if (Marshallable.class.isAssignableFrom(type)) {
            return "marshallable";
        }

        return "object";
    }

    ;

    private Class createClass() {

        int maxArgs = 0;
        // className = className + i.getAndIncrement();
        StringBuilder interfaceMethods = new StringBuilder();

        StringBuilder sb = new StringBuilder();
        try {
            sb.append("package " + packageName + ";\n\n" +

                    "import net.openhft.chronicle.wire.*;\n" +
                    "import " + IntConversion.class.getName() + ";\n" +
                    "import " + LongConversion.class.getName() + ";\n" +
                    "import " + GenerateMethodWriter.class.getName() + ";\n" +
                    "import " + MessageHistory.class.getName() + ";\n" +
                    "import " + MethodReader.class.getName() + ";\n" +
                    "import " + MethodId.class.getName() + ";\n" +
                    "import " + GenerateMethodWriter.class.getName() + ";\n" +
                    "import " + DocumentContext.class.getName() + ";\n" +
                    "import " + SharedDocumentContext.class.getName() + ";\n" +
                    "import " + MethodWriterInvocationHandlerSupplier.class.getName() + ";\n" +
                    "import " + Jvm.class.getName() + ";\n" +
                    "import " + Closeable.class.getName() + ";\n" +
                    "import " + DocumentContextHolder.class.getName() + ";\n" +
                    "import " + MethodWriterListener.class.getName() + ";\n" +
                    "import java.lang.reflect.InvocationHandler;\n" +
                    "import java.lang.reflect.Method;\n" +
                    "import java.util.stream.IntStream;\n" +
                    "import java.util.ArrayList;\n" +
                    "import java.util.List;\n");

            sb.append("public final class ")
                    .append(className)
                    .append(" implements ");

            final StringBuilder methodArray = new StringBuilder();
            int count = 0;

            String sep = "";
            // create methodArray
            for (Class interfaceClazz : interfaces) {
                sb.append(sep);
                String interfaceName = nameForClass(interfaceClazz);
                sb.append(interfaceName);
                sb.append(",");

                if (!interfaceClazz.isInterface())
                    throw new IllegalArgumentException("expecting and interface instead of class=" + interfaceClazz.getName());

                Method[] dms = interfaceClazz.getMethods();
                int n = dms.length;

                for (int i = 0; i < n; ++i) {
                    Method dm = dms[i];
                    if (dm.isDefault() || Modifier.isStatic(dm.getModifiers()))
                        continue;

                    if (dm.getName().equals("documentContext") && dm.getParameterTypes().length == 1 && dm.getParameterTypes()[0] == ThreadLocal.class && SharedDocumentContext.class.isAssignableFrom(dm.getReturnType()))
                        continue;

                    //    if (dm.getGenericReturnType() instanceof TypeVariableImpl)
                    //       return null;

                    interfaceMethods.append(createMethod(dm, interfaceClazz, wireType, genericEvent, hasMethodWriterListener, metaData, useMethodId));

                }

            }

            sb.setLength(sb.length() - 1);
            sb.append(" {\n\n");

            sb.append(constructorAndFields(className));
            sb.append(methodDocumentContext());
            sb.append(interfaceMethods);
            sb.append("}\n");

            //    if (DUMP_CODE)
            System.out.println(sb);

            //   try {

            //      return Class.forName(packageName + '.' + className);
            //    } catch (ClassNotFoundException e) {
            return CompilerUtils.CACHED_COMPILER.loadFromJava(classLoader, packageName + '.' + className, sb.toString());
            //    }
        } catch (
                Throwable e) {
            System.out.println(sb.toString());
            e.printStackTrace();
            throw Jvm.rethrow(new ClassNotFoundException(e.getMessage() + '\n' + sb, e));
        }

    }

    @NotNull
    private String nameForClass(Class interfaceClazz) {
        return interfaceClazz.getName().replace('$', '.');
    }

    private CharSequence constructorAndFields(final String className) {

        methodDocumentContext();

        final StringBuilder fields = new StringBuilder("  // fields\n");

        fields.append("  private transient final Closeable closeable;\n");
        fields.append("  private transient final MethodWriterListener methodWriterListener;\n");
        fields.append("  private transient final MarshallableOut out;\n");
        fields.append("  private transient ThreadLocal<" + DOCUMENT_CONTEXT_HOLDER + "> documentContextTL = ThreadLocal.withInitial(DocumentContextHolder::new);\n");

        for (Map.Entry<Class, Integer> e : methodWritersMap.entrySet()) {
            fields.append(format("  private %s methodWriter%s;\n", e.getKey().getName(), e.getValue()));
        }
        fields.append("\n");

        StringBuilder constructor = new StringBuilder("  // constructor\n");
        constructor.append(format("  public %s(MarshallableOut out, Closeable closeable, MethodWriterListener methodWriterListener) {\n" +
                "    this.methodWriterListener = methodWriterListener;\n" +
                "    this.out = out;\n" +
                "    this.closeable = closeable;\n" +
                "  }\n\n", className));

        return fields.append(constructor);

    }

    private CharSequence methodDocumentContext() {
        return ("  // method documentContext\n" +
                "  @Override\n" +
                "  public <T extends " + SHARED_DOCUMENT_CONTEXT + "> T documentContext(final ThreadLocal<" + DOCUMENT_CONTEXT_HOLDER + "> documentContextTL) {\n" +
                "    this.documentContextTL = documentContextTL; \n" +
                "    return (T)this;\n" +
                "  }\n\n");
    }

    private CharSequence createMethod(final Method dm, final Class<?> interfaceClazz, final WireType wireType, final String genericEvent, final boolean hasMethodWriterListener, boolean metaData, boolean useMethodId) {

        if (Modifier.isStatic(dm.getModifiers()))
            return "";

        if (dm.getParameterTypes().length == 0 && dm.isDefault()) {
            return "";
        }

        if ("writingDocument".contentEquals(dm.getName()) && dm.getReturnType() == DocumentContext.class && dm.getParameterCount() == 0) {
            return "public DocumentContext writingDocument(){\n return GenerateMethodWriter.acquireDocumentContext(false,this.documentContextTL,this.out);\n}\n";
        }

        final int len = dm.getParameters().length;
        Class<?> returnType = dm.getReturnType();
        final String typeName = nameForClass(returnType);

        final StringBuilder methodBody = new StringBuilder();
        String methodID = "";
        if (dm.getReturnType() == void.class && "close".equals(dm.getName()) && dm.getParameterCount() == 0) {
            methodBody.append("    if (this.closeable != null)\n");
            methodBody.append("        this.closeable.close();\n");
        } else {

            methodBody.append(format("    final DocumentContext dc = GenerateMethodWriter.acquireDocumentContext(%s,this.documentContextTL,this.out);\n\n", metaData));

            methodBody.append(recordHistory());

            int startJ = 0;

            String eventName;
            if (dm.getParameterCount() > 0 && dm.getName().equals(genericEvent)) {
                // this is used when we are processing the genericEvent
                eventName = dm.getParameters()[0].getName();
                startJ = 1;
            } else {
                eventName = '\"' + dm.getName() + '\"';
            }

            methodBody.append(format("    if (dc.wire().bytes().retainsComments())\n     dc.wire().bytes().comment(\"%s\");\n", dm.getName()));

            Optional<Annotation> methodId = useMethodId ? stream(dm.getAnnotations()).filter(f -> f instanceof MethodId).findFirst() : Optional.empty();
            if ((wireType != WireType.TEXT && wireType != WireType.YAML) && methodId.isPresent()) {

                long value = ((MethodId) methodId.get()).value();
                methodBody.append(format("    final ValueOut valueOut = dc.wire().writeEventId(%d);\n", value));
                methodID = format("  @MethodId(%d)\n", value);

            } else
                methodBody.append(format("    final ValueOut valueOut = dc.wire().writeEventName(%s);\n", eventName));

            if (dm.getParameterCount() - startJ == 1) {
                methodBody.append(String.format("    if (dc.wire().bytes().retainsComments())\n   GenerateMethodWriter.addComment(dc.wire().bytes(), %s);\n  ", dm.getParameters()[0].getName()));
            }

            if (hasMethodWriterListener && dm.getParameterCount() > 0)
                createMethodWriterListener(dm, methodBody);
            else if (dm.getParameters().length > 0) {

                if (dm.getParameterTypes().length > startJ + 1)
                    methodBody.append("    valueOut.array(v -> {\n");
                for (int j = startJ; j < len; j++) {

                    Parameter p = dm.getParameters()[j];

                    Optional<String> intConversion = stream(p.getAnnotations()).filter(a -> a.annotationType() == IntConversion.class).map(x -> (((IntConversion) x).value().getName())).findFirst();
                    Optional<String> longConversion = stream(p.getAnnotations()).filter(a -> a.annotationType() == LongConversion.class).map(x -> (((LongConversion) x).value().getName())).findFirst();
                    String name = intConversion.orElseGet(() -> (longConversion.orElse("")));

                    if (!name.isEmpty() && (WireType.TEXT == wireType || WireType.YAML == wireType))
                        methodBody.append(format("        //todo improve this\n        valueOut.rawText(new %s().asText(%s));\n", name, p.getName()));
                    else if (p.getType().isPrimitive() || CharSequence.class.isAssignableFrom(p.getType())) {
                        if ("int64".contentEquals(toString(p.getType())))
                            methodBody.append(format("        %s.object(long.class,%s);\n", dm.getParameterTypes().length > startJ + 1 ? "v" : "valueOut", p.getName()));
                        else

                            methodBody.append(format("        %s.%s(%s);\n", dm.getParameterTypes().length > startJ + 1 ? "v" : "valueOut", toString(p.getType()), p.getName()));
                    } else {
                        String className = p.getType().getTypeName().replace('$', '.');

                        if (Marshallable.class.isAssignableFrom(p.getType())) {
                            methodBody.append(format("        if (%s.getClass() == %s.class )\n ", p.getName(), className));
                            methodBody.append(format("          %s.marshallable(%s);\n", dm.getParameterTypes().length > startJ + 1 ? "v" : "valueOut", p.getName()));
                            methodBody.append("        else\n");
                        }
                        // methodBody.append(format("          %s.object(%s);\n", dm.getParameterTypes().length > startJ + 1 ? "v" : "valueOut", p.getName()));

                        methodBody.append(format("          %s.object(%s);\n", dm.getParameterTypes().length > startJ + 1 ? "v" : "valueOut", p.getName()));

                    }

                }

                if (dm.getParameterTypes().length > startJ + 1)
                    methodBody.append("    }, Object[].class);\n");

            }

            if (dm.getParameterTypes().length == 0)
                methodBody.append("    valueOut.text(\"\");\n");

            if (returnType == Void.class || returnType == void.class || returnType.isPrimitive()) {
                methodBody.append("    dc.close();\n");
            }

        }
        return

                format("%s  public %s %s(%s){\n" +
                                "%s" +
                                "%s" +
                                "  }\n\n", methodID, typeName, dm.getName(), methodSignature(dm, len), methodBody,

                        methodReturn(dm, interfaceClazz));
    }

    private void createMethodWriterListener(final Method dm, final StringBuilder methodBody) {
        methodBody.append("    Object[] args$$ = new Object[]{");
        for (int i1 = 0; i1 < dm.getParameters().length; i1++) {
            methodBody.append("(Object)" + dm.getParameters()[i1].getName() + ",");
        }
        methodBody.setLength(methodBody.length() - 1);
        methodBody.append("};\n");

        methodBody.append(format("    this.methodWriterListener.onWrite(\"%s\",args$$);\n", dm.getName()));
        if (dm.getParameterCount() == 1) {
            if (Marshallable.class.isAssignableFrom(dm.getParameterTypes()[0])) {
                methodBody.append(format("        if (args$$[0].getClass() == %s.class)\n ", dm.getParameterTypes()[0].getName().replace('$', '.')));
                methodBody.append("             valueOut.marshallable((Marshallable)args$$[0]);\n");
                methodBody.append("          else\n");
                methodBody.append("             valueOut.object(args$$[0]);\n");
            } else {
                methodBody.append("             valueOut.object(args$$[0]);\n");
            }

        } else {
            methodBody.append("    valueOut.object(args$$);\n");
        }
    }

    private StringBuilder recordHistory() {
        final StringBuilder result = new StringBuilder();
        result.append("    // record history\n");
        result.append("    if (out.recordHistory())   \n" +
                "        dc.wire().writeEventName(MethodReader.HISTORY).marshallable(MessageHistory.get());\n\n");

        return result;
    }

    private StringBuilder methodReturn(final Method dm, final Class<?> interfaceClazz) {
        final StringBuilder result = new StringBuilder();
        if (dm.getReturnType() == Void.class || dm.getReturnType() == void.class)
            return result;

        if (dm.getReturnType().isAssignableFrom(interfaceClazz) || dm.getReturnType() == interfaceClazz) {
            result.append("    return this;\n");
        } else if (dm.getReturnType().isInterface()) {
            String index = methodWritersMap.computeIfAbsent(dm.getReturnType(), k -> count.incrementAndGet()).toString();
            result.append("    // method return\n");

            result.append(format("    return ((" + SHARED_DOCUMENT_CONTEXT + ") (methodWriter%s ==null\n" +
                    "        ? methodWriter%s = out.methodWriter(%s.class)\n" +
                    "        :  methodWriter%s)).documentContext(documentContextTL);\n", index, index, dm.getReturnType().getName(), index));
        } else if (!dm.getReturnType().isPrimitive()) {
            result.append("    return null;\n");
        } else if (dm.getReturnType() == boolean.class) {
            result.append("    return false;\n");
        } else if (dm.getReturnType() == byte.class) {
            result.append("    return (byte)0;\n");
        } else {
            result.append("    return 0;\n");
        }

        return result;

    }

}

