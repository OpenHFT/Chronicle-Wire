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
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static net.openhft.chronicle.wire.VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN;
import static org.junit.Assert.*;

/**
 * Smoke test that toggles codegen/proxy path via system property and verifies
 * events still round-trip through MethodWriter/Reader.
 */
@RunWith(Parameterized.class)
public class MethodWriterCodegenToggleTest extends WireTestCommon {

    interface API { void a(int x); void b(String s); }

    @Parameterized.Parameters(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[]{Boolean.TRUE}, new Object[]{Boolean.FALSE});
    }

    private final boolean disable;

    public MethodWriterCodegenToggleTest(boolean disable) {
        this.disable = disable;
    }

    @After
    public void clearProp() {
        System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
    }

    @Test
    public void roundTrip() {
        System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, String.valueOf(disable));
        // Some environments may fall back to proxy; ignore the warning.
        ignoreException("Falling back to proxy method writer");

        Wire w = new BinaryWire(Bytes.allocateElasticOnHeap(256));
        API writer = w.methodWriter(API.class);
        writer.a(7);
        writer.b("hi");

        List<String> seen = new ArrayList<>();
        MethodReader r = w.methodReader(new API() {
            @Override public void a(int x) { seen.add("a:" + x); }
            @Override public void b(String s) { seen.add("b:" + s); }
        });
        while (r.readOne()) { /* drain */ }
        assertEquals(2, seen.size());
        assertTrue(seen.get(0).startsWith("a:"));
        assertTrue(seen.get(1).startsWith("b:"));
    }
}

