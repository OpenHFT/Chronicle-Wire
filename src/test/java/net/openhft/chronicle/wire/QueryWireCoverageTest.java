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
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class QueryWireCoverageTest extends WireTestCommon {

    @Test
    public void writesAndReadsQueryFragments() throws InvalidMarshallableException {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        QueryWire writer = new QueryWire(bytes);

        writer.write("flag").bool(true);
        writer.write("count").int64(42);
        writer.write("name").text("alpha beta");
        writer.write("raw").rawBytes("tail".getBytes(StandardCharsets.ISO_8859_1));
        writer.write("payload").bytes(new byte[]{1, 2, 3});
        writer.write("seq").sequence(new String[]{"ignored"}, (ignored, out) -> {
            out.text("one");
            out.text("two");
        });
        writer.write("obj").marshallable(out -> out.write("k").text("v"));

        String query = bytes.toString();
        assertTrue(query.contains("flag=true"));
        assertTrue(query.contains("count=42"));
        assertTrue(query.contains("raw=tail"));
        assertTrue(query.contains("payload="));
        assertTrue(query.contains("seq=[one,two,]"));

        bytes.readPositionRemaining(0, bytes.writePosition());
        QueryWire reader = new QueryWire(bytes);

        assertEquals("true", reader.read("flag").text());
        assertEquals(42L, reader.read("count").int64());
        assertEquals("alpha beta", reader.read("name").text());

        Bytes<?> payload = Bytes.allocateElasticOnHeap();
        reader.read("payload").textTo(payload);
        assertEquals("AQID", payload.toString());
        payload.releaseLast();

        List<String> items = new ArrayList<>();
        reader.read("seq").sequence(items, (target, valueIn) -> {
            do {
                target.add(valueIn.text());
            } while (valueIn.hasNextSequenceItem());
        });
        List<String> expected = new ArrayList<>();
        expected.add("one");
        expected.add("two");
        assertEquals(expected, items);

        AtomicReference<String> nested = new AtomicReference<>();
        reader.read("obj").marshallable(in -> nested.set(in.read("k").text()));
        assertEquals("v", nested.get());

        bytes.releaseLast();
    }
}
