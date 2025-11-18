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
    static class MyAMC extends AbstractMarshallableCfg {
        NestedAMC nestedAMC = new NestedAMC();  // Configuration nested inside MyAMC
        NestedSDM nestedSDM = new NestedSDM();  // Self-describing data nested inside MyAMC
    }

    // Define a nested configuration class that also extends AbstractMarshallableCfg
    static class NestedAMC extends AbstractMarshallableCfg {
        long number = 128;    // Default value for the number
        boolean flag;        // Boolean flag
    }

    // Define a nested self-describing data class
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
        // Note: Use content-based assertions rather than exact string match
        // to avoid non-deterministic field ordering from getDeclaredFields()
        String actual = myAMC.toString();
        
        // Verify the class header is present
        assertEquals(true, actual.startsWith("!net.openhft.chronicle.wire.AbstractMarshallableCfgTest$MyAMC {"));
        
        // Verify both nested objects and their fields are present with correct values
        assertEquals(true, actual.contains("nestedAMC: {"));
        assertEquals(true, actual.contains("number: 0"));
        assertEquals(true, actual.contains("flag: true"));
        assertEquals(true, actual.contains("nestedSDM: {"));
        assertEquals(true, actual.contains("bytes: Hi"));
        assertEquals(true, actual.contains("amt: 1.0"));
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
