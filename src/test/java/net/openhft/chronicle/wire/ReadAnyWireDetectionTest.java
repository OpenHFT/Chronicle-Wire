/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"deprecation", "removal"})
public class ReadAnyWireDetectionTest extends WireTestCommon {

    @Test
    public void detectsWireTypeAndReadsPayload() {
        for (WireCase testCase : cases()) {
            Bytes<?> encoded = encode(testCase.type, wire -> {
                try (DocumentContext dc = wire.writingDocument(false)) {
                    dc.wire().write("msg").text(testCase.payload);
                }
            });
            ReadAnyWire readAnyWire = new ReadAnyWire(encoded);
            WireType detected;
            try (DocumentContext dc = readAnyWire.readingDocument()) {
                assertTrue(dc.isPresent());
                assertEquals(testCase.payload, dc.wire().read("msg").text());
                detected = readAnyWire.underlyingType().get();
            }
            assertEquals(testCase.expectedType, detected);
            encoded.releaseLast();
        }
    }

    private Collection<WireCase> cases() {
        return Arrays.asList(
                new WireCase(WireType.TEXT, "lorem ipsum", WireType.TEXT),
                new WireCase(WireType.BINARY, "binary-payload", WireType.BINARY)
        );
    }

    private Bytes<?> encode(WireType type, Consumer<Wire> writer) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
        Wire wire = type.apply(bytes);
        writer.accept(wire);
        Bytes<?> copy = Bytes.wrapForRead(bytes.toByteArray());
        bytes.releaseLast();
        copy.readPositionRemaining(0, copy.writePosition());
        return copy;
    }

    private static final class WireCase {
        final WireType type;
        final String payload;
        final WireType expectedType;

        private WireCase(WireType type, String payload, WireType expectedType) {
            this.type = type;
            this.payload = payload;
            this.expectedType = expectedType;
        }
    }
}
