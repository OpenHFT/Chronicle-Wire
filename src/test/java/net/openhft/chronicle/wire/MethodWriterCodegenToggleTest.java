/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static net.openhft.chronicle.wire.VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test that toggles codegen/proxy path via system property and verifies
 * events still round-trip through MethodWriter/Reader.
 */
public class MethodWriterCodegenToggleTest extends WireTestCommon {

    private boolean disable;

    public void initMethodWriterCodegenToggleTest(boolean disable) {
        this.disable = disable;
    }

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[]{Boolean.TRUE}, new Object[]{Boolean.FALSE});
    }

    @AfterEach
    public void clearProp() {
        System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
    }

    @MethodSource("data")
    @ParameterizedTest(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    public void roundTrip(boolean disable) {
        initMethodWriterCodegenToggleTest(disable);
        System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, String.valueOf(disable));
        // Some environments may fall back to proxy; ignore the warning.
        ignoreException("Falling back to proxy method writer");

        Wire w = new BinaryWire(Bytes.allocateElasticOnHeap(256));
        API writer = w.methodWriter(API.class);
        writer.a(7);
        writer.b("hi");

        List<String> seen = new ArrayList<>();
        MethodReader r = w.methodReader(new API() {
            @Override
            public void a(int x) {
                seen.add("a:" + x);
            }

            @Override
            public void b(String s) {
                seen.add("b:" + s);
            }
        });
        while (r.readOne()) {
            continue;
        }
        assertEquals(2, seen.size());
        assertTrue(seen.get(0).startsWith("a:"));
        assertTrue(seen.get(1).startsWith("b:"));
    }

    interface API {
        void a(int x);

        void b(String s);
    }
}
