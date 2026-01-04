/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// This test class aims to test the deserialization from "naked" files using different wire types.
public class DeserializeFromNakedFileTest extends WireTestCommon {

    // Parameterized setup to generate combinations of wire types for testing.
    public static Collection<Object[]> combinations() {
        Object[][] list = {
                {WireType.TEXT},
                {WireType.YAML}
        };
        return Arrays.asList(list);
    }

    // Test to verify the deserialization of a POJO from the "naked.yaml" file.
    @MethodSource("combinations")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Deserialises POJO from naked yaml file")
    public void testPOJO(WireType wireType) throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip naked file POJO test for wireType=" + wireType);
        PlainOldJavaClass res = wireType.fromFile(PlainOldJavaClass.class, "naked.yaml");

        // Validate if the deserialized object has the expected attribute value.
        assertEquals(20, res.heartBtInt,
                "POJO heartBtInt should match expected value for wireType=" + wireType);
    }

    // Test to verify the deserialization of a self-describing class from the "naked.yaml" file.
    @MethodSource("combinations")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Deserialises self describing class from naked yaml file")
    public void testSelfDescribing(WireType wireType) throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip naked file self describing test for wireType=" + wireType);
        SelfDescribingClass res = wireType.fromFile(SelfDescribingClass.class, "naked.yaml");

        // Validate if the deserialized object has the expected attribute value.
        assertEquals(20, res.heartBtInt,
                "Self-describing heartBtInt should match expected value for wireType=" + wireType);
    }

    // Test to verify the deserialization of a bytes class from the "naked.yaml" file.
    @MethodSource("combinations")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Deserialises bytes class from naked yaml file")
    public void testBytes(WireType wireType) throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip naked file bytes test for wireType=" + wireType);
        // Skip the test if the WireType is YAML.
        assumeFalse(wireType == WireType.YAML,
                "yaml wire does not support bytes class from naked.yaml, wireType=" + wireType);
        BytesClass res = wireType.fromFile(BytesClass.class, "naked.yaml");

        // Validate if the deserialized object has the expected byte representation.
        assertEquals(0x72616548, res.heartBtInt,
                "heartBtInt should match expected bytes value for wireType=" + wireType);
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
