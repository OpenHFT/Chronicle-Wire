/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Invocation;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;

import java.lang.reflect.Method;

/**
 * Version of {@link MethodReaderInterceptorReturns} that lets generated
 * readers inline custom logic without reflection overhead.
 *
 * <p>Code returned by {@link #codeBeforeCall(Method, String, String[])} and
 * {@link #codeAfterCall(Method, String, String[])} is inserted before and after
 * the actual invocation in the generated reader.  The snippets may reference
 * the deserialised argument variables and the target object instance.
 *
 * <p>Simple example that skips the call of method "foo" when its second argument is null:
 * <pre>{@code
 *     public String codeBeforeCall(Method m, String objectName, String[] argumentNames) {
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
     * @return a unique string ID for this generator implementation.
     */
    String generatorId();

    /**
     * @param m             Calling method.
     * @param objectName    Object instance name.
     * @param argumentNames Call argument names.
     * @return Source code to add before the method call.
     */
    String codeBeforeCall(Method m, String objectName, String[] argumentNames);

    /**
     * @param m             Calling method.
     * @param objectName    Object instance name.
     * @param argumentNames Call argument names.
     * @return Source code to add after the method call.
     */
    String codeAfterCall(Method m, String objectName, String[] argumentNames);
}
