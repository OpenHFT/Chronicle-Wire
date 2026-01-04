/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class AbstractFieldTest extends WireTestCommon {

    // Provide a collection of WireTypes for parameterized testing.
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{WireType.BINARY},
                new Object[]{WireType.BINARY_LIGHT},
                new Object[]{WireType.TEXT},
                new Object[]{WireType.YAML},
                new Object[]{WireType.YAML_ONLY},
                new Object[]{WireType.JSON_ONLY}
        );
    }

    // Test serialization and deserialization of the abstract field in MSDMHolder class.
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Serialises abstract field with self describing type")
    void abstractField(WireType wireType) {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip abstract field test for MSDMHolder");

        MSDMHolder holder = new MSDMHolder();
        holder.marshallable = new MySelfDescribingMarshallable("Hello World");

        final Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.getValueOut().object(MSDMHolder.class, holder);

        MSDMHolder result = wire.getValueIn().object(MSDMHolder.class);
        assertEquals(holder, result, "Expected MSDMHolder to round-trip for wireType=" + wireType);
        assertNotNull(result.marshallable,
                "MSDMHolder marshallable field should be present for wireType=" + wireType);
        assertEquals("Hello World",
                ((MySelfDescribingMarshallable) result.marshallable).text,
                "MSDMHolder marshallable text should round-trip for wireType=" + wireType);
    }

    // Test serialization and deserialization of the abstract field in MSDMHolder2 class.
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Serialises concrete field with self describing type")
    void abstractField2(WireType wireType) {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip abstract field test for MSDMHolder2");

        MSDMHolder2 holder = new MSDMHolder2();
        holder.marshallable = new MySelfDescribingMarshallable("Hello World");

        final Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.getValueOut().object(MSDMHolder2.class, holder);

        MSDMHolder2 result = wire.getValueIn().object(MSDMHolder2.class);
        assertEquals(holder, result, "Expected MSDMHolder2 to round-trip for wireType=" + wireType);
        assertNotNull(result.marshallable,
                "MSDMHolder2 marshallable field should be present for wireType=" + wireType);
        assertEquals("Hello World",
                result.marshallable.text,
                "MSDMHolder2 marshallable text should round-trip for wireType=" + wireType);
    }

    // Holder class to test serialization and deserialization with an abstract field.
    static class MSDMHolder extends SelfDescribingMarshallable {
        SelfDescribingMarshallable marshallable;
    }

    // Second holder class to test serialization and deserialization with a specific MySelfDescribingMarshallable field.
    static class MSDMHolder2 extends SelfDescribingMarshallable {
        MySelfDescribingMarshallable marshallable;
    }

    // Custom Marshallable class for testing purposes.
    static class MySelfDescribingMarshallable extends SelfDescribingMarshallable {
        final String text;

        MySelfDescribingMarshallable(String s) {
            text = s;
        }
    }
}
