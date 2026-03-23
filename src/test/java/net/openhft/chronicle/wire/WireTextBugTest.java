/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * @author Rob Austin
 */
public class WireTextBugTest extends WireTestCommon {

    @Test
    // Test for handling text within the Wire framework
    public void testText() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Adding alias for the Bug class
        ClassAliasPool.CLASS_ALIASES.addAlias(Bug.class);

        // Create a BinaryWire object with specific settings
        @NotNull Wire encodeWire = new BinaryWire(Bytes.allocateElasticOnHeap(), false, true, false, Integer.MAX_VALUE, "lzw");

        // Create a Bug object and set its clOrdID field
        @NotNull Bug b = new Bug();
        b.setClOrdID("FIX.4.4:12345678_client1->FOO/MINI1-1234567891234-12");

        // Check the Bug object's string representation
        assertEquals("!Bug {\n" +
                "  clOrdID: \"FIX.4.4:12345678_client1->FOO/MINI1-1234567891234-12\"\n" +
                "}\n", b.toString());

        // Write the Bug object to the wire
        encodeWire.getValueOut().object(b);

        // Convert the wire data to a byte array
        byte[] bytes = encodeWire.bytes().toByteArray();

        // Create a new BinaryWire for decoding, using the byte array
        @NotNull Wire decodeWire = new BinaryWire(Bytes.wrapForRead(bytes));

        // Read the Bug object from the wire
        @Nullable Object o = decodeWire.getValueIn()
                .object(Object.class);
        @Nullable Bug b2 = (Bug) o;

        // Check the deserialized Bug object's string representation
        assertEquals("!Bug {\n" +
                "  clOrdID: \"FIX.4.4:12345678_client1->FOO/MINI1-1234567891234-12\"\n" +
                "}\n", b2.toString());

        // Release resources
        encodeWire.bytes().releaseLast();
        decodeWire.bytes().releaseLast();
    }

    // Inner class to represent a Bug with a single field clOrdID
    static class Bug extends SelfDescribingMarshallable {
        private String clOrdID; // Field to hold some string identifier

        // Getter for clOrdID
        public String getClOrdID() {
            return clOrdID;
        }

        // Setter for clOrdID
        void setClOrdID(String aClOrdID) {
            clOrdID = aClOrdID;
        }
    }
}
