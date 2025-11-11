/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.core.Jvm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

// This test class aims to test the deserialization from "naked" files using different wire types.
@RunWith(value = Parameterized.class)
public class DeserializeFromNakedFileTest extends WireTestCommon {

    // WireType instance to be used for the deserialization test.
    private final WireType wireType;

    // Constructor to initialize the WireType instance.
    public DeserializeFromNakedFileTest(WireType wireType) {
        this.wireType = wireType;
    }

    // Parameterized setup to generate combinations of wire types for testing.
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> combinations() {
        Object[][] list = {
                {WireType.TEXT},
                {WireType.YAML}
        };
        return Arrays.asList(list);
    }

    // Test to verify the deserialization of a POJO from the "naked.yaml" file.
    @Test
    public void testPOJO() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        PlainOldJavaClass res = wireType.fromFile(PlainOldJavaClass.class, "naked.yaml");

        // Validate if the deserialized object has the expected attribute value.
        assertEquals(20, res.heartBtInt);
    }

    // Test to verify the deserialization of a self-describing class from the "naked.yaml" file.
    @Test
    public void testSelfDescribing() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        SelfDescribingClass res = wireType.fromFile(SelfDescribingClass.class, "naked.yaml");

        // Validate if the deserialized object has the expected attribute value.
        assertEquals(20, res.heartBtInt);
    }

    // Test to verify the deserialization of a bytes class from the "naked.yaml" file.
    @Test
    public void testBytes() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        // Skip the test if the WireType is YAML.
        assumeFalse(wireType == WireType.YAML);
        BytesClass res = wireType.fromFile(BytesClass.class, "naked.yaml");

        // Validate if the deserialized object has the expected byte representation.
        assertEquals(0x72616548, res.heartBtInt);
    }

    // Plain old Java class used for the deserialization test.
    private static class PlainOldJavaClass {
        int heartBtInt;
    }

    // Self-describing class that extends SelfDescribingMarshallable for the deserialization test.
    private static class SelfDescribingClass extends SelfDescribingMarshallable {
        int heartBtInt;
    }

    // Bytes class that implements BytesMarshallable for the deserialization test.
    private static class BytesClass implements BytesMarshallable {
        int heartBtInt;
    }
}
