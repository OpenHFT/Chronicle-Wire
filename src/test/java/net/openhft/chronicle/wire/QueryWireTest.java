/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.bytes.NativeBytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by peter on 28/05/15.
 */
public class QueryWireTest {
    private NativeBytes<Void> bytes;

    @NotNull
    private QueryWire createWire() {
        bytes = nativeBytes();
        return new QueryWire(bytes);
    }

    @Test
    public void readWriteQuery() {
        @NotNull QueryWire wire = createWire();
        wire.write(() -> "bool").bool(true)
                .write(() -> "int").int64(12345)
                .write(() -> "text").text("Hello World")
                .write(() -> "float").float64(12.345);

        assertEquals("bool=true&int=12345&text=Hello World&float=12.345", bytes.toString());
        wire.read(() -> "bool").bool(this, (o, b) -> assertTrue(b))
                .read(() -> "int").int64(this, (o, i) -> assertEquals(12345, i))
                .read(() -> "text").text(this, (o, s) -> assertEquals("Hello World", s))
                .read(() -> "float").float64(this, (o, f) -> assertEquals(12.345, f, 0.0));
        @NotNull WireParser wp = WireParser.wireParser((s, v, o) -> System.err.println(s + " " + v.text()));
        @NotNull List<Object> results = new ArrayList<>();
        wp.register(() -> "bool", (s, v, o) -> v.bool(results, List::add));
        wp.register(() -> "int", (s, v, o) -> v.int64(results, List::add));
        wp.register(() -> "text", (s, v, o) -> v.text(results, List::add));
        wp.register(() -> "float", (s, v, o) -> v.float64(results, List::add));
        bytes.readPosition(0);
        while (bytes.readRemaining() > 0)
            wp.parseOne(wire, null);
        assertEquals(new ArrayList<>(Arrays.asList(true, 12345L, "Hello World", 12.345)), results);
    }
}
