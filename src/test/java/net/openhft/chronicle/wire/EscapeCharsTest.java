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
    final Character ch;
    final Future future;

    public EscapeCharsTest(@NotNull Character ch, Future future) {
        this.ch = ch;
        this.future = future;
    }

    @NotNull
    @Parameterized.Parameters(name = "char = {0}")
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();
        for (char i = 36; i < 260; i++) {
            char finalI = i;
            list.add(new Object[]{i, ForkJoinPool.commonPool().submit(() -> testEscaped(finalI))});
        }
        return list;
    }

    @Test
    public void testEscaped() throws ExecutionException, InterruptedException {
        future.get();
    }

    static void testEscaped(char ch) {
        @NotNull Wire wire = createWire();
        wire.write("" + ch).text("" + ch);
        wire.write("" + ch + ch).text("" + ch + ch);

        @NotNull StringBuilder sb = new StringBuilder();
        @Nullable String s = wire.read(sb).text();
        assertEquals("key " + ch, "" + ch, sb.toString());
        assertEquals("value " + ch, "" + ch, s);
        @Nullable String ss = wire.read(sb).text();
        assertEquals("key " + ch + ch, "" + ch + ch, sb.toString());
        assertEquals("value " + ch + ch, "" + ch + ch, ss);

        wire.bytes().releaseLast();
    }

    @NotNull
    static TextWire createWire() {
        return new TextWire(nativeBytes());
    }
}
