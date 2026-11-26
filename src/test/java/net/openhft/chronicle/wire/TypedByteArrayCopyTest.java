/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class TypedByteArrayCopyTest extends WireTestCommon {
    @Test public void printableArrayToText() {
        copy("hello".getBytes(StandardCharsets.US_ASCII), WireType.TEXT);
    }

    @Test public void binaryArrayToText() {
        copy(new byte[]{0, 1, -1, -128, 65}, WireType.TEXT);
    }

    @Test public void emptyArrayToText() {
        copy(new byte[0], WireType.TEXT);
    }

    @Test public void printableArrayToBinary() {
        copy("hello".getBytes(StandardCharsets.US_ASCII), WireType.BINARY);
    }

    @Test public void binaryArrayToBinary() {
        copy(new byte[]{0, 1, -1, -128, 65}, WireType.BINARY);
    }

    @Test public void emptyArrayToBinary() {
        copy(new byte[0], WireType.BINARY);
    }

    @Test public void typedTextReusesMatchingArray() {
        copy(new byte[]{65, 66}, WireType.TEXT, new byte[2], true);
    }

    @Test public void typedTextReplacesShortArray() {
        copy(new byte[]{65, 66}, WireType.TEXT, new byte[1], false);
    }

    @Test public void typedTextReplacesLongArray() {
        copy(new byte[]{65, 66}, WireType.TEXT, new byte[3], false);
    }

    @Test public void typedTextReusesEmptyArray() {
        copy(new byte[0], WireType.TEXT, new byte[0], true);
    }

    private void copy(byte[] expected, WireType destination) {
        copy(expected, destination, null, false);
    }

    private void copy(byte[] expected, WireType destination, byte[] using, boolean reused) {
        Bytes<?> source = Bytes.allocateElasticOnHeap();
        Bytes<?> target = Bytes.allocateElasticOnHeap();
        try {
            Wire in = new BinaryWire(source);
            Wire out = destination.apply(target);
            in.write("value").object(Object.class, expected);
            in.write("tail").int32(7);
            long limit = source.readLimit();
            in.copyTo(out);
            byte[] actual = out.read("value").bytes(using);
            assertArrayEquals(expected, actual);
            if (reused)
                assertSame(using, actual);
            else
                assertNotSame(using, actual);
            assertEquals(7, out.read("tail").int32());
            assertEquals(limit, source.readLimit());
        } finally {
            target.releaseLast();
            source.releaseLast();
        }
    }
}
