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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(value = Parameterized.class)
public class CopyTest extends WireTestCommon {

    // Initial source and destination wire types for the copy test
    private final WireType from, to;

    // Determines if the test uses the type information while copying
    private boolean withType;

    // Constructor to initialize wire types and whether the test runs with type information
    public CopyTest(WireType from, WireType to, boolean withType) {
        this.from = from;
        this.to = to;
        this.withType = withType;
    }

    // Define the combinations of wire types and settings for the test
    @Parameterized.Parameters(name = "from: {0}, to: {1}, withType: {2}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                // new Object[] {WireType.TEXT, WireType.BINARY, true}, // not supported yet
                // new Object[] {WireType.TEXT, WireType.BINARY, false}, // not supported yet
                new Object[]{WireType.YAML, WireType.BINARY_LIGHT, true},
                // new Object[]{WireType.TEXT, WireType.BINARY_LIGHT, true},
                new Object[]{WireType.BINARY, WireType.JSON, false},
                new Object[]{WireType.BINARY, WireType.TEXT, true},
                new Object[]{WireType.BINARY, WireType.TEXT, false},
                //  new Object[]{WireType.RAW, WireType.RAW, false},
                new Object[]{WireType.JSON, WireType.JSON, false},
                // new Object[]{WireType.JSON, WireType.JSON, true}, // not supported as types are dropped for backward compatability
                new Object[]{WireType.JSON, WireType.JSON_ONLY, false},
                new Object[]{WireType.JSON_ONLY, WireType.JSON_ONLY, false},
                new Object[]{WireType.JSON_ONLY, WireType.JSON_ONLY, true},
                new Object[]{WireType.TEXT, WireType.TEXT, false},
                new Object[]{WireType.TEXT, WireType.TEXT, true},
                new Object[]{WireType.YAML, WireType.YAML, false},
                new Object[]{WireType.YAML, WireType.YAML, true},
                new Object[]{WireType.YAML_ONLY, WireType.YAML_ONLY, true},
                new Object[]{WireType.JSON_ONLY, WireType.TEXT, true},
                new Object[]{WireType.JSON_ONLY, WireType.YAML, true},
                new Object[]{WireType.JSON_ONLY, WireType.BINARY, true},
                new Object[]{WireType.JSON_ONLY, WireType.BINARY_LIGHT, true}
        );
    }

    // This test ensures that data can be copied from one wire to another, preserving integrity
    @SuppressWarnings("rawtypes")
    @Test
    public void testCopy() {
        // Create source bytes and wire objects
        Bytes<?> bytesFrom = Bytes.allocateElasticOnHeap(64);
        Wire wireFrom = from.apply(bytesFrom);

        // Create destination bytes and wire objects
        Bytes<?> bytesTo = Bytes.allocateElasticOnHeap(64);
        Wire wireTo = to.apply(bytesTo);

        // Create an instance of 'AClass' for testing
        AClass a = create();

        // Write the 'AClass' instance to the source wire
        if (withType)
            wireFrom.write("test").object(a);
        else
            wireFrom.write("test").marshallable(a);

        // Copy data from source to destination wire
        wireFrom.copyTo(wireTo);

        // Perform checks if the destination wire type is JSON
        if (to == WireType.JSON || to == WireType.JSON_ONLY) {
            final String text = wireTo.toString();
            assertFalse(text, text.contains("? "));
            assertFalse(text, text.contains("\n\""));
        }

        // Validate the data in the destination wire
        final String event = wireTo.readEvent(String.class);
        assertEquals("test", event);
        AClass b = wireTo.getValueIn().object(AClass.class);

        assertEquals(a, b);

        // If testing with type information, re-run copy with typedMarshallable
        if (withType) {
            wireFrom.clear();
            wireTo.clear();

            wireFrom.write("msg").typedMarshallable(a);
            wireFrom.copyTo(wireTo);
            if (from == WireType.JSON_ONLY) {
                System.out.println(wireFrom);
                System.out.println(wireTo);
            }
            Object b2 = wireTo.read("msg").object();

            assertEquals(a, b2);
        }
    }

    // Helper method to create a test instance of 'AClass'
    private AClass create() {
        AClass aClass = new AClass();
        aClass.map = new HashMap<>();
        aClass.map.put(CcyPair.EURUSD, "eurusd");
        aClass.array = new String[]{"hello", "there"};
        aClass.intValue = 11;
        aClass.value = 123.4;
        return aClass;
    }

    // Class representing the data structure to be used in the copy test
    @SuppressWarnings("unused")
    private static class AClass extends SelfDescribingMarshallable {
        Map<CcyPair, String> map;
        String[] array;
        int intValue;
        double value;
    }
}
