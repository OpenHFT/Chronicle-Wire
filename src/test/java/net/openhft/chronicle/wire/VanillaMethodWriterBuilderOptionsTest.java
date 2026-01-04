/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
public class VanillaMethodWriterBuilderOptionsTest extends WireTestCommon {

    @Test
    @DisplayName("Honours update interceptor and thread-safe toggle")
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
            continue;
        }
        assertEquals(1, seen.size(), "Reader should keep only the forwarded event");
        assertEquals("keep", seen.get(0), "Reader should forward only the keep event");
    }

    interface Events {
        void event(String s);
    }
}
