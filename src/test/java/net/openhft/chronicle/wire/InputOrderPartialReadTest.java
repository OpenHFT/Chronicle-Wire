/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import static org.junit.Assert.*;

import net.openhft.chronicle.bytes.Bytes;

import org.junit.Test;

public class InputOrderPartialReadTest extends WireTestCommon {

    public static class Value extends SelfDescribingMarshallable {
        public int x = 10, y = 20;
        public String name = "default";
    }

    private Value read(String input, boolean overwrite) {
        Value value = new Value();
        value.x = 1;
        value.y = 2;
        value.name = "retained";
        Bytes<?> bytes = Bytes.from(input);
        try {
            WireMarshaller.of(Value.class)
                    .readMarshallableInputOrder(value, new TextWire(bytes), overwrite);
            return value;
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void partialInOrderReadKeepsAbsentFields() throws Exception {
        Value value = read("x: 7\n", false);
        assertEquals(7, value.x);
        assertEquals(2, value.y);
        assertEquals("retained", value.name);
    }

    @Test
    public void partialOutOfOrderReadKeepsAbsentFields() throws Exception {
        Value value = read("y: 7\n", false);
        assertEquals(1, value.x);
        assertEquals(7, value.y);
        assertEquals("retained", value.name);
    }

    @Test
    public void unknownPrefixDoesNotResetExistingFields() throws Exception {
        Value value = read("extra: { nested: 3 }\ny: 7\n", false);
        assertEquals(1, value.x);
        assertEquals(7, value.y);
        assertEquals("retained", value.name);
    }

    @Test
    public void reorderedFieldsRemainCaseInsensitive() throws Exception {
        Value value = read("Y: 7\nX: 8\n", true);
        assertEquals(8, value.x);
        assertEquals(7, value.y);
        assertEquals("default", value.name);
    }

    @Test
    public void overwriteTrueRestoresAbsentDefaults() throws Exception {
        Value value = read("y: 7\n", true);
        assertEquals(10, value.x);
        assertEquals(7, value.y);
        assertEquals("default", value.name);
    }

    @Test
    public void unnamedEntryInNamedFallbackDoesNotOverwriteFirstField() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = new BinaryWire(bytes);
            wire.write("y").int32(7);
            wire.write("").int32(42);
            Value value = new Value();
            WireMarshaller.of(Value.class).readMarshallableInputOrder(value, wire, true);
            assertEquals(10, value.x);
            assertEquals(7, value.y);
        } finally {
            bytes.releaseLast();
        }
    }
}
