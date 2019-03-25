/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.*;

public class MarshallableTest {
    @Test(expected = IOException.class)
    public void fromFile() throws IOException {
        fail("Got " + Marshallable.fromFile("empty-file.yaml"));
    }

    @Test
    public void testEmptyFromString() {
        assertEquals("", Marshallable.fromString(""));
    }

    @Ignore("Undefined behaviour")
    @Test(expected = IllegalArgumentException.class)
    public void testFromString2() {
        Object o = Marshallable.fromString("\"");
        assertNotNull(o);
    }

    @Ignore("Undefined behaviour")
    @Test(expected = IllegalArgumentException.class)
    public void testFromString3() {
        Object o = Marshallable.fromString("'");
        assertNotNull(o);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testBytesMarshallable() {
        @NotNull Marshallable m = new MyTypes();

        @NotNull Bytes bytes = nativeBytes();
        assertTrue(bytes.isElastic());
        @NotNull TextWire wire = new TextWire(bytes);
        m.writeMarshallable(wire);

        m.readMarshallable(wire);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testEquals() {
        @NotNull final Bytes bytes = nativeBytes();
        assertTrue(bytes.isElastic());
        @NotNull final MyTypes source = new MyTypes();
        //change default value fields in order to let destination to be changed from its default values too
        source.b(true);
        source.s((short) 1);
        source.d(1.0);
        source.l(1L);
        source.i(1);
        source.text("a");
        @NotNull final Marshallable destination = new MyTypes();
        assertNotEquals(source, destination);
        @NotNull final TextWire wire = new TextWire(bytes);
        source.writeMarshallable(wire);
        destination.readMarshallable(wire);
        assertEquals(source, destination);
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    @Test
    public void testCopy() {
        DTO2 dto2 = new DTO2();
        dto2.one = RetentionPolicy.CLASS;
        dto2.two = Arrays.asList(1L, 22L);
        dto2.three = "2018-11-02";

        String s = WireType.TEXT.asString(dto2);
        System.out.println(s);
        DTO1 dto1 = WireType.TEXT.fromString(DTO1.class, s);
        assertEquals("!net.openhft.chronicle.wire.MarshallableTest$DTO1 {\n" +
                "  one: CLASS,\n" +
                "  two: [\n" +
                "    1,\n" +
                "    22\n" +
                "  ],\n" +
                "  three: 2018-11-02\n" +
                "}\n", WireType.TEXT.asString(dto1));
    }

    static class DTO1 {
        String one;
        List<Integer> two;
        LocalDate three;
    }

    static class DTO2 {
        RetentionPolicy one;
        List<Long> two;
        String three;
    }

}
