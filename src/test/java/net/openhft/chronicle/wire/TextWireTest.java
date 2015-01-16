package net.openhft.chronicle.wire;

import net.openhft.lang.io.Bytes;
import net.openhft.lang.io.DirectStore;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TextWireTest {

    Bytes bytes = new DirectStore(256).bytes();

    private TextWire createBytes() {
        bytes.clear();
        return new TextWire(bytes);
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
        assertEquals("\"\": \"\": \"\": ", wire.toString());
    }

    @Test
    public void testWrite1() throws Exception {
        Wire wire = createBytes();
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);
        wire.flip();
        assertEquals("field1: field2: field3: ", wire.toString());
    }

    @Test
    public void testWrite2() throws Exception {
        Wire wire = createBytes();
        wire.write("Hello", BWKey.field1);
        wire.write("World", BWKey.field2);
        wire.write("Long field name which is more than 32 characters, Bye", BWKey.field3);
        wire.flip();
        assertEquals("Hello: World: Long field name which is more than 32 characters, Bye: ", wire.toString());
    }

    @Test
    public void testRead() throws Exception {
        Wire wire = createBytes();
        wire.write();
        wire.write(BWKey.field1);
        wire.write("Test", BWKey.field2);
        wire.flip();
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

        // ok as blank matches anything
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        // not a match
        try {
            wire.read(BWKey.field1);
            fail();
        } catch (UnsupportedOperationException expected) {
            wire.read(StringBuilder::new, BWKey.field1);
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
        wire.read(() -> name, BWKey.field1);
        assertEquals(0, name.length());

        wire.read(() -> name, BWKey.field1);
        assertEquals(BWKey.field1.name(), name.toString());

        wire.read(() -> name, BWKey.field1);
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
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

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
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

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
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

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
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

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
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

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
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

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
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicLong i = new AtomicLong();
        IntConsumer ic = i::set;
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
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

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
        String name1 = "Long field name which is more than 32 characters, \\ \nBye";

        wire.write("Test", BWKey.field2)
                .text(name1);
        wire.flip();
        assertEquals("\"\": Hello\n" +
                "field1: world\n" +
                "Test: \"Long field name which is more than 32 characters, \\\\ \\nBye\"\n", wire.toString());

        // ok as blank matches anything
        StringBuilder sb = new StringBuilder();
        Stream.of("Hello", "world", name1).forEach(e -> {
            wire.read()
                    .text(() -> sb);
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
        wire.writeValue().comment("");
        wire.flip();
        assertEquals("\"\": !MyType " +
                "field1: !AlsoMyType " +
                "Test: !" + name1 + " # \n", wire.toString());

        // ok as blank matches anything
        StringBuilder sb = new StringBuilder();
        Stream.of("MyType", "AlsoMyType", name1).forEach(e -> {
            wire.read().type(() -> sb);
            assertEquals(e, sb.toString());
        });

        assertEquals(3, bytes.remaining());
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