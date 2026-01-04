/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class AbstractMarshallableCfgTest extends WireTestCommon {
    static class MyAMC extends AbstractMarshallableCfg {
        final NestedAMC nestedAMC = new NestedAMC();  // Configuration nested inside MyAMC
        final NestedSDM nestedSDM = new NestedSDM();  // Self-describing data nested inside MyAMC
    }

    // Define a nested configuration class that also extends AbstractMarshallableCfg
    static class NestedAMC extends AbstractMarshallableCfg {
        long number = 128;    // Default value for the number
        boolean flag;        // Boolean flag
    }

    // Define a nested self-describing data class
    static class NestedSDM extends SelfDescribingMarshallable {
        final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer();
        double amt = 1.0;
    }

    // Test Cases
    // Test the string representation of the MyAMC configuration
    @Test
    @DisplayName("Renders configuration to string with defaults")
    void asString() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip toString configuration test");

        MyAMC myAMC = new MyAMC();

        // Verify default string representation
        assertEquals("!net.openhft.chronicle.wire.AbstractMarshallableCfgTest$MyAMC {\n" +
                        "}\n",
                myAMC.toString(),
                "MyAMC toString should show default output");

        // Modify values for nested configurations
        myAMC.nestedAMC.number = 0;
        myAMC.nestedAMC.flag = true;
        myAMC.nestedSDM.bytes.append("Hi");

        // Verify modified string representation
        assertEquals("!net.openhft.chronicle.wire.AbstractMarshallableCfgTest$MyAMC {\n" +
                        "  nestedAMC: {\n" +
                        "    number: 0,\n" +
                        "    flag: true\n" +
                        "  },\n" +
                        "  nestedSDM: {\n" +
                        "    bytes: Hi,\n" +
                        "    amt: 1.0\n" +
                        "  }\n" +
                        "}\n",
                myAMC.toString(),
                "MyAMC toString should show updated output");
    }

    // Test the deep copy functionality
    @Test
    @DisplayName("Deep copy clones nested configuration values")
    void deepCopy() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip deep copy configuration test");

        MyAMC myAMC = new MyAMC();

        // Create a deep copy of the MyAMC instance
        MyAMC myAMC2 = myAMC.deepCopy();

        // Ensure deep copied nested configurations are not the same references
        assertNotSame(myAMC.nestedAMC, myAMC2.nestedAMC,
                "nestedAMC should be a deep copy");
        assertNotSame(myAMC.nestedSDM, myAMC2.nestedSDM,
                "nestedSDM should be a deep copy");
        assertNotSame(myAMC.nestedSDM.bytes, myAMC2.nestedSDM.bytes,
                "nestedSDM bytes should be a deep copy");
        assertEquals(myAMC.nestedAMC.number, myAMC2.nestedAMC.number,
                "nestedAMC.number should match after deep copy");
        assertEquals(myAMC.nestedAMC.flag, myAMC2.nestedAMC.flag,
                "nestedAMC.flag should match after deep copy");
        assertEquals(myAMC.nestedSDM.amt, myAMC2.nestedSDM.amt, 0.0,
                "nestedSDM.amt should match after deep copy");
    }
}
