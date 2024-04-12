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

package net.openhft.chronicle.wire.dynenum;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.EnumCache;
import net.openhft.chronicle.core.util.Mocker;
import net.openhft.chronicle.wire.*;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;

import static net.openhft.chronicle.wire.DynamicEnum.updateEnum;
import static org.junit.Assert.*;

// The WireDynamicEnumTest class extends WireTestCommon to inherit its common functionalities.
// This class is intended to test dynamic enumeration functionalities in the context of wiring.
public class WireDynamicEnumTest extends WireTestCommon {

    // This setup method is executed before each test.
    // It adds the required class aliases to the ClassAliasPool to facilitate serialization and deserialization.
    @Before
    public void addClassAlias() {
        ClassAliasPool.CLASS_ALIASES.addAlias(HoldsWDENum.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(UnwrapsWDENum.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(UnwrapsWDENum2.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(UsesWDENums.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(WDENums.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(WDENum2.class);
    }

    // This method simulates the dynamic addition of an enumeration value.
    // It initializes various objects and performs operations to test the behavior of dynamic enums.
    private static void doAddedEnum(WireType wireType) throws NoSuchFieldException {
        Wire tw = wireType.apply(Bytes.allocateElasticOnHeap());
        UsesWDENums nums = tw.methodWriter(UsesWDENums.class);
        nums.push(WDENums.ONE);

        EnumCache<WDENums> cache = EnumCache.of(WDENums.class);
        WDENums three = cache.valueOf("THREE");
        three.setField("nice", "Three");
        three.setField("value", 3);

        nums.unwraps(new UnwrapsWDENum(three));

        nums.push(three);

        nums.holds(new HoldsWDENum(WDENums.TWO, three));

        WDENum2 ace = new WDENum2("Ace", 101);
        nums.unwrap2(new UnwrapsWDENum2(ace));

        nums.push2(ace);

        // Assert that the operations and manipulations performed above yield the expected string representation.
        assertEquals("push: ONE\n" +
                "...\n" +
                "unwraps: {\n" +
                "  c: !WDENums {\n" +
                "    name: THREE,\n" +
                "    nice: Three,\n" +
                "    value: 3\n" +
                "  }\n" +
                "}\n" +
                "...\n" +
                "push: THREE\n" +
                "...\n" +
                "holds: {\n" +
                "  a: TWO,\n" +
                "  b: THREE\n" +
                "}\n" +
                "...\n" +
                "unwrap2: {\n" +
                "  d: !WDENum2 {\n" +
                "    name: ACE,\n" +
                "    nice: Ace,\n" +
                "    value: 101\n" +
                "  }\n" +
                "}\n" +
                "...\n" +
                "push2: ACE\n" +
                "...\n", tw.toString());
    }

    // This test validates the functionality of adding a dynamic enum using TEXT wire type.
    @Test
    public void dontResetDynamicEnum() {
        HoldsWDENum x = new HoldsWDENum(null, null);
        assertEquals("!HoldsWDENum {\n" +
                "  a: !!null \"\",\n" +
                "  b: !!null \"\"\n" +
                "}\n", x.toString());

        HoldsWDENum a = new HoldsWDENum(WDENums.ONE, WDENums.TWO);
        Wires.reset(a);
        assertEquals("!HoldsWDENum {\n" +
                "  a: !!null \"\",\n" +
                "  b: !!null \"\"\n" +
                "}\n", a.toString());
    }

    @Test
    public void addedEnum() throws NoSuchFieldException {
        doAddedEnum(WireType.TEXT);
    }

    // This test validates the functionality of adding a dynamic enum using YAML_ONLY wire type.
    @Test
    public void addedEnumYaml() throws NoSuchFieldException {
        doAddedEnum(WireType.YAML_ONLY);
    }

    // This test method validates the deserialization process of the dynamic enums and checks the correctness of the output.
    @Test
    public void deserialize() {
        // Define the text input string representing the serialized form of the dynamic enums and their operations.
        String text = "push: ONE\n" +
                "...\n" +
                "unwraps: {\n" +
                "  c: !WDENums {\n" +
                "    name: FOUR,\n" +
                "    nice: Four,\n" +
                "    value: 4\n" +
                "  }\n" +
                "}\n" +
                "...\n" +
                "push: FOUR\n" +
                "...\n" +
                "holds: {\n" +
                "  a: TWO,\n" +
                "  b: FOUR\n" +
                "}\n" +
                "...\n" +
                "unwrap2: {\n" +
                "  d: !WDENum2 {\n" +
                "    name: ACE,\n" +
                "    nice: Ace,\n" +
                "    value: 101\n" +
                "  }\n" +
                "}\n" +
                "...\n" +
                "push2: ACE\n" +
                "...\n";

        // Convert the text string to a TextWire object for deserialization.
        TextWire tw = new TextWire(Bytes.from(text)).useTextDocuments();

        // Create a StringWriter object to capture the output of the deserialization.
        StringWriter sw = new StringWriter();

        // Initialize a method reader for the deserialization process.
        // Mocker.logging generates mock implementations of the provided interface that log method calls.
        MethodReader reader = tw.methodReader(Mocker.logging(UsesWDENums.class, "", sw));

        // Read and deserialize each entry in the input text.
        // Expect to read 6 entries based on the input structure.
        for (int i = 0; i < 6; i++)
            assertTrue(reader.readOne());

        // After reading all entries, no more entries should be available.
        assertFalse(reader.readOne());

        // Assert that the output captured in the StringWriter matches the expected deserialized format.
        assertEquals("push[ONE]\n" +
                "unwraps[!UnwrapsWDENum {\n" +
                "  c: !WDENums {\n" +
                "    name: FOUR,\n" +
                "    nice: Four,\n" +
                "    value: 4\n" +
                "  }\n" +
                "}\n" +
                "]\n" +
                "push[FOUR]\n" +
                "holds[!HoldsWDENum {\n" +
                "  a: TWO,\n" +
                "  b: FOUR\n" +
                "}\n" +
                "]\n" +
                "unwrap2[!UnwrapsWDENum2 {\n" +
                "  d: !WDENum2 {\n" +
                "    name: ACE,\n" +
                "    nice: Ace,\n" +
                "    value: 101\n" +
                "  }\n" +
                "}\n" +
                "]\n" +
                "push2[!WDENum2 {\n" +
                "  name: ACE,\n" +
                "  nice: Ace,\n" +
                "  value: 101\n" +
                "}\n" +
                "]\n", sw.toString().replace("\r", ""));
    }

    /**
     * Test method to validate the custom deserialization process of the dynamic enums.
     * Instead of using a mocker to log method calls, a real implementation of the UsesWDENums interface is provided.
     */
    @Test
    public void deserialize2() {
        // Define the input text string, which represents serialized data.
        String text = "push: ONE\n" +
                "...\n" +
                "unwraps: {\n" +
                "  c: !WDENums {\n" +
                "    name: FOUR,\n" +
                "    nice: Four,\n" +
                "    value: 4\n" +
                "  }\n" +
                "}\n" +
                "...\n" +
                "push: FOUR\n" +
                "...\n" +
                "holds: {\n" +
                "  a: TWO,\n" +
                "  b: FOUR\n" +
                "}\n" +
                "...\n" +
                "unwrap2: {\n" +
                "  d: !WDENum2 {\n" +
                "    name: KING,\n" +
                "    nice: King,\n" +
                "    value: 112\n" +
                "  }\n" +
                "}\n" +
                "...\n" +
                "push2: ONE\n" +
                "...\n" +
                "push2: TWO\n" +
                "...\n" +
                "push2: KING\n" +
                "...\n";

        // Convert the text string to a TextWire object for deserialization.
        TextWire tw = new TextWire(Bytes.from(text)).useTextDocuments();

        // StringWriter is used to capture the customized output of the deserialization.
        StringWriter sw = new StringWriter();

        // Create a method reader with an anonymous class implementation of UsesWDENums interface.
        // This allows custom actions to be executed upon reading each piece of the serialized data.
        MethodReader reader = tw.methodReader(new UsesWDENums() {

            // Implement the 'push' method to capture the name, nice, and value of the WDENums enum.
            @Override
            public void push(WDENums nums) {
                sw.append(nums.name() + " ~ " + nums.nice + " ~ " + nums.value + "\n");
            }

            // Implement the 'holds' method to capture and print the held WDENums.
            @Override
            public void holds(HoldsWDENum holdsWDENum) {
                sw.append(holdsWDENum.toString());
                sw.append("# " + holdsWDENum.a.value + ", " + holdsWDENum.b.value + "\n");
            }

            // Implement the 'unwraps' method to mark a WDENums enum as updated and then call the 'updateEnum' method.
            @Override
            public void unwraps(UnwrapsWDENum unwrapsWDENum) {
                WDENums c = unwrapsWDENum.c;
                sw.append("Update " + c + "\n");
                updateEnum(c);
            }

            // Implement the 'push2' method to capture the name, nice, and value of the WDENum2 enum.
            @Override
            public void push2(WDENum2 nums) {
                sw.append(nums.name() + " = " + nums.nice + " = " + nums.value + "\n");
            }

            // Implement the 'unwrap2' method to mark a WDENum2 as updated and then call the 'updateEnum' method.
            @Override
            public void unwrap2(UnwrapsWDENum2 unwrapsWDENum2) {
                WDENum2 d = unwrapsWDENum2.d;
                sw.append("Update " + d + "\n");
                updateEnum(d);
            }
        });

        // Read and deserialize each entry in the input text.
        // Expect to read 8 entries based on the input structure.
        for (int i = 0; i < 8; i++)
            assertTrue(reader.readOne());

        // After reading all entries, no more entries should be available.
        assertFalse(reader.readOne());

        // Assert that the output captured in the StringWriter matches the expected deserialized format.
        assertEquals("ONE ~ One ~ 1\n" +
                "Update FOUR\n" +
                "FOUR ~ Four ~ 4\n" +
                "!HoldsWDENum {\n" +
                "  a: TWO,\n" +
                "  b: FOUR\n" +
                "}\n" +
                "# 2, 4\n" +
                "Update !WDENum2 {\n" +
                "  name: KING,\n" +
                "  nice: King,\n" +
                "  value: 112\n" +
                "}\n" +
                "\n" +
                "ONE = One = 1\n" +
                "TWO = Two = 2\n" +
                "KING = King = 112\n", sw.toString());
    }

    // Test the deep copy functionality of WDENums
    @Test
    public void testDeepCopy() {
        // Assert that the deep copy of WDENums.ONE is equal to WDENums.ONE
        assertEquals(WDENums.ONE, WDENums.ONE.deepCopy());

        // Assert that the deep copy of WDENums.TWO is the same instance as WDENums.TWO
        assertSame(WDENums.TWO, WDENums.TWO.deepCopy());
    }

    // Enum representing WDENums with associated values and display names
    enum WDENums implements WDEI, DynamicEnum<WDENums> {
        ONE("One", 1),  // Enum value representing "One" with a value of 1
        TWO("Two", 2);  // Enum value representing "Two" with a value of 2

        private final String nice;  // Display name for the enum value
        private final int value;    // Numerical value for the enum

        // Constructor to initialize the enum with a display name and value
        WDENums(String nice, int value) {
            this.nice = nice;
            this.value = value;
        }

        // Getter method for the display name
        @Override
        public String nice() {
            return nice;
        }

        // Getter method for the value
        @Override
        public int value() {
            return value;
        }
    }

    // Interface defining operations related to WDENums
    interface UsesWDENums {
        // Pushes the given WDENums value
        void push(WDENums nums);

        // Pushes the given WDENum2 value
        void push2(WDENum2 nums);

        // Handles the given HoldsWDENum value
        void holds(HoldsWDENum holdsWDENum);

        // Unwraps the given UnwrapsWDENum value
        void unwraps(UnwrapsWDENum unwrapsWDENum);

        // Unwraps the given UnwrapsWDENum2 value
        void unwrap2(UnwrapsWDENum2 unwrapsWDENum2);
    }

    // Interface defining essential methods for the WDE types
    interface WDEI {
        // Getter method for the name of the WDE type
        String name();

        // Getter method for the display name of the WDE type
        String nice();

        // Getter method for the value of the WDE type
        int value();
    }

    // Represents an enhanced version of WDENums with additional functionalities
    static class WDENum2 extends SelfDescribingMarshallable implements WDEI, DynamicEnum<WDENum2> {
        // Static instances representing pre-defined values
        static final WDENum2 ONE = new WDENum2("One", 1);  // Represents "One" with a value of 1
        static final WDENum2 TWO = new WDENum2("Two", 2);  // Represents "Two" with a value of 2

        private final String name;  // The uppercase version of the display name
        private final String nice;  // Display name for the enum value
        private final int value;    // Numerical value for the enum

        // Constructor initializes the enum with a display name and value
        WDENum2(String nice, int value) {
            this.name = nice.toUpperCase();  // Converts the display name to uppercase
            this.nice = nice;
            this.value = value;
        }

        // Getter method for the uppercase name of the enum value
        @Override
        public String name() {
            return name;
        }

        // Getter method for the display name
        @Override
        public String nice() {
            return nice;
        }

        // Getter method for the value
        @Override
        public int value() {
            return value;
        }

        // Returns the ordinal value, which is always -1 for this class
        @Override
        public int ordinal() {
            return -1;
        }
    }

    // Class to hold two instances of WDENums
    static class HoldsWDENum extends SelfDescribingMarshallable {
        WDENums a, b;  // The two instances of WDENums

        // Constructor initializes the holder with two WDENums values
        public HoldsWDENum(WDENums a, WDENums b) {
            this.a = a;
            this.b = b;
        }
    }

    // Class to wrap and unwrap an instance of WDENums for marshalling purposes
    static class UnwrapsWDENum extends SelfDescribingMarshallable {
        @AsMarshallable
        WDENums c;  // The wrapped instance of WDENums

        // Constructor initializes the wrapper with a WDENums value
        public UnwrapsWDENum(WDENums c) {
            this.c = c;
        }
    }

    // Class to wrap and unwrap an instance of WDENum2 for marshalling purposes
    static class UnwrapsWDENum2 extends SelfDescribingMarshallable {
        @AsMarshallable
        WDENum2 d;  // The wrapped instance of WDENum2

        // Constructor initializes the wrapper with a WDENum2 value
        public UnwrapsWDENum2(WDENum2 d) {
            this.d = d;
        }
    }
}
