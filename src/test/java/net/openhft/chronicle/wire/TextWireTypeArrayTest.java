/*
 * Copyright 2014-2020 chronicle.software
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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TextWireTypeArrayTest extends WireTestCommon {
    @Test
    public void shouldUnmarshalArrayOfType() {
        final Bytes<?> bytes = Bytes.elasticByteBuffer();

        final Wire wire = WireType.TEXT.apply(bytes);
        final HasClasses hasClasses = new HasClasses();
        wire.getValueOut().typedMarshallable(hasClasses);
        final String expected = "" +
                "!net.openhft.chronicle.wire.TextWireTypeArrayTest$HasClasses {\n" +
                "  classes: [ !type String, !type int, !type java.lang.Number ]\n" +
                "}\n";
        assertEquals(expected, bytes.toString());

        final TextWire textWire = TextWire.from(bytes.toString());
        final Object a = textWire.getValueIn().typedMarshallable();
        assertEquals(expected, a.toString());
        bytes.releaseLast();
    }

    static class HasClasses extends SelfDescribingMarshallable {
        Class<?>[] classes = {String.class, Integer.class, Number.class};
    }
}
