/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

/**
 * Test class to validate behaviors associated with class aliases in the context of Wire.
 * This test extends the WireTestCommon for utility behaviors related to Wire tests.
 */
public class Issue277Test extends WireTestCommon {

    /**
     * Sets up the testing environment before executing the test methods.
     * It specifically adds class aliases to the ClassAliasPool.
     */
    @Before
    public void setup() {
        // Add class aliases for Data1 and Data2 to the ClassAliasPool
        ClassAliasPool.CLASS_ALIASES.addAlias(Data1.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(Data2.class);
    }

    // Sample data in string format to be used for deserialization tests
    private static final String data = "" +
            "!Data1 {\n" +
            "  name: Tom,\n" +
            "  age: 25,\n" +
            "  address: \"21 high street, Liverpool\"\n" +
            "}\n";

    /**
     * Validates that the data can be correctly parsed into a Data2 object.
     * This test does not expect a RuntimeException because a correct class alias is provided.
     */
    @Test
    public void isOk() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Deserialize the sample data into a Data2 object without throwing an exception
        Data2 o2 = WireType.TEXT.fromString(Data2.class, data);

        // Assert that the deserialized Data2 object matches the expected string representation
        assertEquals("!Data2 {\n" +
                "  name: Tom,\n" +
                "  age: 25,\n" +
                "  address: \"21 high street, Liverpool\"\n" +
                "}\n", o2.toString());
    }

    /**
     * Aims to reproduce a ClassCastException by trying to parse a Data1 serialized data
     * as if it was a Data2 serialized data without providing the class alias.
     */
    @Test(expected = ClassCastException.class)
    public void reproduce() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // This operation should fail and throw a ClassCastException
        Data2 o2 = WireType.TEXT.fromString(data);
        fail("" + o2);
    }

    /**
     * Sample data class representing a user's basic details.
     * This class extends the SelfDescribingMarshallable for serialization and deserialization.
     */
    private static class Data1 extends SelfDescribingMarshallable {
        String name;
        int age;
        String address;
    }

    /**
     * Another sample data class similar to Data1. Used to test the behaviors of class aliases.
     * Like Data1, this class also extends the SelfDescribingMarshallable.
     */
    private static class Data2 extends SelfDescribingMarshallable {
        String name;
        int age;
        String address;
    }
}
