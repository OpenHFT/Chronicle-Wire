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
 * <p>This mechanism is used by {@link GenerateMethodReader} to weave logic
 * directly into the generated class.  If the supplied code fails to compile,
 * the call falls back to {@link #intercept(Method, Object, Object[], Invocation)}
 * as it would with a reflective interceptor.
 */
public interface GeneratingMethodReaderInterceptorReturns extends MethodReaderInterceptorReturns {
    /**
     * Specifies ID of this generating interceptor.<br>
     * Contract: if the code provided by generating interceptor differs from the code provided by another generating
     * interceptor, theirs IDs should be different as well.
     * Provided ID will be used in the classname of a generated method reader to ensure re-compilation when a new
     * generator is passed.
     *
     * @return a unique string ID for this generator implementation. This ID influences the generated class name.
     */
    String generatorId();

    /**
     * @param m             the {@link Method} being intercepted
     * @param objectName    name of the variable holding the target instance in the generated code
     * @param argumentNames variable names of the deserialised arguments in the generated code
     * @return Java source code to insert before the actual call, or {@code null} if nothing should be added
     */
    String codeBeforeCall(Method m, String objectName, String[] argumentNames);

    /**
     * @param m             the {@link Method} being intercepted
     * @param objectName    name of the variable holding the target instance in the generated code
     * @param argumentNames variable names of the deserialised arguments in the generated code
     * @return Java source code to insert after the actual call, or {@code null} if nothing should be added
     */
    String codeAfterCall(Method m, String objectName, String[] argumentNames);
}
