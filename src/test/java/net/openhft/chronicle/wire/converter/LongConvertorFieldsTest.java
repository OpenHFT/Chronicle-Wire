package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LongConvertorFieldsTest {
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

        public Base16DTO(byte b, char ch, short s, int i, long l) {
            this.b = b;
            this.ch = ch;
            this.s = s;
            this.i = i;
            this.l = l;
        }
    }

    @Test
    public void base16() {
        doTest(new Base16DTO((byte) 1, '2', (short) 3, 4, 5), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base16DTO {\n" +
                "  b: 1,\n" +
                "  ch: 32,\n" +
                "  s: 3,\n" +
                "  i: 4,\n" +
                "  l: 5\n" +
                "}\n");
        // note shorter types are shorter strings and not all ffffffffffffffff
        doTest(new Base16DTO((byte) -1, (char) -1, (short) -1, -1, -1), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base16DTO {\n" +
                "  b: ff,\n" +
                "  ch: ffff,\n" +
                "  s: ffff,\n" +
                "  i: ffffffff,\n" +
                "  l: ffffffffffffffff\n" +
                "}\n");
    }

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

        public Base64DTO(byte b, char ch, short s, int i, long l) {
            this.b = b;
            this.ch = ch;
            this.s = s;
            this.i = i;
            this.l = l;
        }
    }

    @Test
    public void base64() {
        doTest(new Base64DTO((byte) 1, '2', (short) 3, 4, 5), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base64DTO {\n" +
                "  b: A,\n" +
                "  ch: x,\n" +
                "  s: C,\n" +
                "  i: D,\n" +
                "  l: E\n" +
                "}\n");
        // note shorter types are shorter strings and not all ffffffffffffffff
        doTest(new Base64DTO((byte) -1, (char) -1, (short) -1, -1, -1), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base64DTO {\n" +
                "  b: C_,\n" +
                "  ch: O__,\n" +
                "  s: O__,\n" +
                "  i: C_____,\n" +
                "  l: O__________\n" +
                "}\n");
    }

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

        public Base85DTO(byte b, char ch, short s, int i, long l) {
            this.b = b;
            this.ch = ch;
            this.s = s;
            this.i = i;
            this.l = l;
        }
    }

    @Test
    public void base85() {
        doTest(new Base85DTO((byte) 1, '2', (short) 3, 4, 5), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$Base85DTO {\n" +
                "  b: 1,\n" +
                "  ch: g,\n" +
                "  s: 3,\n" +
                "  i: 4,\n" +
                "  l: 5\n" +
                "}\n");
        // note shorter types are shorter strings and not all ffffffffffffffff
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

        public WordsDTO(byte b, char ch, short s, int i, long l) {
            this.b = b;
            this.ch = ch;
            this.s = s;
            this.i = i;
            this.l = l;
        }
    }

    @Test
    public void words() {
        doTest(new WordsDTO((byte) 1, '2', (short) 3, 4, 5), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$WordsDTO {\n" +
                "  b: aid,\n" +
                "  ch: joy,\n" +
                "  s: air,\n" +
                "  i: all,\n" +
                "  l: and\n" +
                "}\n");
        // note shorter types are shorter strings and not all ffffffffffffffff
        doTest(new WordsDTO((byte) -1, (char) -1, (short) -1, -1, -1), "" +
                "!net.openhft.chronicle.wire.converter.LongConvertorFieldsTest$WordsDTO {\n" +
                "  b: corn,\n" +
                "  ch: writer.eight,\n" +
                "  s: writer.eight,\n" +
                "  i: writer.writer.among,\n" +
                "  l: writer.writer.writer.writer.writer.leg\n" +
                "}\n");
    }

    private void doTest(Marshallable dto, String expected) {
        Wire wire = new YamlWire(Bytes.allocateElasticOnHeap());
        wire.getValueOut().object(dto);
        assertEquals(expected, wire.toString());
        Object object = wire.getValueIn().object();
        assertEquals(dto, object);
    }
}
