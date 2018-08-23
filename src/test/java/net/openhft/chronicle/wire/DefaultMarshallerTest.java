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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/*
 * Created by Peter Lawrey on 16/03/16.
 */
public class DefaultMarshallerTest {

    @Test
    public void testDeserializeWithNestedArray() {
        ClassAliasPool.CLASS_ALIASES.addAlias(NestedEnum.class);
        DMOuterClassWithEmbeddedArray dmOuterClass = ObjectUtils.newInstance(DMOuterClassWithEmbeddedArray.class);

        @NotNull DMOuterClassWithEmbeddedArray oc = new DMOuterClassWithEmbeddedArray("words");
        oc.enums = new NestedEnum[3];
        oc.enums[0] = NestedEnum.ONE;
        oc.enums[1] = NestedEnum.TWO;
        oc.enums[2] = NestedEnum.THREE;

        assertEquals("!net.openhft.chronicle.wire.DefaultMarshallerTest$DMOuterClassWithEmbeddedArray {\n" +
                "  str: words,\n" +
                "  enums: [ ONE, TWO, THREE ]\n" +
                "}\n", oc.toString());

        @NotNull Wire text = new TextWire(Bytes.elasticHeapByteBuffer(128));
        oc.writeMarshallable(text);

        @NotNull DMOuterClassWithEmbeddedArray oc2 = new DMOuterClassWithEmbeddedArray();
        oc2.readMarshallable(text);

        assertEquals(oc, oc2);

        text.bytes().release();
    }

    @Test
    public void testDeserialize() {
        ClassAliasPool.CLASS_ALIASES.addAlias(DMNestedClass.class);
        DMOuterClass dmOuterClass = ObjectUtils.newInstance(DMOuterClass.class);
        assertNotNull(dmOuterClass.nested);

        @NotNull DMOuterClass oc = new DMOuterClass("words", true, (byte) 1, 2, 3, 4, 5, (short) 6);
        oc.nested.add(new DMNestedClass("hi", 111));
        oc.nested.add(new DMNestedClass("bye", 999));
        oc.map.put("key", new DMNestedClass("value", 1));
        oc.map.put("keyz", new DMNestedClass("valuez", 1111));

        assertEquals("!net.openhft.chronicle.wire.DefaultMarshallerTest$DMOuterClass {\n" +
                "  text: words,\n" +
                "  b: true,\n" +
                "  bb: 1,\n" +
                "  s: 6,\n" +
                "  f: 3.0,\n" +
                "  d: 2.0,\n" +
                "  l: 5,\n" +
                "  i: 4,\n" +
                "  nested: [\n" +
                "    { str: hi, num: 111 },\n" +
                "    { str: bye, num: 999 }\n" +
                "  ],\n" +
                "  map: {\n" +
                "    key: { str: value, num: 1 },\n" +
                "    keyz: { str: valuez, num: 1111 }\n" +
                "  }\n" +
                "}\n", oc.toString());

        @NotNull Wire text = new TextWire(Bytes.elasticHeapByteBuffer(64));
        oc.writeMarshallable(text);

        @NotNull DMOuterClass oc2 = new DMOuterClass();
        oc2.readMarshallable(text);

        assertEquals(oc, oc2);

        text.bytes().release();
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    static enum NestedEnum {
        ONE,
        TWO,
        THREE;
    }

    static class DMOuterClass extends AbstractMarshallable {
        String text;
        boolean b;
        byte bb;
        short s;
        float f;
        double d;
        long l;
        int i;
        @NotNull
        List<DMNestedClass> nested = new ArrayList<>();
        @NotNull
        Map<String, DMNestedClass> map = new LinkedHashMap<>();

        DMOuterClass() {

        }

        public DMOuterClass(String text, boolean b, byte bb, double d, float f, int i, long l, short s) {
            this.text = text;
            this.b = b;
            this.bb = bb;
            this.d = d;
            this.f = f;
            this.i = i;
            this.l = l;
            this.s = s;
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }
    }

    static class DMNestedClass extends AbstractMarshallable {
        String str;
        int num;

        public DMNestedClass(String str, int num) {
            this.str = str;
            this.num = num;
        }
    }

    static class DMOuterClassWithEmbeddedArray extends AbstractMarshallable {
        String str;
        NestedEnum[] enums;

        DMOuterClassWithEmbeddedArray() {

        }

        DMOuterClassWithEmbeddedArray(String s) {
            this.str = s;
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

    }

}
