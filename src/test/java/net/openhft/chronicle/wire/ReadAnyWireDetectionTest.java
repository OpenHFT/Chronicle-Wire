/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReadAnyWireDetectionTest extends WireTestCommon {

    @Test
    public void detectsWireTypeAndReadsPayload() {
        for (TestCase testCase : cases()) {
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

    private Collection<TestCase> cases() {
        return Arrays.asList(
                new TestCase(WireType.TEXT, "lorem ipsum", WireType.TEXT),
                new TestCase(WireType.BINARY, "binary-payload", WireType.BINARY)
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

    private static final class TestCase {
        final WireType type;
        final String payload;
        final WireType expectedType;

        private TestCase(WireType type, String payload, WireType expectedType) {
            this.type = type;
            this.payload = payload;
            this.expectedType = expectedType;
        }
    }
}
