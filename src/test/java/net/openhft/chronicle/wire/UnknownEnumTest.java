/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

// Class to test behavior of Wire in the context of Enums, especially unknown Enums
public class UnknownEnumTest extends WireTestCommon {

    // Serialized map data, presumably representing an unknown Enum value for testing purposes
    private static final byte[] SERIALISED_MAP_DATA = new byte[]{
            (byte) -59, 101, 118, 101, 110, 116, -126, 60, 0, 0, 0, -71, 3,
            107, 101, 121, -74, 47, 110, 101, 116, 46, 111, 112, 101, 110, 104,
            102, 116, 46, 99, 104, 114, 111, 110, 105, 99, 108, 101, 46, 119,
            105, 114, 101, 46, 85, 110, 107, 110, 111, 119, 110, 69, 110, 117,
            109, 84, 101, 115, 116, 36, 84, 101, 109, 112, -27, 70, 73, 82, 83, 84};

    // Helper method to create a Wire instance
    public Wire createWire() {
        return WireType.TEXT.apply(Bytes.allocateElasticOnHeap(128));
    }

    // Test to check how the Wire handles an unknown dynamic Enum
    @Test
    public void testUnknownDynamicEnum() {
        Wire wire = createWire();
        wire.write("value").text("Maybe");

        // Reading an unknown Enum value as a known Enum type (YesNo)
        YesNo yesNo = wire.read("value").asEnum(YesNo.class);

        Wire wire2 = createWire();
        wire2.write("value").asEnum(yesNo);

        // Verifying that the written Enum value is correctly read as text
        String maybe = wire2.read("value").text();
        assertEquals("Maybe", maybe);
    }

    // Test to check how the Wire handles an unknown static Enum
    @Test
    public void testUnknownStaticEnum() {
        Wire wire = createWire();
        wire.write("value").text("Maybe");

        // Expecting a failure while trying to read an unknown Enum value as a known static Enum type (StrictYesNo)
        assertThrows(IllegalArgumentException.class, () -> wire.read("value").asEnum(StrictYesNo.class));
    }

    /*
    Documents the behaviour of BinaryWire when an enum type is unknown
     */
    @Test(expected = ClassNotFoundRuntimeException.class)
    public void shouldConvertEnumValueToStringWhenTypeIsNotKnownInBinaryWireThrows() {
        final Bytes<ByteBuffer> bytes = Bytes.wrapForRead(ByteBuffer.wrap(SERIALISED_MAP_DATA));

        final Wire wire = WireType.BINARY.apply(bytes);

        // Reading the serialized map data and ensuring the unknown Enum value is read as a String
        final Map<String, Object> enumField = wire.read("event").marshallableAsMap(String.class, Object.class);
        assertEquals("FIRST", enumField.get("key"));
    }

    // This test ensures that TextWire produces a friendly error message for unknown Enum types
    @Test
    public void shouldGenerateFriendlyErrorMessageWhenTypeIsNotKnownInTextWire() {
        Wires.GENERATE_TUPLES = true;
        try {
            final TextWire textWire = TextWire.from("enumField: !UnknownEnum QUX");
            textWire.valueIn.wireIn().read("enumField").object();

            fail(); // This point should not be reached
        } catch (Exception e) {
            // Ensuring the error message is in the expected format
            String message = e.getMessage().replaceAll(" [a-z0-9.]+.Proxy\\d+", " ProxyXX");
            assertThat(message,
                    is(equalTo("Trying to read marshallable class ProxyXX at [pos: 23, rlim: 27, wlim: 27, cap: 27 ]  QUX expected to find a {")));
        } finally {
            Wires.GENERATE_TUPLES = false;
        }
    }

    @SuppressWarnings("deprecation")
    enum YesNo implements DynamicEnum {
        Yes,
        No
    }

    // Static Enum for testing
    enum StrictYesNo {
        Yes,
        No
    }
}
