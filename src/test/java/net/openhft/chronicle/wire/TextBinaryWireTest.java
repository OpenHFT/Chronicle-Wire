package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

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
}
