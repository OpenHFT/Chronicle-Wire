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

import net.openhft.chronicle.wire.utils.SourceCodeFormatter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dynamically generates a class that implements {@link MethodDelegate} and the
 * additional interfaces supplied in {@link GMDMetaData#interfaces()}. Calls to
 * those interfaces are forwarded to a delegate instance set via
 * {@link MethodDelegate#delegate(Object)}.
 * <p>
 * The generated class allows the delegate to be swapped or wrapped at run time,
 * providing a lightweight means of adapting behaviour. This generator extends
 * {@link AbstractClassGenerator} and produces a typed wrapper that forwards
 * every method invocation to the current delegate.
 */
public class GenerateMethodDelegate extends AbstractClassGenerator<GenerateMethodDelegate.GMDMetaData> {
    /**
     * Creates a generator with the default metadata.
     */
    public GenerateMethodDelegate() {
        super(new GMDMetaData());
    }

    /**
     * Ensures {@link MethodDelegate} is always included before delegating to the
     * superclass to create or load the proxy class.
     */
    @Override
    public synchronized <T> Class<T> acquireClass(ClassLoader classLoader) {
        metaData().interfaces().add(MethodDelegate.class);
        return super.acquireClass(classLoader);
    }

    /**
     * Creates the generic type signature in the form
     * {@code <OUT extends Object & Interface1 & Interface2 & MethodDelegate<OUT>>}.
     */
    @Override
    protected String generateGenericType() {
        return "OUT extends Object & " + metaData().interfaces().stream()
                .map(this::nameForClass)
                .map(s -> s.equals("MethodDelegate") ? "MethodDelegate<OUT>" : s)
                .collect(Collectors.joining(" & "));
    }

    /**
     * Emits the field that stores the current delegate instance.
     */
    @Override
    protected void generateFields(SourceCodeFormatter mainCode) {
        mainCode.append("private ").append(getDelegateType()).append(" delegate;\n");
    }

    /**
     * Returns the name used for the delegate type in generated code.
     */
    protected String getDelegateType() {
        return "OUT";
    }

    /**
     * Uses the implicit no-arg constructor; no constructors are generated.
     */
    @Override
    protected void generateConstructors(SourceCodeFormatter mainCode) {
    }

    /**
     * Generates a concrete implementation of each method. The
     * {@code delegate(Object)} method stores the delegate, while all other
     * methods are handled by the superclass.
     */
    @Override
    protected void generateMethod(Method method, SourceCodeFormatter mainCode) {
        String s = method.toString();
        if (s.equals("public abstract void net.openhft.chronicle.wire.MethodDelegate.delegate(java.lang.Object)")) {
            withLineNumber(mainCode)
                    .append("public void delegate(Object delegate) {\n" +
                            "this.delegate = (").append(getDelegateType()).append(") delegate;\n" +
                            "}\n");
        } else {
            super.generateMethod(method, mainCode);
        }
    }

    /**
     * Writes the body of a delegated method. The parameters and their list are
     * passed in so the invocation can be constructed correctly.
     */
    @Override
    protected void generateMethod(Method method, StringBuilder params, List<String> paramList, SourceCodeFormatter mainCode) {
        if (method.getReturnType() != void.class)
            mainCode.append("return ");
        getDelegate(mainCode, method)
                .append(".").append(method.getName()).append("(").append(params).append(");\n");
    }

    /**
     * Appends the expression used to access the current delegate.
     */
    protected SourceCodeFormatter getDelegate(SourceCodeFormatter mainCode, Method method) {
        return mainCode.append("this.delegate");
    }

    /**
     * Metadata for {@link GenerateMethodDelegate}. It currently mirrors
     * {@link AbstractClassGenerator.MetaData} without extra fields.
     */
    public static class GMDMetaData extends AbstractClassGenerator.MetaData<GMDMetaData> {
        // Intentionally left blank
    }
}
