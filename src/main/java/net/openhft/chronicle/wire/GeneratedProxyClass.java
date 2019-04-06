package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.compiler.CompilerUtils;
import sun.reflect.generics.reflectiveObjects.TypeVariableImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by Rob Austin
 * <p>
 * The purpose of this class is to generate a proxy that will re-use the arg[]
 */
@SuppressWarnings("restriction")
public enum GeneratedProxyClass {
    ;

    /**
     * @param interfaces an interface class
     * @return a proxy class from an interface class or null if it can't be created
     */
    @SuppressWarnings("rawtypes")
    public static Class from(String packageName, Set<Class> interfaces, String className) {
        int maxArgs = 0;
        Set<Method> methods = new LinkedHashSet<>(16);

        StringBuilder sb = new StringBuilder("package " + packageName + ";\n\n" +
                "import net.openhft.chronicle.core.Jvm;\n" +
                "import java.lang.reflect.InvocationHandler;\n" +
                "import java.lang.reflect.Method;\n" +
                "import java.util.ArrayList;\n" +
                "import java.util.List;\n");

        sb.append("public class ")
                .append(className)
                .append(" implements ");


        final StringBuilder methodArray = new StringBuilder();
        int count = 0;

        String sep = "";
        // create methodArray
        for (Class interfaceClazz : interfaces) {
            sb.append(sep);
            String interfaceName = interfaceClazz.getName().replace('$', '.');
            sb.append(interfaceName);

            if (!interfaceClazz.isInterface())
                throw new IllegalArgumentException("expecting and interface instead of class=" + interfaceClazz.getName());

            Method[] dms = interfaceClazz.getMethods();
            int n = dms.length;

            for (int i = 0; i < n; ++i) {
                Method dm = dms[i];
                if (dm.isDefault() || Modifier.isStatic(dm.getModifiers()))
                    continue;

                if (dm.getGenericReturnType() instanceof TypeVariableImpl)
                    return null;

                if (!methods.add(dm))
                    continue;

                maxArgs = Math.max(maxArgs, dm.getParameterCount());

                methodArray.append("\n    //").append(createMethodSignature(dm, dm.getReturnType()))
                        .append("    methods[").append(count++)
                        .append("]=").append(interfaceName)
                        .append(".class.getMethods()[").append(i)
                        .append("];\n");
            }

            sep = ",\n              ";
        }
        sb.append(" {\n" +
                '\n');

        addFieldsAndConstructor(maxArgs, methods, sb, className, methodArray);

        createProxyMethods(methods, sb);
        sb.append("}\n");

        try {
            // synchronizing due to ConcurrentModificationException in net.openhft.compiler.MyJavaFileManager.buffers
            synchronized (CompilerUtils.CACHED_COMPILER) {
                return CompilerUtils.CACHED_COMPILER.loadFromJava(GeneratedProxyClass.class.getClassLoader(), packageName + '.' + className, sb.toString());
            }
        } catch (Throwable e) {
            throw Jvm.rethrow(new ClassNotFoundException(e.getMessage() + '\n' + sb, e));
        }

    }

    private static void addFieldsAndConstructor(final int maxArgs, final Set<Method> declaredMethods, final StringBuilder sb, final String className, final StringBuilder methodArray) {

        sb.append("  private final Object proxy;\n" +
                "  private final InvocationHandler handler;\n" +
                "  private Method[] methods = new  Method[")
                .append(declaredMethods.size())
                .append("];\n")
                .append("  private List<Object[]> args = new ArrayList<Object[]>(")
                .append(maxArgs + 1)
                .append(");\n\n")
                .append("  public ")
                .append(className)
                .append("(Object proxy, InvocationHandler handler) {\n")
                .append("    this.proxy = proxy;\n")
                .append("    this.handler = handler;\n");
        for (int j = 0; j <= maxArgs; j++) {
            sb.append("    args.add(new Object[")
                    .append(j)
                    .append("]);\n");
        }

        sb.append(methodArray);
        sb.append("  }\n" +
                '\n');
    }

    private static void createProxyMethods(final Set<Method> declaredMethods, final StringBuilder sb) {
        int methodIndex = -1;
        for (final Method dm : declaredMethods) {

            final Class<?> returnType = dm.getReturnType();

            methodIndex++;

            sb.append(createMethodSignature(dm, returnType));
            sb.append("    Method method = this.methods[").append(methodIndex).append("];\n");
            sb.append("    Object[] a = this.args.get(").append(dm.getParameterCount()).append(");\n");

            assignParametersToArgs(sb, dm);
            callInvoke(sb, returnType);
        }
    }

    private static void callInvoke(final StringBuilder sb, final Class<?> returnType) {
        sb.append("    try {\n" +
                "      ");

        if (returnType != void.class)
            sb.append("return (").append(returnType.getName()).append(')');

        sb.append(" handler.invoke(proxy,method,a);\n" +
                "    } catch (Throwable throwable) {\n" +
                "       throw Jvm.rethrow(throwable);\n" +
                "    }\n" +
                "  }\n");
    }

    private static void assignParametersToArgs(final StringBuilder sb, final Method dm) {
        final int len = dm.getParameters().length;
        for (int j = 0; j < len; j++) {
            String paramName = dm.getParameters()[j].getName();
            sb.append("    a[").append(j).append("] = ").append(paramName).append(";\n");
        }
    }

    private static CharSequence createMethodSignature(final Method dm, final Class<?> returnType) {
        final int len = dm.getParameters().length;
        final StringBuilder result = new StringBuilder();
        final String typeName = returnType.getName();
        result.append("  public ").append(typeName).append(' ').append(dm.getName()).append('(');

        for (int j = 0; j < len; j++) {
            Parameter p = dm.getParameters()[j];

            String className = p.getType().getTypeName().replace('$', '.');
            result.append(className)
                    .append(' ')
                    .append(p.getName());
            if (j == len - 1)
                break;

            result.append(',');
        }

        result.append(") {\n");
        return result;
    }

}
