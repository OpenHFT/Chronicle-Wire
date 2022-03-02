/*
 * Copyright 2016-2020 chronicle.software
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
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class InnerMapTest extends WireTestCommon {

    @Test
    public void testMyInnnerMap() {
        @NotNull MyMarshable myMarshable = new MyMarshable().name("rob");
        myMarshable.commission().put("hello", 123.4);
        myMarshable.nested = new MyNested("text");

        String asString = myMarshable.toString();
        Assert.assertEquals("!net.openhft.chronicle.wire.InnerMapTest$MyMarshable {\n" +
                "  name: rob,\n" +
                "  commission: {\n" +
                "    hello: 123.4\n" +
                "  },\n" +
                "  nested: !net.openhft.chronicle.wire.InnerMapTest$MyNested {\n" +
                "    value: text\n" +
                "  }\n" +
                "}\n", asString);

        @SuppressWarnings("rawtypes")
        Bytes b = Bytes.elasticByteBuffer();
        @NotNull Wire w = new BinaryWire(b);     // works with text fails with binary
        try (DocumentContext dc = w.writingDocument(false)) {
            dc.wire().write("marshable").typedMarshallable(myMarshable);
        }

        try (DocumentContext dc = w.readingDocument()) {
            @Nullable MyMarshable tm = dc.wire().read(() -> "marshable").typedMarshallable();
            Assert.assertEquals(asString, tm.toString());
        }

        b.releaseLast();
    }

    static class MyMarshable extends SelfDescribingMarshallable implements Demarshallable {
        String name;
        Map<String, Double> commission;
        Marshallable nested;

        @UsedViaReflection
        public MyMarshable(@NotNull WireIn wire) {
            readMarshallable(wire);
        }

        public MyMarshable() {
            this.commission = new LinkedHashMap<>();
        }

        public String name() {
            return name;
        }

        public Map<String, Double> commission() {
            return commission;
        }

        @NotNull
        public MyMarshable name(String name) {
            this.name = name;
            return this;
        }
    }

    static class MyNested extends SelfDescribingMarshallable {
        String value;

        public MyNested(String value) {
            this.value = value;
        }
    }
}
