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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class BinaryWireNestedDocumentTest extends WireTestCommon {

    @Test
    public void roundTripsNestedMarshallable() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("outer").marshallable(m ->
                    m.write("inner").marshallable(n -> n.write("value").text("hi")));
        }

        bytes.readPositionRemaining(0, bytes.writePosition());
        try (DocumentContext dc = wire.readingDocument()) {
            Map<String, Object> outer = dc.wire().read("outer").marshallableAsMap(String.class, Object.class);
            assertTrue(outer.containsKey("inner"));
            @SuppressWarnings("unchecked")
            Map<String, Object> inner = (Map<String, Object>) outer.get("inner");
            assertEquals("hi", inner.get("value"));
        }
    }
}

