/*
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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// Class OverrideAValueTest extends WireTestCommon to perform tests related to value overrides and immutability
public class OverrideAValueTest extends WireTestCommon {

    // Test to ensure deserialization does not modify immutable objects
    @Test
    public void testDontTouchImmutables() {
        // Deserialization of a NumberHolder instance with num set to 2
        @Nullable NumberHolder nh = Marshallable.fromString("!" + NumberHolder.class.getName() + " { num: 2 } ");
        // Assertion to confirm that the ONE constant remains 1 and the deserialized value is 2
        assertEquals(1, NumberHolder.ONE.intValue());
        assertEquals(2, nh.num.intValue());
    }

    // Test to ensure deserialization does not modify immutable nested objects
    @Test
    public void testDontTouchImmutables2() {
        // Mark NumberHolder class as immutable
        ObjectUtils.immutabile(NumberHolder.class, true);
        // Deserialize an ObjectHolder with a nested NumberHolder
        @Nullable ObjectHolder oh = Marshallable.fromString("!" + ObjectHolder.class.getName() + " { nh: !" + NumberHolder.class.getName() + " { num: 3 } } ");
        // Assert various values remain unchanged after deserialization
        assertEquals(1, NumberHolder.ONE.intValue());
        assertEquals(1, ObjectHolder.NH.num.intValue());
        assertEquals(3, oh.nh.num.intValue());
    }

    // Test to ensure that class changes during deserialization are handled appropriately
    @Test
    public void testAllowClassChange() {
        // Deserialization of a ParentHolder instance with a nested SubClass object having name "bob" and value 3.3
        @Nullable ParentHolder ph = Marshallable.fromString("!" + ParentHolder.class.getName() + " { object: !" + SubClass.class.getName() + " { name: bob, value: 3.3 } } ");
        // Assertion to confirm the deserialized structure by comparing the toString() output
        assertEquals("!net.openhft.chronicle.wire.OverrideAValueTest$ParentHolder {\n" +
                "  object: !net.openhft.chronicle.wire.OverrideAValueTest$SubClass {\n" +
                "    name: bob,\n" +
                "    value: 3.3\n" +
                "  }\n" +
                "}\n", ph.toString());
    }

    // Static class NumberHolder, extending SelfDescribingMarshallable, to represent a holder for an Integer object
    static class NumberHolder extends SelfDescribingMarshallable {
        // Declaration and initialization of a static final Integer ONE
        @SuppressWarnings("UnnecessaryBoxing")
        public static final Integer ONE = new Integer(1);
        // Non-static Integer field num, initialized to ONE
        @NotNull
        Integer num = ONE;
    }

    // Static class ObjectHolder, extending SelfDescribingMarshallable, to hold an instance of NumberHolder
    static class ObjectHolder extends SelfDescribingMarshallable {
        // Declaration and initialization of a static final NumberHolder NH
        @SuppressWarnings("UnnecessaryBoxing")
        public static final NumberHolder NH = new NumberHolder();
        // Non-static NumberHolder field nh, initialized to NH
        @NotNull
        NumberHolder nh = NH;
    }

    // Static class ParentClass, extending SelfDescribingMarshallable, representing a parent class with a name attribute
    static class ParentClass extends SelfDescribingMarshallable {
        // String field name, initialized to "name"
        @NotNull
        String name = "name";
    }

    // Static class SubClass, extending ParentClass, representing a subclass with an additional value attribute
    static class SubClass extends ParentClass {
        // Double field value, initialized to 1.28
        double value = 1.28;
    }

    // Static class ParentHolder, extending SelfDescribingMarshallable, to encapsulate an instance of ParentClass
    static class ParentHolder extends SelfDescribingMarshallable {
        // Final field object, instantiated as a new ParentClass
        final ParentClass object = new ParentClass();
    }
}
