/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
@SuppressFBWarnings(
        value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
        justification = "Fields are populated via Wire marshalling in tests.")
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
    @DisplayName("Method writer codegen toggle round-trips")
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
        assertEquals(2, seen.size(), "Reader should dispatch two calls");
        assertTrue(seen.get(0).startsWith("a:"), "First call should start with the a: marker");
        assertTrue(seen.get(1).startsWith("b:"), "Second call should start with the b: marker");
    }

    interface API {
        void a(int x);

        void b(String s);
    }
}
