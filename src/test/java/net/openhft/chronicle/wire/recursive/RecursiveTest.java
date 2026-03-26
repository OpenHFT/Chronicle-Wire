/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.recursive;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.WireMarshaller;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Test for recursion in the marshaller and fields. WIRE_MARSHALLER_CL.get should not recurse while
 * looking up fields of the class. At time of writing this occurs when checking if the component class
 * of a subfield is a leaf and is only a problem when the component class is the same as the parent class.
 */
class RecursiveTest {

    @Test
    void referToBaseClass() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        test(new ReferToBaseClass("hello"), new ReferToBaseClass(null));
    }

    @Test
    void referToSameClass() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        test(new ReferToSameClass("test"), new ReferToSameClass(null));
    }

    @Test
    void marshallerReferToSameClass() {
        WireMarshaller<?> marshaller = WireMarshaller.WIRE_MARSHALLER_CL.get(ReferToSameClass.class);
        assertNotNull(marshaller);
    }

    @Test
    void marshallerReferToBaseClass() {
        WireMarshaller<?> marshaller = WireMarshaller.WIRE_MARSHALLER_CL.get(ReferToBaseClass.class);
        assertNotNull(marshaller);
    }

    private void test(Base from, Base to) {
        from.copyTo(to);
        assertEquals(from.name(), to.name());
    }
}
