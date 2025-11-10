//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * Functional interface describing a strategy for deserialising data.
 * <p>
 * Implementations convert a {@link ValueIn} representation to an object of type {@code T}.
 * This allows custom logic to be supplied wherever a {@code ValueIn} needs to be turned into a concrete object.
 *
 * @param <T> the type produced after parsing the input value
 */
@FunctionalInterface
public interface MarshallableParser<T> {

    /**
     * Parses the provided {@code ValueIn} into an instance of type {@code T}.
     *
     * @param valueIn the {@link ValueIn} holding the serialised form
     * @return non-null instance of {@code T} deserialised from {@code valueIn}
     */
    @NotNull
    T parse(ValueIn valueIn);
}
