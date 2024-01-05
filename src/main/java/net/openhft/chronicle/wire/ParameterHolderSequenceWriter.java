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

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

// lambda was causing garbage
/**
 * Represents a utility class responsible for writing sequences of method parameters into a given output format.
 * Specifically, it encapsulates the logic to serialize method parameters into a wire format or similar output stream.
 * The class is designed with the assumption that the sequence will either start from the first parameter or
 * skip the first parameter and start from the second, as dictated by the `from0` and `from1` `BiConsumer` respectively.
 */
class ParameterHolderSequenceWriter {

    /**
     * Represents the parameter types of a method.
     */
    @SuppressWarnings("rawtypes")
    final Class[] parameterTypes;

    /**
     * A consumer that writes the entire sequence of parameters into a given output format.
     */
    final BiConsumer<Object[], ValueOut> from0;

    /**
     * A consumer that writes the sequence of parameters into a given output format,
     * starting from the second parameter.
     */
    final BiConsumer<Object[], ValueOut> from1;

    /**
     * Represents the method ID associated with the method.
     * It's useful for identifying the method in serialized data.
     */
    final long methodId;

    /**
     * Initializes the `ParameterHolderSequenceWriter` with the provided method. The method's parameters are extracted,
     * and appropriate serialization consumers (`from0` and `from1`) are initialized based on the parameters.
     *
     * @param method The method whose parameters are to be serialized.
     */
    @SuppressWarnings("unchecked")
    protected ParameterHolderSequenceWriter(Method method) {
        this.parameterTypes = method.getParameterTypes();
        this.from0 = (a, v) -> {
            for (int i = 0; i < parameterTypes.length; i++)
                v.object(parameterTypes[i], a[i]);
        };
        this.from1 = (a, v) -> {
            for (int i = 1; i < parameterTypes.length; i++)
                v.object(parameterTypes[i], a[i]);
        };
        MethodId methodId = Jvm.findAnnotation(method, MethodId.class);
        this.methodId = methodId == null ? Long.MIN_VALUE : methodId.value();
    }
}
