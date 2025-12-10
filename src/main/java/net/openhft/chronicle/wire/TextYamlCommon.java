/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.PointerBytesStore;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

final class TextYamlCommon {
    private TextYamlCommon() {
    }

    static DocumentContext readDocument(WireOut owner,
                                        ReadDocumentContext readContext,
                                        long readLocation) {
        final long readPosition = owner.bytes().readPosition();
        final long readLimit = owner.bytes().readLimit();
        owner.bytes().readPosition(readLocation);
        owner.initReadContext();
        readContext.closeReadLimit(readLimit);
        readContext.closeReadPosition(readPosition);
        return readContext;
    }

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

    static WireIn bytesCommon(WireIn owner, @NotNull BytesOut<?> toBytes) {
        toBytes.clear();
        return owner.bytes(b -> toBytes.write((BytesStore) b));
    }

    static WireIn bytesSetCommon(WireIn owner, @NotNull PointerBytesStore toBytes) {
        return owner.bytes(bytes -> {
            long capacity = bytes.readRemaining();
            Bytes<Void> bytes2 = Bytes.allocateDirect(capacity);
            bytes2.write((BytesStore) bytes);
            toBytes.set(bytes2.addressForRead(bytes2.start()), capacity);
        });
    }

    static WireIn bytesMarshallableCommon(WireIn owner,
                                          @NotNull ReadBytesMarshallable bytesConsumer) {
        owner.consumePadding();
        bytesConsumer.readMarshallable(owner.bytes());
        return owner;
    }

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
