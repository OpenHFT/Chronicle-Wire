/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.Validatable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Marshallable2Test extends WireTestCommon {

    // Instance variable for the WireType being tested in this instance of the test
    private WireType wireType;

    // Constructor that initializes the WireType for this instance of the test
    public void initMarshallable2Test(WireType wireType) {
        this.wireType = wireType;
    }

    // Parameterized test setup: defining the different WireTypes that the tests will be run with
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{WireType.BINARY},
                new Object[]{WireType.BINARY_LIGHT},
                new Object[]{WireType.TEXT},
                new Object[]{WireType.YAML},
                new Object[]{WireType.YAML_ONLY},
                new Object[]{WireType.JSON},
                new Object[]{WireType.JSON_ONLY}
        );
    }

    // Test case to verify if the Wire's WriteDocumentContext behaves correctly in terms of being empty or not
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "{0}")
    public void writeDocumentIsEmpty(WireType wireType) {
        initMarshallable2Test(wireType);
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(16);
        Wire wire = wireType.apply(bytes);
        try (DocumentContext dc = wire.writingDocument()) {
            WriteDocumentContext wdc = (WriteDocumentContext) dc;
            assertTrue(wdc.isEmpty());
            wdc.wire().write("hi");
            assertFalse(wdc.isEmpty());
        }
        try (DocumentContext dc = wire.writingDocument(true)) {
            WriteDocumentContext wdc = (WriteDocumentContext) dc;
            assertTrue(wdc.isEmpty());
            wdc.wire().write("hi");
            assertFalse(wdc.isEmpty());
        }
    }

    // Test case to verify that a complex object with nested inner objects can be correctly serialized and deserialized
    @MethodSource("wireTypes")
    @SuppressWarnings("rawtypes")
    @ParameterizedTest(name = "{0}")
    public void testObject(WireType wireType) {
        initMarshallable2Test(wireType);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        Wire wire = wireType.apply(bytes);

        Outer source = new Outer("Armadillo");
        source.inner2 = new Inner2();

        wire.getValueOut().object(source);
        Outer target = wire.getValueIn().object(source.getClass());
        assertEquals(source, target);
        assertTrue(target.validated);
    }

    // Test case to verify if writing to the Wire is complete under various conditions
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "{0}")
    public void writingIsComplete(WireType wireType) {
        initMarshallable2Test(wireType);
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        Wire wire = wireType.apply(bytes);
        assertTrue(wire.writingIsComplete());
        try (DocumentContext dc = wire.writingDocument()) {
            assertFalse(dc.wire().writingIsComplete());
            dc.wire().write("say").text("hi");
        }
        assertTrue(wire.writingIsComplete());

        try (WriteDocumentContext dc = (WriteDocumentContext) wire.acquireWritingDocument(false)) {
            assertFalse(dc.wire().writingIsComplete());
            dc.wire().write("say").text("hi");
            dc.chainedElement(true);
        }
        assertFalse(wire.writingIsComplete());

        try (WriteDocumentContext dc = (WriteDocumentContext) wire.acquireWritingDocument(false)) {
            assertFalse(dc.wire().writingIsComplete());
            dc.wire().write("say").text("hi");
            dc.chainedElement(false);
        }
        assertTrue(wire.writingIsComplete());
    }

    // Static class representing an outer object that contains nested inner objects and implements the Validatable interface
    @SuppressWarnings("unused")
    private static class Outer extends SelfDescribingMarshallable implements Validatable {
        final String name;
        Inner1 inner1;
        Inner2 inner2;
        transient boolean validated;

        Outer(String name) {
            this.name = name;
        }

        @Override
        public void validate() throws InvalidMarshallableException {
            validated = true;
        }
    }

    // Static class representing a first type of inner object
    private static class Inner1 extends SelfDescribingMarshallable {
    }

    // Static class representing a second type of inner object
    private static class Inner2 extends SelfDescribingMarshallable {
    }
}
