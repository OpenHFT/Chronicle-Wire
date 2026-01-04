/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.wire.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for validating the behaviour of field converters with long data type values.
 */
class LongConvertorFieldsTest {

    /**
     * DTO class representing data fields to be serialized using Base16 encoding.
     */
    static class Base16DTO extends SelfDescribingMarshallable {
        @Base16
        final
        byte b;
        @Base16
        final
        char ch;
        @Base16
        final
        short s;
        @Base16
        final
        int i;
        @Base16
        final
        long l;

        /**
         * Constructor to initialize the Base16DTO fields.
         *
         * @param b  Byte value.
         * @param ch Character value.
         * @param s  Short value.
         * @param i  Int value.
         * @param l  Long value.
         */
        Base16DTO(byte b, char ch, short s, int i, long l) {
            this.b = b;
            this.ch = ch;
            this.s = s;
            this.i = i;
            this.l = l;
        }
    }

    /**
     * Test for verifying the Base16 encoding functionality.
     */
    @Test
    @DisplayName("Serialises Base16 fields using expected hex widths")
    void base16() {
        // Validate Base16 encoding with a range of positive values
        assertEquals("!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base16DTO {\n" +
                "  b: 1,\n" +
                "  ch: 32,\n" +
                "  s: 3,\n" +
                "  i: 4,\n" +
                "  l: 5\n" +
                "}\n", doTest(new Base16DTO((byte) 1, '2', (short) 3, 4, 5)), "base16 converter should serialize small positive values as compact hexadecimal without leading zeros");
        // Validate Base16 encoding with maximum negative values
        // Note: shorter types yield shorter strings, not all ffffffffffffffff
        assertEquals("!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base16DTO {\n" +
                "  b: ff,\n" +
                "  ch: ffff,\n" +
                "  s: ffff,\n" +
                "  i: ffffffff,\n" +
                "  l: ffffffffffffffff\n" +
                "}\n", doTest(new Base16DTO((byte) -1, (char) -1, (short) -1, -1, -1)), "base16 converter should serialize negative values with hexadecimal width matching primitive type size, not uniform padding");
    }

    /**
     * DTO class representing data fields to be serialized using Base64 encoding.
     */
    static class Base64DTO extends SelfDescribingMarshallable {
        @Base64
        final
        byte b;
        @Base64
        final
        char ch;
        @Base64
        final
        short s;
        @Base64
        final
        int i;
        @Base64
        final
        long l;

        /**
         * Constructor to initialize the Base64DTO fields.
         *
         * @param b  Byte value.
         * @param ch Character value.
         * @param s  Short value.
         * @param i  Int value.
         * @param l  Long value.
         */
        Base64DTO(byte b, char ch, short s, int i, long l) {
            this.b = b;
            this.ch = ch;
            this.s = s;
            this.i = i;
            this.l = l;
        }
    }

    /**
     * Test for verifying the Base64 encoding functionality.
     */
    @Test
    @DisplayName("Serialises Base64 fields using expected symbol widths")
    void base64() {
        // Validate Base64 encoding with a range of positive values
        assertEquals("!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base64DTO {\n" +
                "  b: A,\n" +
                "  ch: x,\n" +
                "  s: C,\n" +
                "  i: D,\n" +
                "  l: E\n" +
                "}\n", doTest(new Base64DTO((byte) 1, '2', (short) 3, 4, 5)), "base64 converter should serialize small positive values as single-character base64 strings");
        // Validate Base64 encoding with maximum negative values
        // Note: shorter types yield shorter strings, not all ffffffffffffffff
        assertEquals("!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base64DTO {\n" +
                "  b: C_,\n" +
                "  ch: O__,\n" +
                "  s: O__,\n" +
                "  i: C_____,\n" +
                "  l: O__________\n" +
                "}\n", doTest(new Base64DTO((byte) -1, (char) -1, (short) -1, -1, -1)), "base64 converter should serialize negative values with underscore padding proportional to primitive type width");
    }

    /**
     * DTO class representing data fields to be serialized using Base85 encoding.
     */
    static class Base85DTO extends SelfDescribingMarshallable {
        @Base85
        final
        byte b;
        @Base85
        final
        char ch;
        @Base85
        final
        short s;
        @Base85
        final
        int i;
        @Base85
        final
        long l;

        /**
         * Constructor to initialize the Base85DTO fields.
         *
         * @param b  Byte value.
         * @param ch Character value.
         * @param s  Short value.
         * @param i  Int value.
         * @param l  Long value.
         */
        Base85DTO(byte b, char ch, short s, int i, long l) {
            this.b = b;
            this.ch = ch;
            this.s = s;
            this.i = i;
            this.l = l;
        }
    }

    /**
     * Test for verifying the Base85 encoding functionality.
     */
    @Test
    @DisplayName("Serialises Base85 fields with variable length text")
    void base85() {
        // Validate Base85 encoding with a range of positive values
        assertEquals("!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base85DTO {\n" +
                "  b: 1,\n" +
                "  ch: g,\n" +
                "  s: 3,\n" +
                "  i: 4,\n" +
                "  l: 5\n" +
                "}\n", doTest(new Base85DTO((byte) 1, '2', (short) 3, 4, 5)), "base85 converter should serialize small positive values as compact single-character strings");
        // Validate Base85 encoding with maximum negative values
        // Note: the encoded values for negative numbers are not straightforward like Base16 and Base64
        assertEquals("!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base85DTO {\n" +
                "  b: 30,\n" +
                "  ch: 960,\n" +
                "  s: 960,\n" +
                "  i: .Gk<0,\n" +
                "  l: +ko2&)z.H0\n" +
                "}\n", doTest(new Base85DTO((byte) -1, (char) -1, (short) -1, -1, -1)), "base85 converter should serialize negative values as variable-length strings that grow with primitive type width");
    }

