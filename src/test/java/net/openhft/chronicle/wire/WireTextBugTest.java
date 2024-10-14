/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
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
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.junit.Assert.assertEquals;

/**
 * @author Rob Austin
 */
public class WireTextBugTest extends WireTestCommon {

    @org.junit.Test
    // Test for handling text within the Wire framework
    public void testText() {
        // Adding alias for the Bug class
        ClassAliasPool.CLASS_ALIASES.addAlias(Bug.class);

        // Create a BinaryWire object with specific settings
        @NotNull Wire encodeWire = new BinaryWire(Bytes.elasticByteBuffer(), false, true, false, Integer.MAX_VALUE, "lzw");

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
        public void setClOrdID(String aClOrdID) {
            clOrdID = aClOrdID;
        }
    }
}
