/*
 * Copyright 2014-2018 Chronicle Software
 *
 * http://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

public class TextWireTypeArrayTest {
    static class Person extends AbstractMarshallable {
        Class<?>[] classes = {Object.class, Object.class};
    }

    @Test
    public void shouldUnmarshalArrayOfType() {
        final Bytes<?> bytes = Wires.acquireBytes();

        final Wire wire = WireType.TEXT.apply(bytes);
        final Person person = new Person();
        wire.getValueOut().typedMarshallable(person);
        System.err.println(bytes.toString());

        final TextWire textWire = TextWire.from(bytes.toString());
        textWire.getValueIn().typedMarshallable();
    }
}
