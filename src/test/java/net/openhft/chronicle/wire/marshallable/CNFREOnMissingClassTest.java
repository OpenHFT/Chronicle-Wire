/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import net.openhft.chronicle.wire.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class CNFREOnMissingClassTest extends WireTestCommon {

    /**
     * Validates that a ClassNotFoundRuntimeException is correctly thrown when attempting
     * to deserialize data with a class alias that is not registered in the system.
     */
    @Test
    @DisplayName("Missing class alias should raise ClassNotFoundRuntimeException on parse")
    public void throwClassNotFoundRuntimeExceptionOnMissingClassAlias() {
        TwoFields defaults = new TwoFields();
        assertNull(defaults.name, "TwoFields name should default to null");
        assertNull(defaults.fieldOne, "TwoFields fieldOne should default to null");
        assertThrows(ClassNotFoundRuntimeException.class, () -> {
            Wires.setGenerateTuples(false);
            Wire wire = new TextWire(Bytes.from("a: !Aaa { hi: bye }"));
            Object object = wire.read("a").object();
            System.out.println(object);
            assertNotNull(object, "Wire should return a non-null object before failure");
        }, "Missing class alias should raise ClassNotFoundRuntimeException during TextWire parse");
    }

    private static class TwoFields extends AbstractMarshallableCfg {
        private String name;
        private Object fieldOne;
    }

    /**
     * Tests if a ClassNotFoundRuntimeException is thrown when a class for a field is missing.
     */
    @Test
    @DisplayName("Missing class for field raises ClassNotFoundRuntimeException")
    public void throwClassNotFoundRuntimeExceptionOnMissingClassForField() {
        assertThrows(ClassNotFoundRuntimeException.class, () -> {
            Wires.setGenerateTuples(false);
            ClassAliasPool.CLASS_ALIASES.addAlias(TwoFields.class);
            String simpleObject = "!TwoFields { name: \"henry\", fieldOne: !ThisClassDoesntExist { value: 1234 } }";
            String key = "class.not.found.for.missing.class.alias";
            Jvm.startup().on(CNFREOnMissingClassTest.class, "Value of " + key + ": " + Jvm.getBoolean(key));
            final TwoFields simple = Marshallable.fromString(simpleObject);
        }, "Missing field class should raise ClassNotFoundRuntimeException for TwoFields");
    }

    private static class UsesTwoFields extends AbstractMarshallableCfg {
        private TwoFields bothFields;
        private String name;
    }

    /**
     * Failing to load a class for a field with a type of java.lang.Object causes the correct behaviour but in
     * an unexpected code path (the check for a classloader at TextWire#typeOrPrefixObject - line 1913
     */
    @Test
    @DisplayName("Missing class for Object field raises ClassNotFoundRuntimeException")
    public void throwClassNotFoundRuntimeExceptionOnMissingClassForField2() {
        UsesTwoFields defaults = new UsesTwoFields();
        assertNull(defaults.bothFields, "UsesTwoFields bothFields should default to null");
        assertNull(defaults.name, "UsesTwoFields name should default to null");
        assertThrows(ClassNotFoundRuntimeException.class, () ->
                testFieldNotObject0(false, null),
                "Missing Object field class should raise ClassNotFoundRuntimeException");
    }

    @Test
    @DisplayName("Missing class for non-tuple field raises ClassNotFoundRuntimeException")
    public void throwClassNotFoundRuntimeExceptionOnMissingClassForFieldNotATuple() {
        assertThrows(ClassNotFoundRuntimeException.class, () ->
                testFieldNotObject0(true, null),
                "Missing non-tuple field class should raise ClassNotFoundRuntimeException");
    }

    private void testFieldNotObject0(boolean generateTuples, String expected) {
        Wires.setGenerateTuples(generateTuples);
        ClassAliasPool.CLASS_ALIASES.addAlias(UsesTwoFields.class);
        String simpleObject = "!UsesTwoFields { name: \"henry\", bothFields: !ThisClassDoesntExist { name: Jerry } }";
        final UsesTwoFields simple = Marshallable.fromString(simpleObject);
        assertEquals(expected, simple.toString(),
                "Parsed object should match expected output");
    }

    static class UsesInterfaceField extends AbstractMarshallableCfg {
        private TwoFields bothFields;
        private String name;

        private TestEngineListener engineListener;

        public interface TestEngineListener { }
    }

    /**
     * Tests if a ClassNotFoundRuntimeException is thrown for a missing class for an interface field.
     */
    @Test
    @DisplayName("Missing class for interface field raises ClassNotFoundRuntimeException")
    public void throwClassNotFoundRuntimeExceptionOnMissingClassForInterfaceField() {
        UsesInterfaceField defaults = new UsesInterfaceField();
        assertNull(defaults.bothFields, "UsesInterfaceField bothFields should default to null");
        assertNull(defaults.name, "UsesInterfaceField name should default to null");
        assertNull(defaults.engineListener, "UsesInterfaceField engineListener should default to null");
        assertThrows(ClassNotFoundRuntimeException.class, () ->
                parseInterfaceField(false),
                "Missing interface field class should raise ClassNotFoundRuntimeException");
    }

    /**
     * Tuple generation for an interface field
     * when its corresponding class is not found.
     */
    @Test
    @DisplayName("Tuple generation works when interface field class is missing")
    public void useTupleOnMissingClassForInterfaceField() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for tuple generation test");

        String expected = "!UsesInterfaceField {\n" +
                "  name: henry,\n" +
                "  engineListener: !ThisListenerClassDoesntExist {\n" +
                "    value: 128\n" +
                "  }\n" +
                "}\n";
        assertEquals(expected, parseInterfaceField(true), "tuple: missing class for interface field");
    }

    private String parseInterfaceField(boolean generateTuples) {
        Wires.setGenerateTuples(generateTuples);

        ClassAliasPool.CLASS_ALIASES.addAlias(UsesInterfaceField.class);
        String simpleObject = "!UsesInterfaceField { name: \"henry\", engineListener: !ThisListenerClassDoesntExist { value: 128 } }";
        final UsesInterfaceField simple = Marshallable.fromString(simpleObject);
        assertEquals("henry", simple.name, "Parsed interface field should set name");
        assertNotNull(simple.engineListener, "Parsed interface field should set engineListener tuple");
        return simple.toString();
    }
}
