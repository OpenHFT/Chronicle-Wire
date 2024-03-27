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
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;

@RunWith(value = Parameterized.class)
public class UnicodeStringTest extends WireTestCommon {
    @NotNull
    static Bytes<?> bytes = nativeBytes();
    static Wire wire = createWire();
    static char[] chars = new char[128];
    private final char ch;

    public UnicodeStringTest(char ch) {
        this.ch = ch;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        List<Object[]> chars = new ArrayList<>();
        int a = 1, b = 1;
        while (a < Character.MAX_VALUE) {
            int i = a++;
            a = b;
            b += i;
            if (!Character.isValidCodePoint(i))
                continue;
            chars.add(new Object[]{(char) i});
        }
        for (int ch : new int[]{0x0, 0x7F, 0x80, 0x07FF, 0x800, 0xFFFF})
            chars.add(new Object[]{(char) ch});
        return chars;
    }

    @NotNull
    private static BinaryWire createWire() {
        bytes.clear();
        final boolean fixed = true;
        final boolean numericField = false;
        final boolean fieldLess = false;
        final int compressedSize = 128;
        @NotNull BinaryWire wire = new BinaryWire(bytes, fixed, numericField, fieldLess, compressedSize, "lzw", false);

        return wire;
    }

    @AfterClass
    public static void release() {
        bytes.releaseLast();
    }

    @Test
    public void testLongString() {
        wire.clear();
        Arrays.fill(chars, ch);
        @NotNull String s = new String(chars);
        wire.writeDocument(false, w -> w.write(() -> "msg").text(s));

       // System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));
        wire.readDocument(null, w -> w.read(() -> "msg").text(s, Assert::assertEquals));
    }
}
