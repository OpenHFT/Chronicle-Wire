package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.ObjIntConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by peter on 14/05/16.
 */
@RunWith(value = Parameterized.class)
public class TextBinaryWireTest {

    private final WireType wireType;

    public TextBinaryWireTest(WireType wireType) {

        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        return Arrays.asList(
                new Object[]{WireType.TEXT},
                new Object[]{WireType.JSON},
                new Object[]{WireType.BINARY},
                new Object[]{WireType.FIELDLESS_BINARY},
                new Object[]{WireType.RAW}
        );
    }

    @Test
    public void testValueOf() {
        WireType wt = WireType.valueOf(createWire());
        assertEquals(wireType, wt);
    }

    public Wire createWire() {
        return wireType.apply(Bytes.elasticByteBuffer());
    }

    @Test
    public void readingDocumentLocation() {
        Wire wire = createWire();
        wire.writeDocument(true, w -> w.write("header").text("data"));
        long position = wire.bytes().writePosition();
        wire.writeDocument(false, w -> w.write("message").text("text"));

        try (DocumentContext dc = wire.readingDocument(position)) {
            assertEquals("text", dc.wire().read(() -> "message").text());
        }
    }

    @Test
    public void testReadComment() {
        if (wireType == WireType.TEXT || wireType == WireType.BINARY) {
            Wire wire = createWire();
            wire.writeComment("This is a comment");
            StringBuilder sb = new StringBuilder();
            wire.readComment(sb);
            assertEquals("This is a comment", sb.toString());
        }
    }

    @Test
    public void readFieldAsObject() {
        if (wireType == WireType.RAW || wireType == WireType.FIELDLESS_BINARY)
            return;
        Wire wire = createWire();
        wire.write("CLASS").text("class")
                .write("RUNTIME").text("runtime");
        assertEquals(RetentionPolicy.CLASS, wire.readEvent(RetentionPolicy.class));
        assertEquals("class", wire.getValueIn().text());
        assertEquals(RetentionPolicy.RUNTIME, wire.readEvent(RetentionPolicy.class));
        assertEquals("runtime", wire.getValueIn().text());

        assertNull(wire.readEvent(RetentionPolicy.class));
    }

    @Test
    public void readFieldAsLong() {
        if (wireType == WireType.RAW || wireType == WireType.FIELDLESS_BINARY)
            return;
        Wire wire = createWire();
        // todo fix to ensure a field number is used.
        wire.writeEvent(Long.class, 1L).text("class")
                .writeEvent(Long.class, 2L).text("runtime");

        assertEquals((Long) 1L, wire.readEvent(Long.class));
        assertEquals("class", wire.getValueIn().text());
        StringBuilder sb = new StringBuilder();
        wire.readEventName(sb);
        assertEquals("2", sb.toString());
        assertEquals("runtime", wire.getValueIn().text());

        assertNull(wire.readEvent(RetentionPolicy.class));
    }

    @Test
    public void testConvertToNum() {
        if (wireType == WireType.RAW)
            return;

        Wire wire = createWire();
        wire.write("a").bool(false)
                .write("b").bool(true)
                .write("c").float32(2.0f)
                .write("d").float64(3.0);

        final ObjIntConsumer<Integer> assertEquals = (expected, actual) -> Assert.assertEquals((long) expected, actual);
        wire.read(() -> "a").int32(0, assertEquals);
        wire.read(() -> "b").int32(1, assertEquals);
        wire.read(() -> "c").int32(2, assertEquals);
        wire.read(() -> "d").int32(3, assertEquals);
    }
}

