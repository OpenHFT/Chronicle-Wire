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

/**
 * Demonstrates the process of serializing and deserializing `TextObject` using the BinaryWire library.
 */
public class WireExamples2 {

    /**
     * Entry point for the demonstration.
     *
     * @param args Command-line arguments.
     */
    public static void main(String... args) {
        // Add an alias for `TextObject` for ease of serialization
        CLASS_ALIASES.addAlias(TextObject.class);

        // Initialize a new Binary Wire instance
        final Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        // Serialize a `TextObject` instance to the wire
        wire.getValueOut().object(new TextObject("SAMPLETEXT"));

        // Print the hexadecimal representation of the serialized data
        System.out.println("encoded to=" + wire.bytes().toHexString());

        // Deserialize the `TextObject` from the wire and print its value
        System.out.println("deserialized=" + wire.getValueIn().object());

    }

    /**
     * Represents a text object that internally uses a Base64 encoding.
     * Extends the `SelfDescribingMarshallable` to utilize its serialization features.
     */
    public static class TextObject extends SelfDescribingMarshallable {
        // Temporary buffer for conversion purposes
        transient StringBuilder temp = new StringBuilder();

        // Represents the text in Base64 encoded format
        @LongConversion(Base64LongConverter.class)
        private long text;

        /**
         * Constructor to initialize the `TextObject` with the given text.
         *
         * @param text Text to initialize the object with.
         */
        public TextObject(CharSequence text) {
            this.text = Base64LongConverter.INSTANCE.parse(text);
        }

        /**
         * Retrieves the original text from the Base64 encoded representation.
         *
         * @return Original text as CharSequence.
         */
        public CharSequence text() {
            Base64LongConverter.INSTANCE.append(temp, text);
            return temp;
        }
    }
}
