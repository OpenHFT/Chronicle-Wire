/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

/**
 * Internal helper for writing sequences of method parameters into a given output format.
 *
 * <p>This class analyses a {@link Method}'s parameter types once and builds
 * {@link BiConsumer} instances that write the arguments to a
 * {@link ValueOut}. {@link #from0} writes all arguments while
 * {@link #from1} skips the first. The {@link MethodId} annotation is
 * cached in {@link #methodId} for efficient reuse.
 *
 * <p>This was previously implemented with a lambda which produced
 * garbage.
 */
class ParameterHolderSequenceWriter {

    // The cached parameter types of the target method
    @SuppressWarnings("rawtypes")
    final Class[] parameterTypes;

    // Serialises every argument sequence to the provided {@link ValueOut}
    final BiConsumer<Object[], ValueOut> from0;

    // Serialises every argument sequence to the provided {@link ValueOut} from index {@code 1} onwards
    final BiConsumer<Object[], ValueOut> from1;

    // MethodId value or {@code Long.MIN_VALUE} when absent
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
