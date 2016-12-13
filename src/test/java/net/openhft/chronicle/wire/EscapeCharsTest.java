/*
 * Copyright 2016 higherfrequencytrading.com
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

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 13/12/16.
 */
@RunWith(value = Parameterized.class)
public class EscapeCharsTest {
    final Character ch;

    public EscapeCharsTest(Character ch) {
        this.ch = ch;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        List<Object[]> list = new ArrayList<>();
        for (char i = 0; i < 260; i++) {
            list.add(new Object[]{i});
        }
        return list;
    }

    @Test
    public void testEscaped() {
        Wire wire = createWire();
        wire.write("" + ch).text("" + ch);
        wire.write("" + ch + ch).text("" + ch + ch);

        StringBuilder sb = new StringBuilder();
        String s = wire.read(sb).text();
        assertEquals("key " + ch, "" + ch, sb.toString());
        assertEquals("value " + ch, "" + ch, s);
        String ss = wire.read(sb).text();
        assertEquals("key " + ch + ch, "" + ch + ch, sb.toString());
        assertEquals("value " + ch + ch, "" + ch + ch, ss);
    }

    @NotNull
    private TextWire createWire() {
        return new TextWire(nativeBytes());
    }
}
