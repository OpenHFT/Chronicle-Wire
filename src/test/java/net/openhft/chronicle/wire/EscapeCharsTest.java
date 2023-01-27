/*
 * Copyright 2016-2020 chronicle.software
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
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(value = Parameterized.class)
public class EscapeCharsTest extends WireTestCommon {
    @NotNull
    final String chs;
    private final Future future;

    public EscapeCharsTest(@NotNull String chs, Future future) {
        this.chs = chs;
        this.future = future;
    }

    @NotNull
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();
        ExecutorService forkJoinPool = Jvm.isDebug() ? Executors.newSingleThreadExecutor() :ForkJoinPool.commonPool();
        for (char i = 90; i < 300; i += 2) {
            char finalI = i;
            char ch1 = (char) (i + 1);
            Wire json = WireType.JSON_ONLY.apply(Bytes.allocateElasticDirect());
            Wire text = WireType.TEXT.apply(Bytes.allocateElasticDirect());
            Wire yaml = WireType.YAML_ONLY.apply(Bytes.allocateElasticDirect());

            list.add(new Object[]{"JSON (" + (int) i + ") " + i + ch1, forkJoinPool.submit(() -> testEscaped(finalI, json))});
            list.add(new Object[]{"TEXT (" + (int) i + ") " + i + ch1, forkJoinPool.submit(() -> testEscaped(finalI, text))});
            list.add(new Object[]{"YAML (" + +(int) i + ") " + i + ch1, forkJoinPool.submit(() -> testEscaped(finalI, yaml))});
        }
        return list;
    }

    static void testEscaped(char ch, @NotNull Wire wire) {
        wire.reset();
        char ch1 = (char) (ch + 1);
        wire.write("" + ch + ch1)
                .text("" + ch + ch1);
        wire.write("" + ch1 + ch)
                .text("" + ch1 + ch);

        @NotNull StringBuilder sb = new StringBuilder();
        @Nullable String s = wire.read(sb).text();
        assertEquals("key " + ch + ch1, "" + ch + ch1, sb.toString());
        assertEquals("value " + ch1 + ch, "" + ch + ch1, s);
        @Nullable String ss = wire.read(sb).text();
        assertEquals("key " + ch1 + ch, "" + ch1 + ch, sb.toString());
        assertEquals("value " + ch1 + ch, "" + ch1 + ch, ss);
    }

    @Test
    public void testEscaped() throws ExecutionException, InterruptedException {
        assertNull(future.get());
    }
}
