/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.wire.BracketType;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class VanillaFieldInfoTest extends WireTestCommon {

    @Test
    @DisplayName("FieldInfo should read primitive and generic metadata")
    public void readsWritesPrimitiveAndGenericMetadata() throws Exception {
        Sample sample = new Sample();
        Field number = Sample.class.getDeclaredField("number");
        VanillaFieldInfo numberInfo = new VanillaFieldInfo("number", int.class, BracketType.NONE, number);
        numberInfo.set(sample, 17);
        assertEquals(17, numberInfo.getInt(sample), "Primitive field should read back value");
        assertTrue(numberInfo.isEqual(sample, sample), "Field should compare equal to itself");

        Field names = Sample.class.getDeclaredField("names");
        VanillaFieldInfo namesInfo = new VanillaFieldInfo("names", Iterable.class, BracketType.SEQ, names);
        assertEquals(String.class, namesInfo.genericType(0),
                "Generic type should resolve to String");

        // Clear cached field to exercise the reflective lookup branch
        Field fieldField = VanillaFieldInfo.class.getDeclaredField("field");
        fieldField.setAccessible(true);
        fieldField.set(numberInfo, null);
        assertEquals(17, numberInfo.getInt(sample),
                "Reflective lookup should still return primitive value");
    }

    @Test
    @DisplayName("FieldInfo equality should cover primitive and object fields")
    public void equalityCoversPrimitivesAndObjectPaths() throws Exception {
        Sample first = new Sample();
        Sample second = new Sample();
        Field ch = Sample.class.getDeclaredField("character");
        VanillaFieldInfo charInfo = new VanillaFieldInfo("character", char.class, BracketType.NONE, ch);
        charInfo.set(first, 'a');
        charInfo.set(second, 'a');
        assertTrue(charInfo.isEqual(first, second),
                "Primitive fields should compare equal");

        Field text = Sample.class.getDeclaredField("text");
        VanillaFieldInfo textInfo = new VanillaFieldInfo("text", String.class, BracketType.NONE, text);
        textInfo.set(first, "hello");
        textInfo.set(second, "hello");
        assertTrue(textInfo.isEqual(first, second),
                "Object fields should compare equal");
    }

    static class Sample {
        int number;
        char character;
        String text;
        java.util.List<String> names;
    }
}
