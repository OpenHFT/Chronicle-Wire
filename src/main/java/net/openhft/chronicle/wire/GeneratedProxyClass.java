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

import net.openhft.chronicle.core.Jvm;
import net.openhft.compiler.CompilerUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The purpose of this class is to generate a proxy that will re-use the arg[]
 */
@SuppressWarnings("restriction")
public enum GeneratedProxyClass {
    ;
    static final boolean DUMP_CODE = Jvm.getBoolean("dumpCode");

    /**
     * @param classLoader
     * @param interfaces  an interface class
     * @return a proxy class from an interface class or null if it can't be created
     */
    @SuppressWarnings("rawtypes")
    public static Class from(String packageName, Set<Class> interfaces, String className, ClassLoader classLoader) {
        int maxArgs = 0;
        Set<Method> methods = new LinkedHashSet<>(16);

        StringBuilder sb = new StringBuilder("package " + packageName + ";\n\n" +
                "import net.openhft.chronicle.core.Jvm;\n" +
                "import net.openhft.chronicle.wire.MethodWriterInvocationHandlerSupplier;\n" +
                "import java.lang.reflect.InvocationHandler;\n" +
                "import java.lang.reflect.Method;\n" +
                "import java.util.stream.IntStream;\n" +
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
            String interfaceName = nameForClass(interfaceClazz);
            sb.append(interfaceName);

            if (!interfaceClazz.isInterface())
                throw new IllegalArgumentException("expecting an interface instead of class=" + interfaceClazz.getName());

            Method[] dms = interfaceClazz.getMethods();
            int n = dms.length;

            for (int i = 0; i < n; ++i) {
                Method dm = dms[i];
                if (dm.isDefault() || Modifier.isStatic(dm.getModifiers()))
                    continue;

                if (dm.getGenericReturnType() instanceof TypeVariable)
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

        if (DUMP_CODE)
            System.out.println(sb);

        try {
            return CompilerUtils.CACHED_COMPILER.loadFromJava(classLoader, packageName + '.' + className, sb.toString());
        } catch (Throwable e) {
            throw Jvm.rethrow(new ClassNotFoundException(e.getMessage() + '\n' + sb, e));
        }
    }

    @NotNull
    private static String nameForClass(Class interfaceClazz) {
        return interfaceClazz.getName().replace('$', '.');
    }

    private static void addFieldsAndConstructor(final int maxArgs, final Set<Method> declaredMethods, final StringBuilder sb, final String className, final StringBuilder methodArray) {
        sb.append("  private final MethodWriterInvocationHandlerSupplier handler;\n" +
                "    private final Method[] methods = new Method[")
                .append(declaredMethods.size())
                .append("];\n")
                .append("  private static final int maxArgs = " + maxArgs + ";\n")
                .append("  private final ThreadLocal<Object[][]> argsTL = " +
                        "ThreadLocal.withInitial(() -> IntStream.range(0, maxArgs + 1)" +
                        ".mapToObj(Object[]::new).toArray(Object[][]::new));\n\n")
                .append("  public ")
                .append(className)
                .append("(MethodWriterInvocationHandlerSupplier handler) {\n")
                .append("    this.handler = handler;\n")
                .append(methodArray)
                .append("  }\n\n");
    }

    private static void createProxyMethods(final Set<Method> declaredMethods, final StringBuilder sb) {
        int methodIndex = -1;
        for (final Method dm : declaredMethods) {

            final Class<?> returnType = dm.getReturnType();

            methodIndex++;

            sb.append(createMethodSignature(dm, returnType));
            sb.append("    Method _method_ = this.methods[").append(methodIndex).append("];\n");
            sb.append("    Object[] _a_ = this.argsTL.get()[").append(dm.getParameterCount()).append("];\n");

            assignParametersToArgs(sb, dm);
            callInvoke(sb, returnType);
        }
    }

    private static void callInvoke(final StringBuilder sb, final Class<?> returnType) {
        sb.append("    try {\n" +
                "      ");

        if (returnType != void.class)
            sb.append("return (").append(nameForClass(returnType)).append(')');

        sb.append(" handler.get().invoke(this,_method_,_a_);\n" +
                "    } catch (Throwable throwable) {\n" +
                "       throw Jvm.rethrow(throwable);\n" +
                "    }\n" +
                "  }\n");
    }

    private static void assignParametersToArgs(final StringBuilder sb, final Method dm) {
        final int len = dm.getParameters().length;
        for (int j = 0; j < len; j++) {
            String paramName = dm.getParameters()[j].getName();
            sb.append("    _a_[").append(j).append("] = ").append(paramName).append(";\n");
        }
    }

    private static CharSequence createMethodSignature(final Method dm, final Class<?> returnType) {
        final int len = dm.getParameters().length;
        final StringBuilder result = new StringBuilder();
        final String typeName = nameForClass(returnType);
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
