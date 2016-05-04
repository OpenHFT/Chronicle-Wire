package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.NativeBytesStore;
import org.junit.Test;

import java.io.EOFException;
import java.io.StreamCorruptedException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Peter on 04/05/2016.
 */
public class BinaryWireHeadersTest {
    @Test
    public void testHeaderNumbers() throws TimeoutException, EOFException, StreamCorruptedException {
        BytesStore store = NativeBytesStore.elasticByteBuffer();
        Wire wire = new BinaryWire(store.bytesForWrite());
        Wire wire2 = new BinaryWire(store.bytesForWrite());

        assertEquals(0, wire.headerNumber());
        assertTrue(wire.writeFirstHeader());
        wire.getValueOut().text("my header");

        wire.updateFirstHeader();
        assertEquals(0, wire.headerNumber()); // meta data doesn't count.

        for (int i = 0; i <= 3; i++) {
            long position = wire2.writeHeader(1, TimeUnit.MILLISECONDS);
            assertEquals(i, wire2.headerNumber());
            wire2.getValueOut().text("hello world");
            wire2.updateHeader(position, false);
        }
        assertEquals(4, wire2.headerNumber());
        {
            long position = wire2.writeHeader(1, TimeUnit.MILLISECONDS);
            wire2.getValueOut().text("hello world");
            wire2.updateHeader(position, true); // meta data doesn't count.
            assertEquals(4, wire2.headerNumber());
        }

        for (int i = 4; i <= 8; i += 2) {
            long position = wire.writeHeader(1, TimeUnit.MILLISECONDS);
            assertEquals(i, wire.headerNumber());
            wire.getValueOut().text("hello world");
            wire.updateHeader(position, false);

            long position2 = wire2.writeHeader(1, TimeUnit.MILLISECONDS);
            assertEquals(i + 1, wire2.headerNumber());
            wire2.getValueOut().text("hello world");
            wire2.updateHeader(position2, false);
        }
        assertEquals(10, wire2.headerNumber());

    }
}
