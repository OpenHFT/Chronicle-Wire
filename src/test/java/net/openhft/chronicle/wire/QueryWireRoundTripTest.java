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

import static org.junit.Assert.*;

public class QueryWireRoundTripTest extends WireTestCommon {

    @Test
    public void writesAndReadsQueryParameters() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        QueryWire wire = new QueryWire(bytes);

        wire.write("name").text("bob");
        wire.write("age").int32(42);
        wire.write("flag").bool(true);

        String query = bytes.toString();
        assertTrue("query should contain key/value pairs", query.contains("name=bob"));

        QueryWire reader = new QueryWire(Bytes.from(query));
        assertEquals("bob", reader.read("name").text());
        assertEquals(42, reader.read("age").int32());
        assertTrue(reader.read("flag").bool());
    }
}

