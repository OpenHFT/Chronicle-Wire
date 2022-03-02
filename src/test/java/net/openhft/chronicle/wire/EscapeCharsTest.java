/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.assertEquals;

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
        for (char i = 0; i < 300; i += 2) {
            char finalI = i;
            char ch1 = (char) (i + 1);
            list.add(new Object[]{"" + i + ch1, ForkJoinPool.commonPool().submit(() -> testEscaped(finalI))});
        }
        return list;
    }

    static void testEscaped(char ch) {
        char ch1 = (char) (ch + 1);
        @NotNull Wire wire = createWire();
        wire.write("" + ch + ch1).text("" + ch + ch1);
        wire.write("" + ch1 + ch).text("" + ch1 + ch);

        @NotNull StringBuilder sb = new StringBuilder();
        @Nullable String s = wire.read(sb).text();
        assertEquals("key " + ch + ch1, "" + ch + ch1, sb.toString());
        assertEquals("value " + ch1 + ch, "" + ch + ch1, s);
        @Nullable String ss = wire.read(sb).text();
        assertEquals("key " + ch1 + ch, "" + ch1 + ch, sb.toString());
        assertEquals("value " + ch1 + ch, "" + ch1 + ch, ss);

        wire.bytes().releaseLast();
    }

    @NotNull
    static TextWire createWire() {
        return new TextWire(nativeBytes());
    }

    @Test
    public void testEscaped() throws ExecutionException, InterruptedException {
        future.get();
    }
}
