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

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This is the GeneratedProxyClass enum.
 * Its primary purpose is to generate a proxy class that will re-use the arg[] of the invoked method.
 * This generation is specifically designed around the given interfaces and constructs the Java code in string
 * format for the proxy class. Furthermore, it provides an option to dump the generated code for debugging purposes.
 */
@SuppressWarnings("restriction")
public enum GeneratedProxyClass {
    ; // This enum does not have any instances; it is used solely for its static members.

    // Indicates if the generated Java code should be dumped for debugging purposes.
    static final boolean DUMP_CODE = Jvm.getBoolean("dumpCode");

    /**
     * Generates a proxy class based on the given interfaces and other parameters.
     * The resulting proxy class will be designed to re-use the arg[] of its invoked methods.
     *
     * @param packageName The package name for the generated proxy class.
     * @param interfaces  A set containing the interface classes that the proxy should implement.
     * @param className   The name of the generated proxy class.
     * @param classLoader The class loader used to define the proxy class.
     * @return The proxy class based on the provided interface classes, or null if it cannot be created.
     */
    @SuppressWarnings("rawtypes")
    public static Class<?> from(String packageName, Set<Class> interfaces, String className, ClassLoader classLoader) {
        int maxArgs = 0;
        Set<Method> methods = new LinkedHashSet<>(16);

        // Builds the initial portion of the proxy class's Java code.
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
        for (Class<?> interfaceClazz : interfaces) {
            sb.append(sep);
            String interfaceName = nameForClass(interfaceClazz);
            sb.append(interfaceName);

            // Ensure the provided class is actually an interface
            if (!interfaceClazz.isInterface())
                throw new IllegalArgumentException("expecting an interface instead of class=" + interfaceClazz.getName());

            Method[] dms = interfaceClazz.getMethods();
            int n = dms.length;

            for (int i = 0; i < n; ++i) {
                Method dm = dms[i];

                // Skip default or static methods
                if (dm.isDefault() || Modifier.isStatic(dm.getModifiers()))
                    continue;

                // Handle methods with a generic return type
                if (dm.getGenericReturnType() instanceof TypeVariable)
                    return null;

                // Add the method only if it hasn't been added already
                if (!methods.add(dm))
                    continue;

                // Determine the maximum argument count across all methods
                maxArgs = Math.max(maxArgs, dm.getParameterCount());

                // Append information about the method to the methodArray
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

        // Add fields and the constructor to the generated proxy class's code
        addFieldsAndConstructor(maxArgs, methods, sb, className, methodArray);

        // Generate the proxy methods
        createProxyMethods(methods, sb);
        sb.append("}\n");

        // If DUMP_CODE is true, print the generated Java code
        if (DUMP_CODE)
            System.out.println(sb);

        // Attempt to load the generated proxy class
        try {
            return Wires.loadFromJava(classLoader, packageName + '.' + className, sb.toString());
        } catch (Throwable e) {
            throw Jvm.rethrow(new ClassNotFoundException(e.getMessage() + '\n' + sb, e));
        }
    }

    /**
     * Converts the provided class's name by replacing the dollar sign '$' with a period '.'.
     * Typically used to convert nested class names to their canonical form.
     *
     * @param interfaceClazz The class whose name needs to be converted.
     * @return The converted name string.
     */
    @NotNull
    private static String nameForClass(Class<?> interfaceClazz) {
        return interfaceClazz.getName().replace('$', '.');
    }

    /**
     * Constructs the fields and constructor for the proxy class. This includes the method array,
     * maximum arguments, a thread-local to store method arguments, and the constructor which initializes these fields.
     *
     * @param maxArgs        The maximum number of arguments amongst the declared methods.
     * @param declaredMethods The set of methods to be proxied.
     * @param sb             The StringBuilder to which the generated code is appended.
     * @param className      The name of the generated proxy class.
     * @param methodArray    A StringBuilder containing the array of declared methods.
     */
    private static void addFieldsAndConstructor(final int maxArgs, final Set<Method> declaredMethods, final StringBuilder sb, final String className, final StringBuilder methodArray) {
        // Define fields for the proxy class
        sb.append("  private final MethodWriterInvocationHandlerSupplier handler;\n" +
                        "    private final Method[] methods = new Method[")
                .append(declaredMethods.size())
                .append("];\n")
                .append("  private static final int maxArgs = " + maxArgs + ";\n")
                .append("  private final ThreadLocal<Object[][]> argsTL = " +
                        "ThreadLocal.withInitial(() -> IntStream.range(0, maxArgs + 1)" +
                        ".mapToObj(Object[]::new).toArray(Object[][]::new));\n\n")
                        // Define constructor for the proxy class
                .append("  public ")
                .append(className)
                .append("(MethodWriterInvocationHandlerSupplier handler) {\n")
                .append("    this.handler = handler;\n")
                .append(methodArray)
                .append("  }\n\n");
    }

    /**
     * Generates the method signatures for proxy methods, including the logic to capture method arguments and invoke the actual method.
     *
     * @param declaredMethods The set of methods to be proxied.
     * @param sb              The StringBuilder to which the generated code is appended.
     */
    private static void createProxyMethods(final Set<Method> declaredMethods, final StringBuilder sb) {
        int methodIndex = -1;
        for (final Method dm : declaredMethods) {
            // Get return type of the method
            final Class<?> returnType = dm.getReturnType();

            methodIndex++;

            // Append method signature to the StringBuilder
            sb.append(createMethodSignature(dm, returnType));
            sb.append("    Method _method_ = this.methods[").append(methodIndex).append("];\n");
            sb.append("    Object[] _a_ = this.argsTL.get()[").append(dm.getParameterCount()).append("];\n");

            // Assign method parameters to local array
            assignParametersToArgs(sb, dm);
            // Handle method invocation
            callInvoke(sb, returnType);
        }
    }

    /**
     * Generates the method invocation logic. This includes invoking the method and handling any potential exceptions.
     *
     * @param sb         The StringBuilder to which the generated code is appended.
     * @param returnType The return type of the method being invoked.
     */
    private static void callInvoke(final StringBuilder sb, final Class<?> returnType) {
        // Start method invocation
        sb.append("    try {\n" +
                "      ");

        // Check if the method has a return type other than void
        if (returnType != void.class)
            sb.append("return (").append(nameForClass(returnType)).append(')');

        // Invoke the method
        sb.append(" handler.get().invoke(this,_method_,_a_);\n" +
                "    } catch (Throwable throwable) {\n" +
                // Handle exceptions by rethrowing them
                "       throw Jvm.rethrow(throwable);\n" +
                "    }\n" +
                "  }\n");
    }

    /**
     * Assigns the method parameters to a local array for future invocation.
     *
     * @param sb The StringBuilder to which the generated code is appended.
     * @param dm The method whose parameters need to be assigned.
     */
    private static void assignParametersToArgs(final StringBuilder sb, final Method dm) {
        // Get the number of parameters in the method
        final int len = dm.getParameters().length;

        // Iterate through each parameter and assign it to the array
        for (int j = 0; j < len; j++) {
            String paramName = dm.getParameters()[j].getName();
            sb.append("    _a_[").append(j).append("] = ").append(paramName).append(";\n");
        }
    }

    /**
     * Creates a method signature for the given method.
     *
     * @param dm        The method for which the signature needs to be created.
     * @param returnType The return type of the method.
     * @return The method signature as a CharSequence.
     */
    private static CharSequence createMethodSignature(final Method dm, final Class<?> returnType) {
        // Get the number of parameters in the method
        final int len = dm.getParameters().length;
        final StringBuilder result = new StringBuilder();

        // Determine the return type name
        final String typeName = nameForClass(returnType);

        // Start constructing the method signature
        result.append("  public ").append(typeName).append(' ').append(dm.getName()).append('(');

        // Append each parameter to the method signature
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
