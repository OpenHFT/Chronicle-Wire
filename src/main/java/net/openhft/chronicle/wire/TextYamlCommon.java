/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.ref.TextLongArrayReference;
import net.openhft.chronicle.core.values.LongArrayValues;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Shared text/YAML helpers for handling large byte sequences and int64 array marshalling.
 */
final class TextYamlCommon {
    /**
     * Utility holder; not instantiable.
     */
    private TextYamlCommon() {
    }

    /**
     * Produces a string view of {@code bytes}, truncating to 1MB to avoid giant log output while
     * preserving the original read limit.
     */
    static String largeToString(Bytes<?> bytes) {
        if (bytes.readRemaining() > (1024 * 1024)) {
            final long l = bytes.readLimit();
            try {
                bytes.readLimit(bytes.readPosition() + (1024 * 1024));
                return bytes + "..";
            } finally {
                bytes.readLimit(l);
            }
        } else {
            return bytes.toString();
        }
    }

    /**
     * Common logic for reading an int64 array from text wire into a {@link LongArrayValues}, wiring
     * the provided target via the supplied setter. Reuses a {@link TextLongArrayReference} when
     * possible to avoid allocations.
     */
    static <T> WireIn int64arrayCommon(Bytes<?> bytes,
                                       @Nullable LongArrayValues values,
                                       T target,
                                       @NotNull BiConsumer<T, LongArrayValues> setter) {
        if (!(values instanceof TextLongArrayReference)) {
            values = new TextLongArrayReference();
        }
        @NotNull Byteable b = (Byteable) values;
        long length = TextLongArrayReference.peakLength(bytes, bytes.readPosition());
        b.bytesStore(bytes, bytes.readPosition(), length);
        bytes.readSkip(length);
        setter.accept(target, values);
        return (WireIn) target;
    }
}
