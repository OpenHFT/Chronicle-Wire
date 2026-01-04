/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.examples;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

/**
 * Demonstrates serialising and deserialising a {@link TextObject} using BinaryWire for example diagnostic output.
 */
public class WireExamples2 {

    /**
     * Entry point for the wire serialisation demonstration.
     *
     * @param args command-line arguments
     */
    public static void main(String... args) {
        // Add an alias for TextObject for ease of serialisation
        CLASS_ALIASES.addAlias(TextObject.class);

        // Initialize a new Binary Wire instance
        final Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        // Serialise a TextObject instance to the wire
        wire.getValueOut().object(new TextObject("SAMPLETEXT"));

        // Print the hexadecimal representation of the serialised data
        System.out.println("encoded to=" + wire.bytes().toHexString());

        // Deserialise the TextObject from the wire and print its value
        System.out.println("deserialized=" + wire.getValueIn().object());

    }

    /**
     * Represents a text object that internally uses Base64 encoding.
     * Extends {@link SelfDescribingMarshallable} to use its serialisation features.
     */
    static class TextObject extends SelfDescribingMarshallable {
        // Temporary buffer for conversion purposes
        final transient StringBuilder temp = new StringBuilder();

        // Represents the text in Base64 encoded format
        @LongConversion(Base64LongConverter.class)
        private final long text;

        /**
         * Initialise the {@link TextObject} with the given text value.
         *
         * @param text text value to encode
         */
        TextObject(CharSequence text) {
            this.text = Base64LongConverter.INSTANCE.parse(text);
        }

        /**
         * Return the original text from the Base64-encoded representation.
         *
         * @return original text as a character sequence
         */
        public CharSequence text() {
            Base64LongConverter.INSTANCE.append(temp, text);
            return temp;
        }
    }
}
