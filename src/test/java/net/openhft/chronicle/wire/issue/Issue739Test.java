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

package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * see https://github.com/OpenHFT/Chronicle-Wire/issues/739
 */
public class Issue739Test extends WireTestCommon {

    public static class One extends SelfDescribingMarshallable {
        String text;

        public One(String text) {
            this.text = text;
        }
    }

    public static class Two extends SelfDescribingMarshallable {
        String text;

        public Two(String text) {
            this.text = text;
        }
    }

    public static class Three extends SelfDescribingMarshallable {
        private One one;
        private Two two;
        private One twoAndHalf;
    }

    public static class IThree extends SelfDescribingMarshallable {
        private Marshallable one;
        private Marshallable two;
        private Marshallable twoAndHalf;

    }

    @Test
    public void fieldAnchorAlias() {
        Wire wire = new YamlWire(Bytes.wrapForRead(("three: !net.openhft.chronicle.wire.issue.Issue739Test$Three\n" +
                "  one: &first\n" +
                "    text: hello\n" +
                "  two:\n" +
                "    text: world\n" +
                "  twoAndHalf: *first\n").getBytes()));
        Three three = (Three) wire.getValueIn().<Map<String, Object>>typedMarshallable().get("three");
        assertEquals("hello", three.one.text);
        assertEquals("world", three.two.text);
        assertEquals("hello", three.twoAndHalf.text);
        assertSame(three.one, three.twoAndHalf);
    }

    @Test
    public void interfaceFieldAnchorAlias() {
        Wire wire = new YamlWire(Bytes.wrapForRead(("three: !net.openhft.chronicle.wire.issue.Issue739Test$IThree\n" +
                "  one: &first !net.openhft.chronicle.wire.issue.Issue739Test$One\n" +
                "    text: hello\n" +
                "  two: !net.openhft.chronicle.wire.issue.Issue739Test$Two\n" +
                "    text: world\n" +
                "  twoAndHalf: *first\n").getBytes()));
        IThree three = (IThree) wire.getValueIn().<Map<String, Object>>typedMarshallable().get("three");
        assertEquals("hello", ((One)three.one).text);
        assertEquals("world", ((Two)three.two).text);
        assertEquals("hello", ((One)three.twoAndHalf).text);
        assertSame(three.one, three.twoAndHalf);
    }
}
