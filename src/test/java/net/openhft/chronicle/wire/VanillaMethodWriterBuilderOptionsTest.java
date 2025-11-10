//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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

