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

package net.openhft.chronicle.wire.examples;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

public class WireExamples2 {

    public static void main(String... args) {
        CLASS_ALIASES.addAlias(TextObject.class);
        final Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        // serialize
        wire.getValueOut().object(new TextObject("SAMPLETEXT"));

        // log out the encoded data
        System.out.println("encoded to=" + wire.bytes().toHexString());

        // deserialize
        System.out.println("deserialized=" + wire.getValueIn().object());

    }

    public static class TextObject extends SelfDescribingMarshallable {
        transient StringBuilder temp = new StringBuilder();

        @LongConversion(Base64LongConverter.class)
        private long text;

        public TextObject(CharSequence text) {
            this.text = Base64LongConverter.INSTANCE.parse(text);
        }

        public CharSequence text() {
            Base64LongConverter.INSTANCE.append(temp, text);
            return temp;
        }
    }
}
