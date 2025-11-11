/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

@RunWith(value = Parameterized.class)
public class AbstractFieldTest extends WireTestCommon {

    // The specific WireType configuration to be used for each test.
    private final WireType wireType;

    // Constructor that sets the wireType for this test iteration.
    public AbstractFieldTest(WireType wireType) {
        this.wireType = wireType;
    }

    // Provide a collection of WireTypes for parameterized testing.
    @Parameterized.Parameters(name = "{0}")
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
    @Test
    public void abstractField() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        MSDMHolder holder = new MSDMHolder();
        holder.marshallable = new MySelfDescribingMarshallable("Hello World");

        final Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.getValueOut().object(MSDMHolder.class, holder);

        MSDMHolder result = wire.getValueIn().object(MSDMHolder.class);
        assertEquals(holder, result);
    }

    // Test serialization and deserialization of the abstract field in MSDMHolder2 class.
    @Test
    public void abstractField2() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        MSDMHolder2 holder = new MSDMHolder2();
        holder.marshallable = new MySelfDescribingMarshallable("Hello World");

        final Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.getValueOut().object(MSDMHolder2.class, holder);

        MSDMHolder2 result = wire.getValueIn().object(MSDMHolder2.class);
        assertEquals(holder, result);
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
        String text;

        MySelfDescribingMarshallable(String s) {
            text = s;
        }
    }
}
