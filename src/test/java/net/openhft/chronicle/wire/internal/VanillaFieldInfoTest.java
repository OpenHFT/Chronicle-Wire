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
package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.wire.BracketType;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class VanillaFieldInfoTest extends WireTestCommon {

    @Test
    public void readsWritesPrimitiveAndGenericMetadata() throws Exception {
        Sample sample = new Sample();
        Field number = Sample.class.getDeclaredField("number");
        VanillaFieldInfo numberInfo = new VanillaFieldInfo("number", int.class, BracketType.NONE, number);
        numberInfo.set(sample, 17);
        assertEquals(17, numberInfo.getInt(sample));
        assertTrue(numberInfo.isEqual(sample, sample));

        Field names = Sample.class.getDeclaredField("names");
        VanillaFieldInfo namesInfo = new VanillaFieldInfo("names", Iterable.class, BracketType.SEQ, names);
        assertEquals(String.class, namesInfo.genericType(0));

        // Clear cached field to exercise the reflective lookup branch
        Field fieldField = VanillaFieldInfo.class.getDeclaredField("field");
        fieldField.setAccessible(true);
        fieldField.set(numberInfo, null);
        assertEquals(17, numberInfo.getInt(sample));
    }

    @Test
    public void equalityCoversPrimitivesAndObjectPaths() throws Exception {
        Sample first = new Sample();
        Sample second = new Sample();
        Field ch = Sample.class.getDeclaredField("character");
        VanillaFieldInfo charInfo = new VanillaFieldInfo("character", char.class, BracketType.NONE, ch);
        charInfo.set(first, 'a');
        charInfo.set(second, 'a');
        assertTrue(charInfo.isEqual(first, second));

        Field text = Sample.class.getDeclaredField("text");
        VanillaFieldInfo textInfo = new VanillaFieldInfo("text", String.class, BracketType.NONE, text);
        textInfo.set(first, "hello");
        textInfo.set(second, "hello");
        assertTrue(textInfo.isEqual(first, second));
    }

    static class Sample {
        int number;
        char character;
        String text;
        java.util.List<String> names;
    }
}