    @Test
    @DisplayName("Round-trips Base85 special character values")
    void detectSpecialCharBase85() {
        final String CHARS = "0123456789" +
                ":;<=>?@" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ_" +
                "abcdefghijklmnopqrstuvwxyz" +
                "\"#$%&'()*+,-./ ";
        LongConverter c = Base85LongConverter.INSTANCE;
        for (int i = 0; i < 85; i++) {
            char ch = CHARS.charAt(i);
            Base85DTO dto = new Base85DTO((byte) i, (char) i, (short) c.parse("0" + ch), (int) c.parse(ch + "a"), c.parse(ch + " "));
            assertEquals(dto, Marshallable.fromString(dto.toString()),
                    "base85 converter should preserve all 85 character combinations in roundtrip serialisation at index " + i + ", char=" + ch);
        }
    }

    static class ShortTextDTO extends SelfDescribingMarshallable {
        @ShortText
        final
        byte b;
        @ShortText
        final
        char ch;
        @ShortText
        final
        short s;
        @ShortText
        final
        int i;
        @ShortText
        final
        long l;

        ShortTextDTO(byte b, char ch, short s, int i, long l) {
            this.b = b;
            this.ch = ch;
            this.s = s;
            this.i = i;
            this.l = l;
        }
    }

    @Test
    @DisplayName("Serialises ShortText fields with expected formatting")
    void shortText() {
        assertEquals("!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$ShortTextDTO {\n" +
                "  b: 1,\n" +
                "  ch: g,\n" +
                "  s: 3,\n" +
                "  i: 4,\n" +
                "  l: 5\n" +
                "}\n", doTest(new ShortTextDTO((byte) 1, '2', (short) 3, 4, 5)), "shorttext converter should serialize small positive values as unquoted single characters");
        // note shorter types are shorter strings and not all ffffffffffffffff
        assertEquals("!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$ShortTextDTO {\n" +
                "  b: \"3 \",\n" +
                "  ch: \"96 \",\n" +
                "  s: \"96 \",\n" +
                "  i: \".Gk< \",\n" +
                "  l: \"+ko2&)z.H \"\n" +
                "}\n", doTest(new ShortTextDTO((byte) -1, (char) -1, (short) -1, -1, -1)), "shorttext converter should serialize negative values as quoted strings with trailing space, length growing with primitive type width");
    }

    @Test
    @DisplayName("Round-trips ShortText special character values")
    void detectSpecialChar() {
        final String CHARS = " " +
                "123456789" +
                ":;<=>?@" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ_" +
                "abcdefghijklmnopqrstuvwxyz" +
                "\"#$%&'()*+,-./0";
        LongConverter c = ShortTextLongConverter.INSTANCE;
        for (int i = 0; i < 85; i++) {
            char ch = CHARS.charAt(i);
            ShortTextDTO dto = new ShortTextDTO((byte) i, (char) i, (short) c.parse("0" + ch), (int) c.parse(ch + "a"), c.parse(ch + " "));
            assertEquals(dto, Marshallable.fromString(dto.toString()),
                    "shorttext converter should preserve all 85 character combinations in roundtrip serialisation at index " + i + ", char=" + ch);
        }
    }

    static class WordsDTO extends SelfDescribingMarshallable {
        @Words
        final
        byte b;
        @Words
        final
        char ch;
        @Words
        final
        short s;
        @Words
        final
        int i;
        @Words
        final
        long l;

        /**
         * Constructor to initialize the WordsDTO fields.
         *
         * @param b  Byte value.
         * @param ch Character value.
         * @param s  Short value.
         * @param i  Int value.
         * @param l  Long value.
         */
        WordsDTO(byte b, char ch, short s, int i, long l) {
            this.b = b;
            this.ch = ch;
            this.s = s;
            this.i = i;
            this.l = l;
        }
    }

    /**
     * Test method for verifying the Words encoding functionality.
     */
    @Test
    @DisplayName("Serialises Words fields into readable tokens")
    void words() {
        // Validate Words encoding with a range of positive values.
        // The expected results are arbitrary word mappings for demonstration.
        assertEquals("!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$WordsDTO {\n" +
                "  b: aid,\n" +
                "  ch: joy,\n" +
                "  s: air,\n" +
                "  i: all,\n" +
                "  l: and\n" +
                "}\n", doTest(new WordsDTO((byte) 1, '2', (short) 3, 4, 5)), "words converter should encode small positive values as single human-readable dictionary words");

        // Validate Words encoding with maximum negative values.
        // Note: shorter types yield different word combinations based on the negative value.
        assertEquals("!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$WordsDTO {\n" +
                "  b: corn,\n" +
                "  ch: writer.eight,\n" +
                "  s: writer.eight,\n" +
                "  i: writer.writer.among,\n" +
                "  l: writer.writer.writer.writer.writer.leg\n" +
                "}\n", doTest(new WordsDTO((byte) -1, (char) -1, (short) -1, -1, -1)), "words converter should encode negative values as dot-separated word chains that grow longer with primitive type width");
    }

    /**
     * Helper method to serialize a DTO object using the YamlWire format,
     * and validate the result against an expected string representation.
     *
     * @param dto      The object to be serialized.
     * @param expected The expected string representation of the serialized object.
     */
    private String doTest(Marshallable dto) {
        Wire wire = new YamlWire();
        wire.getValueOut().object(dto);
        String actual = wire.toString();
        Object object = wire.getValueIn().object();
        assertEquals(dto, object, "long converters should preserve exact values during yaml roundtrip serialization");
        return actual;
    }
}
