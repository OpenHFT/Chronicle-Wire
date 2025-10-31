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

import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dynamically creates a class that implements a destination interface and
 * forwards method calls to a collection of handler objects. Each handler may
 * only implement a subset of the interface methods. When a method on the
 * generated bridge is invoked the call is dispatched to every handler that
 * declares a matching method. This allows one interface to be implemented by a
 * composition of several partial implementations, or for multiple listeners to
 * react to the same call.
 */
public class GenerateMethodBridge extends AbstractClassGenerator<GenerateMethodBridge.MethodBridgeMetaData> {

    /**
     * Generated field names for the handler instances. Each name corresponds
     * to one entry in {@link MethodBridgeMetaData#invokes} and is used when
     * emitting the bridge source code.
     */
    private List<String> fnameList;

    /**
     * Constructs a new instance with a fresh {@link MethodBridgeMetaData}.
     */
    public GenerateMethodBridge() {
        super(new MethodBridgeMetaData());
    }

    /**
     * Builds and instantiates a method bridge for the supplied interface.
     *
     * @param destType  the primary interface the bridge will implement
     * @param toInvoke  handler instances to receive forwarded calls
     * @param ui        optional {@link UpdateInterceptor} applied to bridge invocations
     * @return a new instance of the generated bridge class
     */
    public static Object bridgeFor(Class<?> destType, List<Object> toInvoke, UpdateInterceptor ui) {
        GenerateMethodBridge gmb = new GenerateMethodBridge();
        MethodBridgeMetaData md = gmb.metaData();

        // Set metadata properties based on the destination type and the objects to be invoked.
        md.packageName(Jvm.getPackageName(destType));
        md.baseClassName(destType.getSimpleName());
        md.invokes(toInvoke.stream().map(o -> findClass(o)).collect(Collectors.toList()));
        md.interfaces().add(destType);
        md.useUpdateInterceptor(ui != null);

        // Acquire the class using the destination type's class loader.
        Class<?> aClass = gmb.acquireClass(destType.getClassLoader());
        try {
            // Instantiate the acquired class based on the presence of the UpdateInterceptor.
            return ui == null
                    ? aClass.getConstructor(List.class).newInstance(toInvoke)
                    : aClass.getConstructor(List.class, UpdateInterceptor.class).newInstance(toInvoke, ui);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Determines the most suitable type for declaring the handler field.
     * Returns the first interface implemented by {@code o} if present,
     * otherwise {@code o.getClass()}.
     */
    private static Class<?> findClass(Object o) {
        Class<?> aClass = o.getClass();
        Class<?>[] interfaces = aClass.getInterfaces();
        if (interfaces.length > 0)
            return interfaces[0];
        return aClass;
    }

    /**
     * Emits field declarations for all handler objects defined in the
     * metadata. Field names are derived via {@link #fieldCase(Class)} and
     * made unique if necessary.
     */
    @Override
    protected void generateFields(SourceCodeFormatter mainCode) {
        MethodBridgeMetaData md = metaData();
        List<Class<?>> handlers = md.invokes;
        fnameList = new ArrayList<>();
        for (int i = 0; i < handlers.size(); i++) {
            Class<?> handler = handlers.get(i);
            String fname = fieldCase(handler);
            if (fnameList.contains(fname))
                fname += fnameList.size();
            fnameList.add(fname);
            if (i == 0)
                withLineNumber(mainCode);
            mainCode.append("private final ").append(nameForClass(handler)).append(' ').append(fname).append(";\n");
        }
    }

    /**
     * Generates the bridge constructor which initialises all handler fields and
     * optionally stores an {@link UpdateInterceptor}.
     */
    @Override
    protected void generateConstructors(SourceCodeFormatter mainCode) {
        MethodBridgeMetaData md = metaData();
        withLineNumber(mainCode)
                .append("public ").append(className()).append("(").append(nameForClass(List.class)).append(" handlers");
        if (md.useUpdateInterceptor())
            mainCode.append(", ").append(nameForClass(UpdateInterceptor.class)).append(" updateInterceptor");

        mainCode.append(") {\n");
        List<Class<?>> handlers = metaData().invokes;
        for (int i = 0; i < handlers.size(); i++) {
            Class<?> handler = handlers.get(i);
            mainCode.append("this.").append(fnameList.get(i)).append(" = (").append(nameForClass(handler)).append(") handlers.get(").append(i).append(");\n");
        }
        if (md.useUpdateInterceptor())
            mainCode.append("this.updateInterceptor = updateInterceptor;\n");
        mainCode.append("}\n");
    }

    /**
     * This method is responsible for generating the code for a given method.
     * The method checks if the provided method exists in each of the handler classes from the metaData.
     * If the method exists in a handler class, the corresponding code is generated and appended to the mainCode.
     *
     * @param method     The method for which the code needs to be generated.
     * @param params     The parameters of the method, formatted as a StringBuilder.
     * @param paramList  The list of parameters of the method.
     * @param mainCode   The SourceCodeFormatter where the generated code will be appended.
     */
    protected void generateMethod(Method method, StringBuilder params, List<String> paramList, SourceCodeFormatter mainCode) {
        MethodBridgeMetaData md = metaData();
        String name = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();

        List<Class<?>> handlers = md.invokes;
        boolean first = true;
        for (int i = 0; i < handlers.size(); i++) {
            Class<?> handler = handlers.get(i);
            String fname = fnameList.get(i);
            try {
                handler.getMethod(name, parameterTypes);
                if (first)
                    withLineNumber(mainCode);
                first = false;
                mainCode.append("this.").append(fname).append(".").append(name).append("(").append(params).append(");\n");
            } catch (NoSuchMethodException e) {
                // Handler does not implement this method; skip to the next candidate.
                continue;
            }
        }
    }

    /**
     * This is the MethodBridgeMetaData inner class, extending AbstractClassGenerator.MetaData with MethodBridgeMetaData type.
     * This class encapsulates metadata related to the method bridge generation, particularly holding a list of classes
     * (handlers) that the bridge method may invoke.
     */
    static final class MethodBridgeMetaData extends AbstractClassGenerator.MetaData<MethodBridgeMetaData> {

        // List containing classes that the bridge method may invoke.
        private List<Class<?>> invokes = new ArrayList<>();

        /**
         * Returns the handler types.
         */
        public List<Class<?>> invokes() {
            return invokes;
        }

        /**
         * Setter for the invokes list.
         * This method sets the provided list of classes to the invokes list and returns the current instance.
         * It follows the builder pattern for chaining method calls.
         *
         * @param handlers list of classes representing the handlers
         * @return {@code this} for chaining
         */
        public MethodBridgeMetaData invokes(List<Class<?>> handlers) {
            this.invokes = handlers;
            return this;
        }
    }
}
