package net.openhft.chronicle.wire;

import net.openhft.lang.io.Bytes;
import net.openhft.lang.io.DirectStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(value = Parameterized.class)
public class BinaryWireTest {

    final int testId;
    final boolean fixed;
    final boolean numericField;
    final boolean fieldLess;

    public BinaryWireTest(int testId, boolean fixed, boolean numericField, boolean fieldLess) {
        this.testId = testId;
        this.fixed = fixed;
        this.numericField = numericField;
        this.fieldLess = fieldLess;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        return Arrays.asList(
                new Object[]{0, false, false, false},
                new Object[]{1, true, false, false},
                new Object[]{2, false, true, false},
                new Object[]{3, true, true, false},
                new Object[]{4, false, false, true},
                new Object[]{5, true, false, true}
        );
    }

    Bytes bytes = new DirectStore(256).bytes();

    private BinaryWire createWire() {
        bytes.clear();
        return new BinaryWire(bytes, fixed, numericField, fieldLess);
    }


    enum BWKey implements WireKey {
        field1(1), field2(2), field3(3);
        private final int code;

        BWKey(int code) {
            this.code = code;
        }

        @Override
        public int code() {
            return code;
        }
    }

    @Test
    public void testWrite() throws Exception {
        Wire wire = createWire();
        wire.write();
        wire.write();
        wire.write();
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 3, cap: 256 ] ÀÀÀ",
                "[pos: 0, lim: 3, cap: 256 ] ÀÀÀ",
                "[pos: 0, lim: 3, cap: 256 ] ÀÀÀ",
                "[pos: 0, lim: 3, cap: 256 ] ÀÀÀ",
                "[pos: 0, lim: 0, cap: 256 ] ",
                "[pos: 0, lim: 0, cap: 256 ] ");

        assertEquals(fieldLess ? "" : "\"\": \"\": \"\": ", TextWire.asText(wire));
    }

    private void checkWire(Wire wire, String... expected) {
        assertEquals("id: " + testId, expected[testId], wire.toString());
    }

    @Test
    public void testWrite1() throws Exception {
        Wire wire = createWire();
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 21, cap: 256 ] Æfield1Æfield2Æfield3",
                "[pos: 0, lim: 21, cap: 256 ] Æfield1Æfield2Æfield3",
                "[pos: 0, lim: 6, cap: 256 ] ¹⒈¹⒉¹⒊",
                "[pos: 0, lim: 6, cap: 256 ] ¹⒈¹⒉¹⒊",
                "[pos: 0, lim: 0, cap: 256 ] ",
                "[pos: 0, lim: 0, cap: 256 ] ");
        checkAsText(wire,
                "field1: field2: field3: ",
                "1: 2: 3: ",
                "");
    }

    @Test
    public void testWrite2() throws Exception {
        Wire wire = createWire();
        wire.write("Hello", BWKey.field1);
        wire.write("World", BWKey.field2);
        String name = "Long field name which is more than 32 characters, Bye";
        wire.write(name, BWKey.field3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 67, cap: 256 ] ÅHelloÅWorld·5" + name,
                "[pos: 0, lim: 67, cap: 256 ] ÅHelloÅWorld·5" + name,
                "[pos: 0, lim: 67, cap: 256 ] ÅHelloÅWorld·5" + name,
                "[pos: 0, lim: 67, cap: 256 ] ÅHelloÅWorld·5" + name,
                "[pos: 0, lim: 0, cap: 256 ] ",
                "[pos: 0, lim: 0, cap: 256 ] ");
        assertEquals(fieldLess ? "" : "Hello: World: \"" + name + "\": ", TextWire.asText(wire));
    }

    @Test
    public void testRead() throws Exception {
        Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        wire.write("Test", BWKey.field2);
        wire.flip();
        checkAsText(wire, "\"\": field1: Test: ",
                "\"\": 1: Test: ",
                "");

        wire.read();
        wire.read();
        wire.read();
        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testRead1() throws Exception {
        Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        wire.write("Test", BWKey.field2);
        wire.flip();
        checkAsText(wire, "\"\": field1: Test: ",
                "\"\": 1: Test: ",
                "");

        // ok as blank matches anything
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        // not a match
        try {
            wire.read(BWKey.field1);
            if (!fieldLess) fail();
        } catch (UnsupportedOperationException expected) {
            wire.read(new StringBuilder(), BWKey.field1);
        }
        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testRead2() throws Exception {
        Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        String name1 = "Long field name which is more than 32 characters, Bye";
        wire.write(name1, BWKey.field3);
        wire.flip();

        // ok as blank matches anything
        StringBuilder name = new StringBuilder();
        wire.read(name, BWKey.field1);
        assertEquals(0, name.length());

        wire.read(name, BWKey.field1);
        assertEquals(numericField || fieldLess ? "" : BWKey.field1.name(), name.toString());

        wire.read(name, BWKey.field1);
        assertEquals(fieldLess ? "" : name1, name.toString());

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int8() {
        Wire wire = createWire();
        wire.write().int8(1);
        wire.write(BWKey.field1).int8(2);
        wire.write("Test", BWKey.field2).int8(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊",
                "[pos: 0, lim: 19, cap: 256 ] À¢⒈Æfield1¢⒉ÄTest¢⒊",
                "[pos: 0, lim: 11, cap: 256 ] À⒈¹⒈⒉ÄTest⒊",
                "[pos: 0, lim: 14, cap: 256 ] À¢⒈¹⒈¢⒉ÄTest¢⒊",
                "[pos: 0, lim: 3, cap: 256 ] ⒈⒉⒊",
                "[pos: 0, lim: 6, cap: 256 ] ¢⒈¢⒉¢⒊");
        checkAsText123(wire);

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int8(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    @Test
    public void int16() {
        Wire wire = createWire();
        wire.write().int16(1);
        wire.write(BWKey.field1).int16(2);
        wire.write("Test", BWKey.field2).int16(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊",
                "[pos: 0, lim: 22, cap: 256 ] À£⒈٠Æfield1£⒉٠ÄTest£⒊٠",
                "[pos: 0, lim: 11, cap: 256 ] À⒈¹⒈⒉ÄTest⒊",
                "[pos: 0, lim: 17, cap: 256 ] À£⒈٠¹⒈£⒉٠ÄTest£⒊٠",
                "[pos: 0, lim: 3, cap: 256 ] ⒈⒉⒊",
                "[pos: 0, lim: 9, cap: 256 ] £⒈٠£⒉٠£⒊٠");
        checkAsText123(wire);

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int16(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    @Test
    public void uint8() {
        Wire wire = createWire();
        wire.write().uint8(1);
        wire.write(BWKey.field1).uint8(2);
        wire.write("Test", BWKey.field2).uint8(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊",
                "[pos: 0, lim: 19, cap: 256 ] À¦⒈Æfield1¦⒉ÄTest¦⒊",
                "[pos: 0, lim: 11, cap: 256 ] À⒈¹⒈⒉ÄTest⒊",
                "[pos: 0, lim: 14, cap: 256 ] À¦⒈¹⒈¦⒉ÄTest¦⒊",
                "[pos: 0, lim: 3, cap: 256 ] ⒈⒉⒊",
                "[pos: 0, lim: 6, cap: 256 ] ¦⒈¦⒉¦⒊");
        checkAsText123(wire);

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint8(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    @Test
    public void uint16() {
        Wire wire = createWire();
        wire.write().uint16(1);
        wire.write(BWKey.field1).uint16(2);
        wire.write("Test", BWKey.field2).uint16(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊",
                "[pos: 0, lim: 22, cap: 256 ] À§⒈٠Æfield1§⒉٠ÄTest§⒊٠",
                "[pos: 0, lim: 11, cap: 256 ] À⒈¹⒈⒉ÄTest⒊",
                "[pos: 0, lim: 17, cap: 256 ] À§⒈٠¹⒈§⒉٠ÄTest§⒊٠",
                "[pos: 0, lim: 3, cap: 256 ] ⒈⒉⒊",
                "[pos: 0, lim: 9, cap: 256 ] §⒈٠§⒉٠§⒊٠");
        checkAsText123(wire);

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint16(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    private void checkAsText123(Wire wire) {
        checkAsText(wire, "\"\": 1\n" +
                        "field1: 2\n" +
                        "Test: 3\n",
                "\"\": 1\n" +
                        "1: 2\n" +
                        "Test: 3\n",
                "1\n" +
                        "2\n" +
                        "3\n"
        );
    }

    private void checkAsText(Wire wire, String textFieldExcepted, String numberFieldExpected, String fieldLessExpected) {
        String text = TextWire.asText(wire);
        if (fieldLess)
            assertEquals(fieldLessExpected, text);
        else if (numericField)
            assertEquals(numberFieldExpected, text);
        else
            assertEquals(textFieldExcepted, text);
    }

    @Test
    public void uint32() {
        Wire wire = createWire();
        wire.write().uint32(1);
        wire.write(BWKey.field1).uint32(2);
        wire.write("Test", BWKey.field2).uint32(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊",
                "[pos: 0, lim: 28, cap: 256 ] À¨⒈٠٠٠Æfield1¨⒉٠٠٠ÄTest¨⒊٠٠٠",
                "[pos: 0, lim: 11, cap: 256 ] À⒈¹⒈⒉ÄTest⒊",
                "[pos: 0, lim: 23, cap: 256 ] À¨⒈٠٠٠¹⒈¨⒉٠٠٠ÄTest¨⒊٠٠٠",
                "[pos: 0, lim: 3, cap: 256 ] ⒈⒉⒊",
                "[pos: 0, lim: 15, cap: 256 ] ¨⒈٠٠٠¨⒉٠٠٠¨⒊٠٠٠");
        checkAsText123(wire);

        // ok as blank matches anything
        AtomicLong i = new AtomicLong();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint32(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    @Test
    public void int32() {
        Wire wire = createWire();
        wire.write().int32(1);
        wire.write(BWKey.field1).int32(2);
        wire.write("Test", BWKey.field2).int32(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊",
                "[pos: 0, lim: 28, cap: 256 ] À¤⒈٠٠٠Æfield1¤⒉٠٠٠ÄTest¤⒊٠٠٠",
                "[pos: 0, lim: 11, cap: 256 ] À⒈¹⒈⒉ÄTest⒊",
                "[pos: 0, lim: 23, cap: 256 ] À¤⒈٠٠٠¹⒈¤⒉٠٠٠ÄTest¤⒊٠٠٠",
                "[pos: 0, lim: 3, cap: 256 ] ⒈⒉⒊",
                "[pos: 0, lim: 15, cap: 256 ] ¤⒈٠٠٠¤⒉٠٠٠¤⒊٠٠٠");
        checkAsText123(wire);

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int32(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    @Test
    public void int64() {
        Wire wire = createWire();
        wire.write().int64(1);
        wire.write(BWKey.field1).int64(2);
        wire.write("Test", BWKey.field2).int64(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊",
                "[pos: 0, lim: 40, cap: 256 ] À¥⒈٠٠٠٠٠٠٠Æfield1¥⒉٠٠٠٠٠٠٠ÄTest¥⒊٠٠٠٠٠٠٠",
                "[pos: 0, lim: 11, cap: 256 ] À⒈¹⒈⒉ÄTest⒊",
                "[pos: 0, lim: 35, cap: 256 ] À¥⒈٠٠٠٠٠٠٠¹⒈¥⒉٠٠٠٠٠٠٠ÄTest¥⒊٠٠٠٠٠٠٠",
                "[pos: 0, lim: 3, cap: 256 ] ⒈⒉⒊",
                "[pos: 0, lim: 27, cap: 256 ] ¥⒈٠٠٠٠٠٠٠¥⒉٠٠٠٠٠٠٠¥⒊٠٠٠٠٠٠٠");
        checkAsText123(wire);

        // ok as blank matches anything
        AtomicLong i = new AtomicLong();
        LongStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int64(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    @Test
    public void float64() {
        Wire wire = createWire();
        wire.write().float64(1);
        wire.write(BWKey.field1).float64(2);
        wire.write("Test", BWKey.field2).float64(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊",
                "[pos: 0, lim: 40, cap: 256 ] À\u0091٠٠٠٠٠٠ð?Æfield1\u0091٠٠٠٠٠٠٠@ÄTest\u0091٠٠٠٠٠٠⒏@",
                "[pos: 0, lim: 11, cap: 256 ] À⒈¹⒈⒉ÄTest⒊",
                "[pos: 0, lim: 35, cap: 256 ] À\u0091٠٠٠٠٠٠ð?¹⒈\u0091٠٠٠٠٠٠٠@ÄTest\u0091٠٠٠٠٠٠⒏@",
                "[pos: 0, lim: 3, cap: 256 ] ⒈⒉⒊",
                "[pos: 0, lim: 27, cap: 256 ] \u0091٠٠٠٠٠٠ð?\u0091٠٠٠٠٠٠٠@\u0091٠٠٠٠٠٠⒏@");
        checkAsText123(wire);

        // ok as blank matches anything
        class Floater {
            double f;

            public void set(double d) {
                f = d;
            }
        }
        Floater n = new Floater();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().float64(n::set);
            assertEquals(e, n.f, 0.0);
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void text() {
        String name = "Long field name which is more than 32 characters, Bye";

        Wire wire = createWire();
        wire.write().text("Hello");
        wire.write(BWKey.field1).text("world");
        wire.write("Test", BWKey.field2).text(name);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 80, cap: 256 ] ÀåHelloÆfield1åworldÄTest¸5" + name,
                "[pos: 0, lim: 80, cap: 256 ] ÀåHelloÆfield1åworldÄTest¸5" + name,
                "[pos: 0, lim: 75, cap: 256 ] ÀåHello¹⒈åworldÄTest¸5" + name,
                "[pos: 0, lim: 75, cap: 256 ] ÀåHello¹⒈åworldÄTest¸5" + name,
                "[pos: 0, lim: 67, cap: 256 ] åHelloåworld¸5" + name,
                "[pos: 0, lim: 67, cap: 256 ] åHelloåworld¸5" + name);
        checkAsText(wire, "\"\": Hello\n" +
                        "field1: world\n" +
                        "Test: \"" + name + "\"\n",
                "\"\": Hello\n" +
                        "1: world\n" +
                        "Test: \"" + name + "\"\n",
                "Hello\n" +
                        "world\n" +
                        "\"" + name + "\"\n");

        // ok as blank matches anything
        StringBuilder sb = new StringBuilder();
        Stream.of("Hello", "world", name).forEach(e -> {
            wire.read().text(sb);
            assertEquals(e, sb.toString());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void type() {
        Wire wire = createWire();
        wire.write().type("MyType");
        wire.write(BWKey.field1).type("AlsoMyType");
        String name1 = "com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState";
        wire.write("Test", BWKey.field2).type(name1);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 158, cap: 256 ] À¶⒍MyTypeÆfield1¶⒑AlsoMyTypeÄTest¶{" + name1,
                "[pos: 0, lim: 158, cap: 256 ] À¶⒍MyTypeÆfield1¶⒑AlsoMyTypeÄTest¶{" + name1,
                "[pos: 0, lim: 153, cap: 256 ] À¶⒍MyType¹⒈¶⒑AlsoMyTypeÄTest¶{" + name1,
                "[pos: 0, lim: 153, cap: 256 ] À¶⒍MyType¹⒈¶⒑AlsoMyTypeÄTest¶{" + name1,
                "[pos: 0, lim: 145, cap: 256 ] ¶⒍MyType¶⒑AlsoMyType¶{" + name1,
                "[pos: 0, lim: 145, cap: 256 ] ¶⒍MyType¶⒑AlsoMyType¶{" + name1);
        checkAsText(wire, "\"\": !MyType field1: !AlsoMyType Test: !" + name1,
                "\"\": !MyType 1: !AlsoMyType Test: !" + name1,
                "!MyType !AlsoMyType !" + name1);

        // ok as blank matches anything
        StringBuilder sb = new StringBuilder();
        Stream.of("MyType", "AlsoMyType", name1).forEach(e -> {
            wire.read().type(sb);
            assertEquals(e, sb.toString());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }
}