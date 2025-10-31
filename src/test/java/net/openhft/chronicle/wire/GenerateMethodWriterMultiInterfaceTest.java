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
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GenerateMethodWriterMultiInterfaceTest extends WireTestCommon {

    interface First { void one(int v); }
    interface Second { void two(String s); }

    @Test
    public void builderSupportsAdditionalInterfaces() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        @SuppressWarnings("unchecked")
        VanillaMethodWriterBuilder<First> builder = (VanillaMethodWriterBuilder<First>) wire.methodWriterBuilder(First.class);
        builder.addInterface(Second.class);

        First writer = builder.build();
        ((Second) writer).two("b");
        writer.one(1);

        List<String> seen = new ArrayList<>();
        MethodReader reader = wire.methodReader(new First() {
            @Override public void one(int v) { seen.add("one:" + v); }
        }, new Second() {
            @Override public void two(String s) { seen.add("two:" + s); }
        });
        while (reader.readOne()) {
            // drain
        }
        assertEquals(2, seen.size());
        assertTrue(seen.get(0).startsWith("two:"));
        assertTrue(seen.get(1).startsWith("one:"));
    }
}

