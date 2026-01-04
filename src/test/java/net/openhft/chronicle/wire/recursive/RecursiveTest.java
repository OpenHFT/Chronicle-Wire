/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.recursive;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.WireMarshaller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for recursion in the marshaller and fields. WIRE_MARSHALLER_CL.get should not recurse while
 * looking up fields of the class. At time of writing this occurs when checking if the component class
 * of a subfield is a leaf and is only a problem when the component class is the same as the parent class.
 */
class RecursiveTest {

    @Test
    @DisplayName("copyTo handles base-class recursion safely")
    void referToBaseClass() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for base-class recursion test");
        ReferToBaseClass from = new ReferToBaseClass("hello");
        ReferToBaseClass to = new ReferToBaseClass(null);
        copyTo(from, to);
        assertEquals(from.name(), to.name(), "copyTo should copy name for base-class recursion");
    }

    @Test
    @DisplayName("copyTo handles same-class recursion safely")
    void referToSameClass() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for same-class recursion test");
        ReferToSameClass from = new ReferToSameClass("test");
        ReferToSameClass to = new ReferToSameClass(null);
        copyTo(from, to);
        assertEquals(from.name(), to.name(), "copyTo should copy name for same-class recursion");
    }

    @Test
    @DisplayName("Marshaller resolves same-class recursion safely")
    void marshallerReferToSameClass() {
        WireMarshaller<?> marshaller= WireMarshaller.WIRE_MARSHALLER_CL.get(ReferToSameClass.class);
        assertNotNull(marshaller, "Marshaller should be created for same-class recursion");
    }

    @Test
    @DisplayName("Marshaller resolves base-class recursion safely")
    void marshallerReferToBaseClass() {
        WireMarshaller<?> marshaller = WireMarshaller.WIRE_MARSHALLER_CL.get(ReferToBaseClass.class);
        assertNotNull(marshaller, "Marshaller should be created for base-class recursion");
    }

    private void copyTo(Base from, Base to) {
        from.copyTo(to);
    }
}
