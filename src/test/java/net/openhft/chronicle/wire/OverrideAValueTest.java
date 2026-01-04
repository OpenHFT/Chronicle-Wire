/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// Class OverrideAValueTest extends WireTestCommon to perform tests related to value overrides and immutability
public class OverrideAValueTest extends WireTestCommon {

    // Test to ensure deserialization does not modify immutable objects
    @Test
    @DisplayName("Immutable constant remains unchanged on deserialisation")
    public void testDontTouchImmutables() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for immutable deserialisation test");

        // Deserialization of a NumberHolder instance with num set to 2
        @Nullable NumberHolder nh = Marshallable.fromString("!" + NumberHolder.class.getName() + " { num: 2 } ");
        // Assertion to confirm that the ONE constant remains 1 and the deserialized value is 2
        assertEquals(1, NumberHolder.ONE.intValue(), "Immutable constant should remain unchanged");
        assertEquals(2, nh.num.intValue(), "Deserialised value should retain the provided number");
    }

    // Test to ensure deserialization does not modify immutable nested objects
    @Test
    @DisplayName("Immutable nested constant remains unchanged after deserialisation")
    public void testDontTouchImmutables2() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for nested immutable test");

        // Mark NumberHolder class as immutable
        ObjectUtils.immutable(NumberHolder.class, true);
        // Deserialize an ObjectHolder with a nested NumberHolder
        @Nullable ObjectHolder oh = Marshallable.fromString("!" + ObjectHolder.class.getName() + " { nh: !" + NumberHolder.class.getName() + " { num: 3 } } ");
        // Assert various values remain unchanged after deserialization
        assertEquals(1, NumberHolder.ONE.intValue(), "Immutable constant should remain unchanged in nested case");
        assertEquals(1, ObjectHolder.NH.num.intValue(), "Immutable nested holder should remain unchanged");
        assertEquals(3, oh.nh.num.intValue(), "Deserialised nested value should retain the provided number");
    }

    // Test to ensure that class changes during deserialization are handled appropriately
    @Test
    @DisplayName("Class change is allowed during deserialisation")
    public void testAllowClassChange() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for class change deserialisation test");

        // Deserialization of a ParentHolder instance with a nested SubClass object having name "bob" and value 3.3
        @Nullable ParentHolder ph = Marshallable.fromString("!" + ParentHolder.class.getName() + " { object: !" + SubClass.class.getName() + " { name: bob, value: 3.3 } } ");
        // Assertion to confirm the deserialized structure by comparing the toString() output
        assertEquals("!net.openhft.chronicle.wire.OverrideAValueTest$ParentHolder {\n" +
                "  object: !net.openhft.chronicle.wire.OverrideAValueTest$SubClass {\n" +
                "    name: bob,\n" +
                "    value: 3.3\n" +
                "  }\n" +
                "}\n", ph.toString(), "Deserialised parent should include the subclass fields");
    }

    // Static class NumberHolder, extending SelfDescribingMarshallable, to represent a holder for an Integer object
    @SuppressWarnings({"deprecation", "removal"})
    static class NumberHolder extends SelfDescribingMarshallable {
        // Declaration and initialization of a static final Integer ONE
        @SuppressWarnings("UnnecessaryBoxing")
        static final Integer ONE = new Integer(1);
        // Non-static Integer field num, initialized to ONE
        @NotNull
        final
        Integer num = ONE;
    }

    // Static class ObjectHolder, extending SelfDescribingMarshallable, to hold an instance of NumberHolder
    static class ObjectHolder extends SelfDescribingMarshallable {
        // Declaration and initialization of a static final NumberHolder NH
        @SuppressWarnings("UnnecessaryBoxing")
        static final NumberHolder NH = new NumberHolder();
        // Non-static NumberHolder field nh, initialized to NH
        @NotNull
        final
        NumberHolder nh = NH;
    }

    // Static class ParentClass, extending SelfDescribingMarshallable, representing a parent class with a name attribute
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class ParentClass extends SelfDescribingMarshallable {
        // String field name, initialized to "name"
        @NotNull
        String name = "name";
    }

    // Static class SubClass, extending ParentClass, representing a subclass with an additional value attribute
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class SubClass extends ParentClass {
        // Double field value, initialized to 1.28
        double value = 1.28;
    }

    // Static class ParentHolder, extending SelfDescribingMarshallable, to encapsulate an instance of ParentClass
    private static class ParentHolder extends SelfDescribingMarshallable {
        // Final field object, instantiated as a new ParentClass
        final ParentClass object = new ParentClass();
    }
}
