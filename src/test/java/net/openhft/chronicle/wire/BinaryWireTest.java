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

    final Boolean fixed;

    public BinaryWireTest(Boolean fixed) {
        this.fixed = fixed;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> fixed() {
        return Arrays.asList(new Boolean[]{true}, new Boolean[]{false});
    }

    Bytes bytes = new DirectStore(256).bytes();

    private BinaryWire createBytes() {
        bytes.clear();
        return new BinaryWire(bytes, fixed);
    }

    private String asText(Wire wire) {
        TextWire tw = new TextWire(new DirectStore(256).bytes());
        wire.copyTo(tw);
        tw.flip();
        wire.flip();
        return tw.toString();
    }

    enum BWKey implements WireKey {
        field1, field2, field3;

    }

    @Test
    public void testWrite() throws Exception {
        Wire wire = createBytes();
        wire.write();
        wire.write();
        wire.write();
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 3, cap: 256 ] ÀÀÀ");
        assertEquals("\"\": \"\": \"\": ", asText(wire));
    }

    private void checkWire(Wire wire, String expectedFixed) {
        checkWire(wire, expectedFixed, expectedFixed);
    }

    private void checkWire(Wire wire, String expectedFixed, String expectedVariable) {
        assertEquals(fixed ? expectedFixed : expectedVariable, wire.toString());
    }

    @Test
    public void testWrite1() throws Exception {
        Wire wire = createBytes();
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 21, cap: 256 ] Æfield1Æfield2Æfield3");
        assertEquals("field1: field2: field3: ", asText(wire));
    }

    @Test
    public void testWrite2() throws Exception {
        Wire wire = createBytes();
        wire.write("Hello", BWKey.field1);
        wire.write("World", BWKey.field2);
        wire.write("Long field name which is more than 32 characters, Bye", BWKey.field3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 67, cap: 256 ] ÅHelloÅWorld·5Long field name which is more than 32 characters, Bye");
        assertEquals("Hello: World: \"Long field name which is more than 32 characters, Bye\": ", asText(wire));
    }

    @Test
    public void testRead() throws Exception {
        Wire wire = createBytes();
        wire.write();
        wire.write(BWKey.field1);
        wire.write("Test", BWKey.field2);
        wire.flip();
        assertEquals("\"\": field1: Test: ", asText(wire));

        wire.read();
        wire.read();
        wire.read();
        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testRead1() throws Exception {
        Wire wire = createBytes();
        wire.write();
        wire.write(BWKey.field1);
        wire.write("Test", BWKey.field2);
        wire.flip();
        assertEquals("\"\": field1: Test: ", asText(wire));

        // ok as blank matches anything
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        // not a match
        try {
            wire.read(BWKey.field1);
            fail();
        } catch (UnsupportedOperationException expected) {
            wire.read(new StringBuilder(), BWKey.field1);
        }
        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testRead2() throws Exception {
        Wire wire = createBytes();
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
        assertEquals(BWKey.field1.name(), name.toString());

        wire.read(name, BWKey.field1);
        assertEquals(name1, name.toString());

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int8() {
        Wire wire = createBytes();
        wire.write().int8(1);
        wire.write(BWKey.field1).int8(2);
        wire.write("Test", BWKey.field2).int8(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 19, cap: 256 ] À¢⒈Æfield1¢⒉ÄTest¢⒊",
                "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊");
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", asText(wire));

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
        Wire wire = createBytes();
        wire.write().int16(1);
        wire.write(BWKey.field1).int16(2);
        wire.write("Test", BWKey.field2).int16(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 22, cap: 256 ] À£⒈٠Æfield1£⒉٠ÄTest£⒊٠",
                "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊");
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", asText(wire));

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
        Wire wire = createBytes();
        wire.write().uint8(1);
        wire.write(BWKey.field1).uint8(2);
        wire.write("Test", BWKey.field2).uint8(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 19, cap: 256 ] À¦⒈Æfield1¦⒉ÄTest¦⒊",
                "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊");
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", asText(wire));

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
        Wire wire = createBytes();
        wire.write().uint16(1);
        wire.write(BWKey.field1).uint16(2);
        wire.write("Test", BWKey.field2).uint16(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 22, cap: 256 ] À§⒈٠Æfield1§⒉٠ÄTest§⒊٠",
                "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊");
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", asText(wire));

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

    @Test
    public void uint32() {
        Wire wire = createBytes();
        wire.write().uint32(1);
        wire.write(BWKey.field1).uint32(2);
        wire.write("Test", BWKey.field2).uint32(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 28, cap: 256 ] À¨⒈٠٠٠Æfield1¨⒉٠٠٠ÄTest¨⒊٠٠٠",
                "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊");
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", asText(wire));

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
        Wire wire = createBytes();
        wire.write().int32(1);
        wire.write(BWKey.field1).int32(2);
        wire.write("Test", BWKey.field2).int32(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 28, cap: 256 ] À¤⒈٠٠٠Æfield1¤⒉٠٠٠ÄTest¤⒊٠٠٠",
                "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊");
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", asText(wire));

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
        Wire wire = createBytes();
        wire.write().int64(1);
        wire.write(BWKey.field1).int64(2);
        wire.write("Test", BWKey.field2).int64(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 40, cap: 256 ] À¥⒈٠٠٠٠٠٠٠Æfield1¥⒉٠٠٠٠٠٠٠ÄTest¥⒊٠٠٠٠٠٠٠",
                "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊");
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", asText(wire));

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
        Wire wire = createBytes();
        wire.write().float64(1);
        wire.write(BWKey.field1).float64(2);
        wire.write("Test", BWKey.field2).float64(3);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 40, cap: 256 ] À\u0091٠٠٠٠٠٠ð?Æfield1\u0091٠٠٠٠٠٠٠@ÄTest\u0091٠٠٠٠٠٠⒏@",
                "[pos: 0, lim: 16, cap: 256 ] À⒈Æfield1⒉ÄTest⒊");
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", asText(wire));

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
        Wire wire = createBytes();
        wire.write().text("Hello");
        wire.write(BWKey.field1).text("world");
        String name1 = "Long field name which is more than 32 characters, Bye";

        wire.write("Test", BWKey.field2).text(name1);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 80, cap: 256 ] ÀåHelloÆfield1åworldÄTest¸5" + name1);
        assertEquals("\"\": Hello\n" +
                "field1: world\n" +
                "Test: \"Long field name which is more than 32 characters, Bye\"\n", asText(wire));

        // ok as blank matches anything
        StringBuilder sb = new StringBuilder();
        Stream.of("Hello", "world", name1).forEach(e -> {
            wire.read().text(sb);
            assertEquals(e, sb.toString());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void type() {
        Wire wire = createBytes();
        wire.write().type("MyType");
        wire.write(BWKey.field1).type("AlsoMyType");
        String name1 = "com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState";
        wire.write("Test", BWKey.field2).type(name1);
        wire.flip();
        checkWire(wire, "[pos: 0, lim: 158, cap: 256 ] À¶⒍MyTypeÆfield1¶⒑AlsoMyTypeÄTest¶{" + name1);
        assertEquals("\"\": !MyType field1: !AlsoMyType Test: !com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState", asText(wire));

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

/*
    @Test
    public void testHasNextSequenceItem() throws Exception {

    }

    @Test
    public void testReadSequenceEnd() throws Exception {

    }

    @Test
    public void testWriteComment() throws Exception {

    }

    @Test
    public void testReadComment() throws Exception {

    }

    @Test
    public void testHasMapping() throws Exception {

    }

    @Test
    public void testWriteDocumentStart() throws Exception {

    }

    @Test
    public void testWriteDocumentEnd() throws Exception {

    }

    @Test
    public void testHasDocument() throws Exception {

    }

    @Test
    public void testReadDocumentStart() throws Exception {

    }

    @Test
    public void testReadDocumentEnd() throws Exception {

    }
*/
}