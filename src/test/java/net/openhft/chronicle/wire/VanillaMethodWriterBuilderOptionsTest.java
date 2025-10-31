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

public class VanillaMethodWriterBuilderOptionsTest extends WireTestCommon {

    interface Events { void event(String s); }

    @Test
    public void honoursUpdateInterceptorAndThreadSafeToggle() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        @SuppressWarnings("unchecked")
        VanillaMethodWriterBuilder<Events> builder = (VanillaMethodWriterBuilder<Events>) wire.methodWriterBuilder(Events.class);
        builder.disableThreadSafe(true);
        builder.updateInterceptor((name, arg) -> !"skip".equals(arg));

        Events writer = builder.build();
        writer.event("skip");   // suppressed
        writer.event("keep");   // forwarded

        List<String> seen = new ArrayList<>();
        MethodReader reader = wire.methodReader((Events) seen::add);
        while (reader.readOne()) {
            // drain
        }
        assertEquals(1, seen.size());
        assertEquals("keep", seen.get(0));
    }
}

