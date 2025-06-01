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

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

/**
 * Internal helper for generated method writers.
 *
 * <p>This class analyses a {@link Method}'s parameter types once and builds
 * {@link BiConsumer} instances that write the arguments to a
 * {@link ValueOut}. {@link #from0} writes all arguments while
 * {@link #from1} skips the first. The {@link MethodId} annotation is
 * cached in {@link #methodId} for efficient reuse.
 *
 * <p>This was previously implemented with a lambda which produced
 * garbage.
 *
 * @implNote For internal use only and not part of the public API.
 */
class ParameterHolderSequenceWriter {

    /** The cached parameter types of the target method. */
    @SuppressWarnings("rawtypes")
    final Class[] parameterTypes;

    /** Serialises every argument to the provided {@link ValueOut}. */
    final BiConsumer<Object[], ValueOut> from0;

    /** Serialises arguments from index {@code 1} onwards. */
    final BiConsumer<Object[], ValueOut> from1;

    /** MethodId value or {@code Long.MIN_VALUE} when absent. */
    final long methodId;

    /**
     * Creates a writer for the given method.
     * <p>
     * Parameter types are cached and the consumer functions are built.
     * The {@link MethodId} value is read if present.
     *
     * @param method the method to analyse
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
