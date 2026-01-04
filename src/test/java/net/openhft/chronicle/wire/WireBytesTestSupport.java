/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.internal.NoBytesStore;
import org.jetbrains.annotations.NotNull;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecation")
final class WireBytesTestSupport {
    private WireBytesTestSupport() {
    }

    static void exerciseBytesRoundTrip(Wire wire, BytesStore<?, ?> helloBytes, BytesStore<?, ?> quoteBytes, byte[] allBytes) {
        wire.write().bytes(NoBytesStore.NO_BYTES)
                .write().bytes(helloBytes)
                .write().bytes(quoteBytes)
                .write().bytes(allBytes);
    }

    static void assertBytesRoundTrip(Wire wire, byte[] allBytes, Bytes<?> target) {
        wire.read().bytes(b -> assertEquals(0, b.readRemaining(),
                        "Empty bytes field should have zero remaining"))
                .read().bytes(b -> assertEquals("Hello", b.toString(),
                        "Hello bytes should decode to Hello string"))
                .read().bytes(b -> assertEquals("quotable, text", b.toString(),
                        "Quoted text bytes should decode to string"))
                .read().bytes(target);
        assertEquals(Bytes.wrapForRead(allBytes), target,
                "Target bytes should match original payload");
    }

    static BytesStore<?, ?> helloBytes() {
        return Bytes.wrapForRead("Hello".getBytes(ISO_8859_1));
    }

    static BytesStore<?, ?> quoteBytes() {
        return Bytes.wrapForRead("quotable, text".getBytes(ISO_8859_1));
    }
}
