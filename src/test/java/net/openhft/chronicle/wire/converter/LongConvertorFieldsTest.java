package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for validating the behavior of field converters with long data types.
 */
public class LongConvertorFieldsTest {

    /**
     * DTO class representing data fields to be serialized using Base16 encoding.
     */
    static class Base16DTO extends SelfDescribingMarshallable {
        @Base16
        byte b;
        @Base16
        char ch;
        @Base16
        short s;
        @Base16
        int i;
        @Base16
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
        public Base16DTO(byte b, char ch, short s, int i, long l) {
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
    public void base16() {
        // Validate Base16 encoding with a range of positive values
        doTest(new Base16DTO((byte) 1, '2', (short) 3, 4, 5), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base16DTO {\n" +
                "  b: 1,\n" +
                "  ch: 32,\n" +
                "  s: 3,\n" +
                "  i: 4,\n" +
                "  l: 5\n" +
                "}\n");
        // Validate Base16 encoding with maximum negative values
        // Note: shorter types yield shorter strings, not all ffffffffffffffff
        doTest(new Base16DTO((byte) -1, (char) -1, (short) -1, -1, -1), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base16DTO {\n" +
                "  b: ff,\n" +
                "  ch: ffff,\n" +
                "  s: ffff,\n" +
                "  i: ffffffff,\n" +
                "  l: ffffffffffffffff\n" +
                "}\n");
    }

    /**
     * DTO class representing data fields to be serialized using Base64 encoding.
     */
    static class Base64DTO extends SelfDescribingMarshallable {
        @Base64
        byte b;
        @Base64
        char ch;
        @Base64
        short s;
        @Base64
        int i;
        @Base64
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
        public Base64DTO(byte b, char ch, short s, int i, long l) {
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
    public void base64() {
        // Validate Base64 encoding with a range of positive values
        doTest(new Base64DTO((byte) 1, '2', (short) 3, 4, 5), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base64DTO {\n" +
                "  b: A,\n" +
                "  ch: x,\n" +
                "  s: C,\n" +
                "  i: D,\n" +
                "  l: E\n" +
                "}\n");
        // Validate Base64 encoding with maximum negative values
        // Note: shorter types yield shorter strings, not all ffffffffffffffff
        doTest(new Base64DTO((byte) -1, (char) -1, (short) -1, -1, -1), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base64DTO {\n" +
                "  b: C_,\n" +
                "  ch: O__,\n" +
                "  s: O__,\n" +
                "  i: C_____,\n" +
                "  l: O__________\n" +
                "}\n");
    }

    /**
     * DTO class representing data fields to be serialized using Base85 encoding.
     */
    static class Base85DTO extends SelfDescribingMarshallable {
        @Base85
        byte b;
        @Base85
        char ch;
        @Base85
        short s;
        @Base85
        int i;
        @Base85
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
        public Base85DTO(byte b, char ch, short s, int i, long l) {
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
    public void base85() {
        // Validate Base85 encoding with a range of positive values
        doTest(new Base85DTO((byte) 1, '2', (short) 3, 4, 5), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base85DTO {\n" +
                "  b: 1,\n" +
                "  ch: g,\n" +
                "  s: 3,\n" +
                "  i: 4,\n" +
                "  l: 5\n" +
                "}\n");
        // Validate Base85 encoding with maximum negative values
        // Note: the encoded values for negative numbers are not straightforward like Base16 and Base64
        doTest(new Base85DTO((byte) -1, (char) -1, (short) -1, -1, -1), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base85DTO {\n" +
                "  b: 30,\n" +
                "  ch: 960,\n" +
                "  s: 960,\n" +
                "  i: .Gk<0,\n" +
                "  l: +ko2&)z.H0\n" +
                "}\n");
    }

    @Test
    public void detectSpecialCharBase85() {
        final String CHARS = "" +
                "0123456789" +
                ":;<=>?@" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ_" +
                "abcdefghijklmnopqrstuvwxyz" +
                "\"#$%&'()*+,-./ ";
        LongConverter c = Base85LongConverter.INSTANCE;
        for (int i = 0; i < 85; i++) {
            char ch = CHARS.charAt(i);
            Base85DTO dto = new Base85DTO((byte) i, (char) i, (short) c.parse("0" + ch), (int) c.parse(ch + "a"), c.parse(ch + " "));
            assertEquals(dto, Marshallable.fromString(dto.toString()));
        }
    }

    static class ShortTextDTO extends SelfDescribingMarshallable {
        @ShortText
        byte b;
        @ShortText
        char ch;
        @ShortText
        short s;
        @ShortText
        int i;
        @ShortText
        long l;

        public ShortTextDTO(byte b, char ch, short s, int i, long l) {
            this.b = b;
            this.ch = ch;
            this.s = s;
            this.i = i;
            this.l = l;
        }
    }

    @Test
    public void shortText() {
        doTest(new ShortTextDTO((byte) 1, '2', (short) 3, 4, 5), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$ShortTextDTO {\n" +
                "  b: 1,\n" +
                "  ch: g,\n" +
                "  s: 3,\n" +
                "  i: 4,\n" +
                "  l: 5\n" +
                "}\n");
        // note shorter types are shorter strings and not all ffffffffffffffff
        doTest(new ShortTextDTO((byte) -1, (char) -1, (short) -1, -1, -1), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$ShortTextDTO {\n" +
                "  b: \"3 \",\n" +
                "  ch: \"96 \",\n" +
                "  s: \"96 \",\n" +
                "  i: \".Gk< \",\n" +
                "  l: \"+ko2&)z.H \"\n" +
                "}\n");
    }

    @Test
    public void detectSpecialChar() {
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
            assertEquals(dto, Marshallable.fromString(dto.toString()));
        }
    }

    static class WordsDTO extends SelfDescribingMarshallable {
        @Words
        byte b;
        @Words
        char ch;
        @Words
        short s;
        @Words
        int i;
        @Words
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
        public WordsDTO(byte b, char ch, short s, int i, long l) {
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
    public void words() {
        // Validate Words encoding with a range of positive values.
        // The expected results are arbitrary word mappings for demonstration.
        doTest(new WordsDTO((byte) 1, '2', (short) 3, 4, 5), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$WordsDTO {\n" +
                "  b: aid,\n" +
                "  ch: joy,\n" +
                "  s: air,\n" +
                "  i: all,\n" +
                "  l: and\n" +
                "}\n");

        // Validate Words encoding with maximum negative values.
        // Note: shorter types yield different word combinations based on the negative value.
        doTest(new WordsDTO((byte) -1, (char) -1, (short) -1, -1, -1), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$WordsDTO {\n" +
                "  b: corn,\n" +
                "  ch: writer.eight,\n" +
                "  s: writer.eight,\n" +
                "  i: writer.writer.among,\n" +
                "  l: writer.writer.writer.writer.writer.leg\n" +
                "}\n");
    }

    /**
     * Helper method to serialize a DTO object using the YamlWire format,
     * and validate the result against an expected string representation.
     *
     * @param dto      The object to be serialized.
     * @param expected The expected string representation of the serialized object.
     */
    private void doTest(Marshallable dto, String expected) {
        Wire wire = new YamlWire(Bytes.allocateElasticOnHeap());
        wire.getValueOut().object(dto);
        assertEquals(expected, wire.toString());
        Object object = wire.getValueIn().object();
        assertEquals(dto, object);
    }
}
