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

import net.openhft.chronicle.bytes.Invocation;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;

import java.lang.reflect.Method;

/**
 * <p>This interface is a version of method reader interceptor that allows to change the logic of
 * {@link net.openhft.chronicle.bytes.MethodReader} without overhead provided by reflexive calls.
 *
 * <p>Code returned by {@link #codeBeforeCall(Method, String, String[])} and
 * {@link #codeAfterCall(Method, String, String[])} will be added before and after actual method call in the generated
 * source code of the method reader. It's possible to use original call arguments and object instance in the added code.
 *
 * <p>Simple example that allows to skip call of method "foo" in case its second argument is null:
 * <pre>{@code
 *     public String codeBeforeCall(Method m, String objectName, String[] argumentNames, String serviceName) {
 *         if (m.getName().equals("foo"))
 *             return "if (" + argumentNames[1] + " != null) {";
 *         else
 *             return "";
 *     }
 *
 *     public String codeAfterCall(Method m, String objectName, String[] argumentNames) {
 *         if (m.getName().equals("foo"))
 *             return "}";
 *         else
 *             return "";
 *     }
 * }</pre>
 *
 * <p>Please mind that if provided code fails to compile, reflexive method call will be delegated to the interceptor
 * with {@link #intercept(Method, Object, Object[], Invocation)}, like it happens with a regular
 * {@link MethodReaderInterceptorReturns}.
 */
public interface GeneratingMethodReaderInterceptorReturns extends MethodReaderInterceptorReturns {
    /**
     * Specifies ID of this generating interceptor.<br>
     * Contract: if the code provided by generating interceptor differs from the code provided by another generating
     * interceptor, theirs IDs should be different as well.
     * Provided ID will be used in the classname of a generated method reader to ensure re-compilation when a new
     * generator is passed.
     *
     * @return ID of this generator.
     */
    String generatorId();

    /**
     * @param m             Calling method.
     * @param objectName    Object instance name.
     * @param argumentNames Call argument names.
     * @return Source code to add before the method call.
     * @deprecated use codeBeforeCall(java.lang.reflect.Method, java.lang.String, java.lang.String[], java.lang.String)
     */
    @Deprecated
    default String codeBeforeCall(Method m, String objectName, String[] argumentNames) {
        return codeBeforeCall(m, objectName, argumentNames, "");
    }

    /**
     * @param m             Calling method.
     * @param objectName    Object instance name.
     * @param argumentNames Call argument names.
     * @param serviceName   the name of the service
     * @return Source code to add before the method call.
     */
    default String codeBeforeCall(Method m, String objectName, String[] argumentNames, String serviceName) {
        return codeBeforeCall(m, objectName, argumentNames);
    }

    /**
     * @param m             Calling method.
     * @param objectName    Object instance name.
     * @param argumentNames Call argument names.
     * @return Source code to add after the method call.
     */
    String codeAfterCall(Method m, String objectName, String[] argumentNames);
}
