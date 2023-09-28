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
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.Validatable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(value = Parameterized.class)
public class Marshallable2Test extends WireTestCommon {
    private final WireType wireType;

    public Marshallable2Test(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters(name = "{0}")
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

    @Test
    public void writeDocumentIsEmpty() {
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

    @SuppressWarnings("rawtypes")
    @Test
    public void testObject() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        Wire wire = wireType.apply(bytes);

        Outer source = new Outer("Armadillo");
        source.inner2 = new Inner2();

        wire.getValueOut().object(source);
        Outer target = wire.getValueIn().object(source.getClass());
        assertEquals(source, target);
        assertTrue(target.validated);
    }

    @Test
    public void writingIsComplete() {
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

    @SuppressWarnings("unused")
    private static class Outer extends SelfDescribingMarshallable implements Validatable {
        String name;
        Inner1 inner1;
        Inner2 inner2;
        transient boolean validated;

        public Outer(String name) {
            this.name = name;
        }

        @Override
        public void validate() throws InvalidMarshallableException {
            validated = true;
        }
    }

    private static class Inner1 extends SelfDescribingMarshallable {
    }

    private static class Inner2 extends SelfDescribingMarshallable {
    }
}
