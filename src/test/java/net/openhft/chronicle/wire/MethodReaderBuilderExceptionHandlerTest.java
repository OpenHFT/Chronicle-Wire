/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.onoes.ExceptionHandler;
import net.openhft.chronicle.core.util.IgnoresEverything;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class MethodReaderBuilderExceptionHandlerTest extends WireTestCommon {
    static final String input = "" +
            "---\n" +
            "a: a1\n" +
            "...\n" +
            "---\n" +
            "b: b1\n" +
            "...\n" +
            "---\n" +
            "c: c1\n" +
            "...\n" +
            "---\n" +
            "a: a2\n" +
            "...\n" +
            "---\n" +
            "b: b2\n" +
            "...\n" +
            "---\n" +
            "c: c2\n" +
            "...\n";

    interface _A {
        void a(String text);
    }

    interface _B {
        void b(String text);
    }

    interface _C {
        void c(String text);
    }

    interface _BC extends _B, _C {
    }

    @Test
    public void testNothing() {
        doTest("" +
                        "# false\n",
                ExceptionHandler.ignoresEverything(), IgnoresEverything.class);
    }

    @Test
    public void testA() {
        doTest("" +
                        "a[a1]\n" +
                        "# true\n" +
                        "a[a2]\n" +
                        "# true\n" +
                        "# false\n",
                ExceptionHandler.ignoresEverything(), _A.class);
    }

    @Test
    public void testBC() {
        doTest("" +
                        "b[b1]\n" +
                        "# true\n" +
                        "c[c1]\n" +
                        "# true\n" +
                        "b[b2]\n" +
                        "# true\n" +
                        "c[c2]\n" +
                        "# true\n" +
                        "# false\n",
                ExceptionHandler.ignoresEverything(), _BC.class);
    }

    @Test
    public void testBCWarn() {
        expectException("Unknown method-name='a'");
        doTest("" +
                        "b[b1]\n" +
                        "# true\n" +
                        "c[c1]\n" +
                        "# true\n" +
                        "b[b2]\n" +
                        "# true\n" +
                        "c[c2]\n" +
                        "# true\n" +
                        "# false\n",
                Jvm.warn(), _BC.class);
    }

    private void doTest(String expected, ExceptionHandler eh, Class type) {
        @NotNull StringWriter out = new StringWriter();
        Wire wire = WireType.YAML_ONLY.apply(Bytes.from(input));
        MethodReader reader = wire
                .methodReaderBuilder()
                .exceptionHandlerOnUnknownMethod(eh)
                .build(Mocker.logging(type, "", out));
        while (!wire.isEmpty()) {
            boolean read = reader.readOne();
            out.append("# ").append(Boolean.toString(read)).append("\n");
        }
        assertEquals(expected, out.toString().replace("\r", ""));
    }
}
