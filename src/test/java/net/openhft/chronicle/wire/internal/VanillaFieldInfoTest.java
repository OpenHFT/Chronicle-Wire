/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.wire.BracketType;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

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
        assertSame(String.class, namesInfo.genericType(0));

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
