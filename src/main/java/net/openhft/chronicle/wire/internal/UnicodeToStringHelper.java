/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.VanillaBytes;
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;

import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;

/**
 * Helper class to convert a stream of bytes to a UTF-8 encoded string.
 * Heavily based on the approach used in {@link VanillaBytes#toString()} but with UTF-8 support.
 * This is used selectively rather than everywhere because some parts of whire use 8-bit encoding of strings based on
 * ISO-8859-1.
 */
public final class UnicodeToStringHelper {

    private UnicodeToStringHelper() {
    }

    /**
     * Represent the bytes store as a UTF-8 string.
     *
     * @return UTF-8 string representation of the bytes store.
     */
    public static String toUnicodeString(BytesStore<?, ?> bytesStore) {
        try {
            try {
                return bytesStore instanceof NativeBytesStore
                        ? toStringNativeBytes((NativeBytesStore<?>) bytesStore)
                        : toStringBytesStore(bytesStore);
            } catch (IllegalStateException e) {
                throw Jvm.rethrow(e);
            }
        } catch (Exception e) {
            return e.toString();
        }
    }

    private static String toStringNativeBytes(NativeBytesStore<?> bytesStore) {
        final Memory memory = bytesStore.memory;
        int length = (int)
                Math.min(Bytes.MAX_HEAP_CAPACITY, bytesStore.realReadRemaining());
        byte[] bytes = new byte[length];
        final long address = bytesStore.address + bytesStore.translate(bytesStore.readPosition());
        for (int i = 0; i < length && i < bytesStore.realCapacity(); i++) {
            bytes[i] = memory.readByte(address + i);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String toStringBytesStore(BytesStore<?, ?> bytesStore)
            throws ClosedIllegalStateException {
        int length = (int) Math.min(Bytes.MAX_HEAP_CAPACITY, bytesStore.readRemaining());
        byte[] bytes = new byte[length];
        try {
            for (int i = 0; i < length; i++) {
                bytes[i] = (bytesStore.readByte(bytesStore.readPosition() + i));
            }
        } catch (BufferUnderflowException e) {
            // ignored
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
