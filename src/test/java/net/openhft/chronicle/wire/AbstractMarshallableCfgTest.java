/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assume.assumeFalse;

public class AbstractMarshallableCfgTest extends WireTestCommon{
    @FieldOrder({"nestedAMC", "nestedSDM"})
    static class MyAMC extends AbstractMarshallableCfg {
        NestedAMC nestedAMC = new NestedAMC();  // Configuration nested inside MyAMC
        NestedSDM nestedSDM = new NestedSDM();  // Self-describing data nested inside MyAMC
    }

    // Define a nested configuration class that also extends AbstractMarshallableCfg
    @FieldOrder({"number", "flag"})
    static class NestedAMC extends AbstractMarshallableCfg {
        long number = 128;    // Default value for the number
        boolean flag;        // Boolean flag
    }

    // Define a nested self-describing data class
    @FieldOrder({"bytes", "amt"})
    static class NestedSDM extends SelfDescribingMarshallable {
        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer();
        double amt = 1.0;
    }

    // Test Cases
    // Test the string representation of the MyAMC configuration
    @Test
    public void asString() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        MyAMC myAMC = new MyAMC();

        // Verify default string representation
        assertEquals("" +
                        "!net.openhft.chronicle.wire.AbstractMarshallableCfgTest$MyAMC {\n" +
                        "}\n",
                myAMC.toString());

        // Modify values for nested configurations
        myAMC.nestedAMC.number = 0;
        myAMC.nestedAMC.flag = true;
        myAMC.nestedSDM.bytes.append("Hi");

        // Verify modified string representation
        assertEquals("" +
                        "!net.openhft.chronicle.wire.AbstractMarshallableCfgTest$MyAMC {\n" +
                        "  nestedAMC: {\n" +
                        "    number: 0,\n" +
                        "    flag: true\n" +
                        "  },\n" +
                        "  nestedSDM: {\n" +
                        "    bytes: Hi,\n" +
                        "    amt: 1.0\n" +
                        "  }\n" +
                        "}\n",
                myAMC.toString());
    }

    // Test the deep copy functionality
    @Test
    public void deepCopy() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        MyAMC myAMC = new MyAMC();

        // Create a deep copy of the MyAMC instance
        MyAMC myAMC2 = myAMC.deepCopy();

        // Ensure deep copied nested configurations are not the same references
        assertNotSame(myAMC.nestedAMC, myAMC2.nestedAMC);
        assertNotSame(myAMC.nestedSDM, myAMC2.nestedSDM);
        assertNotSame(myAMC.nestedSDM.bytes, myAMC2.nestedSDM.bytes);
    }
}
