/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire.recursive;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.WireMarshaller;
import org.junit.Test;

import static org.junit.Assume.assumeFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for recursion in the marshaller and fields. WIRE_MARSHALLER_CL.get should not recurse while
 * looking up fields of the class. At time of writing this occurs when checking if the component class
 * of a subfield is a leaf and is only a problem when the component class is the same as the parent class.
 */
public class RecursiveTest {

    @Test
    public void referToBaseClass() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        test(new ReferToBaseClass("hello"), new ReferToBaseClass(null));
    }

    @Test
    public void referToSameClass() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        test(new ReferToSameClass("test"), new ReferToSameClass(null));
    }

    @Test
    public void marshallerReferToSameClass() {
        WireMarshaller<?> marshaller= WireMarshaller.WIRE_MARSHALLER_CL.get(ReferToSameClass.class);
        assertNotNull(marshaller);
    }

    @Test
    public void marshallerReferToBaseClass() {
        WireMarshaller<?> marshaller = WireMarshaller.WIRE_MARSHALLER_CL.get(ReferToBaseClass.class);
        assertNotNull(marshaller);
    }

    private void test(Base from, Base to) {
        from.copyTo(to);
        assertEquals(from.name(), to.name());
    }
}
