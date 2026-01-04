/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Test class to validate behaviours associated with class aliases in the context of Wire.
 * This test extends the WireTestCommon for utility behaviours related to wire tests.
 */
@SuppressWarnings({"deprecation", "removal"})
class Issue277Test extends WireTestCommon {

    /**
     * Sets up the testing environment before executing the test methods.
     * It specifically adds class aliases to the ClassAliasPool.
     */
    @BeforeEach
    void setup() {
        // Add class aliases for Data1 and Data2 to the ClassAliasPool
        ClassAliasPool.CLASS_ALIASES.addAlias(Data1.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(Data2.class);
    }

    // Sample data in string format to be used for deserialisation tests
    private static final String data = "!Data1 {\n" +
            "  name: Tom,\n" +
            "  age: 25,\n" +
            "  address: \"21 high street, Liverpool\"\n" +
            "}\n";

    /**
     * Validates that the data can be correctly parsed into a Data2 object.
     * This test does not expect a RuntimeException because a correct class alias is provided.
     */
    @Test
    @DisplayName("Class alias should parse Data2 text")
    void isOk() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for alias parsing test");

        // Deserialize the sample data into a Data2 object without throwing an exception
        Data2 o2 = WireType.TEXT.fromString(Data2.class, data);

        // Assert that the deserialized Data2 object matches the expected string representation
        assertEquals("!Data2 {\n" +
                "  name: Tom,\n" +
                "  age: 25,\n" +
                "  address: \"21 high street, Liverpool\"\n" +
                "}\n", o2.toString(),
                "Parsed Data2 should match expected text");
    }

    /**
     * Aims to reproduce a ClassCastException by trying to parse a Data1 serialised data
     * as if it was a Data2 serialised data without providing the class alias.
     */
    @Test
    @DisplayName("Missing alias should throw ClassCastException error")
    void reproduce() {
        assertThrows(ClassCastException.class, () -> {
            assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for alias error test");

            // This operation should fail and throw a ClassCastException
            Data2 o2 = WireType.TEXT.fromString(data);
            fail("" + o2);
        }, "Missing alias should throw ClassCastException");
    }

    /**
     * Sample data class representing a user's basic details.
     * This class extends the SelfDescribingMarshallable for serialisation and deserialisation.
     */
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class Data1 extends SelfDescribingMarshallable {
        String name;
        int age;
        String address;
    }

    /**
     * Another sample data class similar to Data1. Used to test the behaviours of class aliases.
     * Like Data1, this class also extends the SelfDescribingMarshallable.
     */
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class Data2 extends SelfDescribingMarshallable {
        String name;
        int age;
        String address;
    }
}
