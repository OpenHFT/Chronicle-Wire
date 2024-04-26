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

import net.openhft.chronicle.wire.utils.SourceCodeFormatter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is the GenerateMethodDelegate class, extending AbstractClassGenerator with GMDMetaData.
 * The class is responsible for generating method delegates using the provided metadata.
 */
@Deprecated(/* to be moved to services in x.27 */)
public class GenerateMethodDelegate extends AbstractClassGenerator<GenerateMethodDelegate.GMDMetaData> {
    public GenerateMethodDelegate() {
        super(new GMDMetaData());
    }

    @Override
    public synchronized Class acquireClass(ClassLoader classLoader) {
        metaData().interfaces().add(MethodDelegate.class);
        return super.acquireClass(classLoader);
    }

    @Override
    protected String generateGenericType() {
        return "OUT extends Object & " + metaData().interfaces().stream()
                .map(this::nameForClass)
                .map(s -> s.equals("MethodDelegate") ? "MethodDelegate<OUT>" : s)
                .collect(Collectors.joining(" & "));
    }

    @Override
    protected void generateFields(SourceCodeFormatter mainCode) {
        mainCode.append("private ").append(getDelegateType()).append(" delegate;\n");
    }

    protected String getDelegateType() {
        return "OUT";
    }

    @Override
    protected void generateConstructors(SourceCodeFormatter mainCode) {
    }

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

    @Override
    protected void generateMethod(Method method, StringBuilder params, List<String> paramList, SourceCodeFormatter mainCode) {
        if (method.getReturnType() != void.class)
            mainCode.append("return ");
        getDelegate(mainCode, method)
                .append(".").append(method.getName()).append("(").append(params).append(");\n");
    }

    /**
     * Appends the delegate to the main code.
     *
     * @param mainCode The SourceCodeFormatter to append the delegate.
     * @param method The associated method.
     * @return Updated SourceCodeFormatter with delegate appended.
     */
    protected SourceCodeFormatter getDelegate(SourceCodeFormatter mainCode, Method method) {
        return mainCode.append("this.delegate");
    }

    /**
     * This is an inner static class representing metadata for GenerateMethodDelegate.
     */
    public static class GMDMetaData extends AbstractClassGenerator.MetaData<GMDMetaData> {
        // This class serves as a metadata container with no additional methods or attributes.
    }
}
