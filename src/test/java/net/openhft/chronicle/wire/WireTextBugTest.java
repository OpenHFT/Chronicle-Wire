/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

/**
 * @author Rob Austin
 */
public class WireTextBugTest extends WireTestCommon {
    private static final String BUG_TEXT = "!Bug {\n" +
            "  clOrdID: \"FIX.4.4:12345678_client1->FOO/MINI1-1234567891234-12\"\n" +
            "}\n";

    @org.junit.Test
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
        assertEquals(BUG_TEXT, b.toString());

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
        assertEquals(BUG_TEXT, b2.toString());

        // Release resources
        encodeWire.bytes().releaseLast();
        decodeWire.bytes().releaseLast();
    }

    @org.junit.Test
    public void textWireOnDirectBytesSurvivesBufferMutation() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        ClassAliasPool.CLASS_ALIASES.addAlias(Bug.class);

        Bytes<?> directBytes = Bytes.allocateElasticDirect(128);
        try {
            Wire encodeWire = new TextWire(directBytes);
            Bug bug = new Bug();
            bug.setClOrdID("FIX.4.4:12345678_client1->FOO/MINI1-1234567891234-12");
            encodeWire.getValueOut().object(bug);

            byte[] snapshot = encodeWire.bytes().toByteArray();
            directBytes.readPositionRemaining(0, directBytes.writePosition());
            Wire decodeWire = new TextWire(directBytes);
            Bug decoded = decodeWire.getValueIn().object(Bug.class);
            assertEquals(BUG_TEXT, decoded.toString());

            directBytes.zeroOut(0, directBytes.realCapacity());
            directBytes.clear();

            // The decoded object should keep its text even though the backing buffer was zeroed.
            assertEquals(BUG_TEXT, decoded.toString());

            // Local mutations must not impact a fresh decode from the saved snapshot.
            decoded.setClOrdID(decoded.getClOrdID() + "-local");
            Wire snapshotWire = new TextWire(Bytes.wrapForRead(snapshot));
            try {
                Bug snapshotBug = snapshotWire.getValueIn().object(Bug.class);
                assertEquals(BUG_TEXT, snapshotBug.toString());
            } finally {
                snapshotWire.bytes().releaseLast();
            }
        } finally {
            directBytes.releaseLast();
        }
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
