package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.time.*;
import java.util.UUID;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.assertEquals;

/**
 * Created by peter.lawrey on 06/02/15.
 */
public class BinaryWire2Test {
    Bytes bytes = nativeBytes();

    private BinaryWire createWire() {
        bytes.clear();
        return new BinaryWire(bytes);
    }

    @Test
    public void testWriteMarshallable() {

    }

    @Test
    public void testBool() {
        Wire wire = createWire();
        wire.write().bool(false)
                .write().bool(true)
                .write().bool(null);
        wire.flip();
        wire.read().bool(Assert::assertFalse)
                .read().bool(Assert::assertTrue)
                .read().bool(Assert::assertNull);
    }

    @Test
    public void testFloat32() {
        Wire wire = createWire();
        wire.write().float32(0.0F)
                .write().float32(Float.NaN)
                .write().float32(Float.POSITIVE_INFINITY);
        wire.flip();
        wire.read().float32(t -> assertEquals(0.0F, t, 0.0F))
                .read().float32(Float::isNaN)
                .read().float32(t -> assertEquals(Float.POSITIVE_INFINITY, t, 0.0F));
    }


    @Test
    public void testTime() {
        Wire wire = createWire();
        LocalTime now = LocalTime.now();
        wire.write().time(now)
                .write().time(LocalTime.MAX)
                .write().time(LocalTime.MIN);
        wire.flip();
        wire.read().time(t -> assertEquals(now, t))
                .read().time(t -> assertEquals(LocalTime.MAX, t))
                .read().time(t -> assertEquals(LocalTime.MIN, t));

    }

    @Test
    public void testZonedDateTime() {
        Wire wire = createWire();
        ZonedDateTime now = ZonedDateTime.now();
        wire.write().zonedDateTime(now)
                .write().zonedDateTime(ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault()))
                .write().zonedDateTime(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault()));
        wire.flip();
        wire.read().zonedDateTime(t -> assertEquals(now, t))
                .read().zonedDateTime(t -> assertEquals(ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault()), t))
                .read().zonedDateTime(t -> assertEquals(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault()), t));
    }

    @Test
    public void testDate() {
        Wire wire = createWire();
        LocalDate now = LocalDate.now();
        wire.write().date(now)
                .write().date(LocalDate.MAX)
                .write().date(LocalDate.MIN);
        wire.flip();
        wire.read().date(t -> assertEquals(now, t))
                .read().date(t -> assertEquals(LocalDate.MAX, t))
                .read().date(t -> assertEquals(LocalDate.MIN, t));
    }

    @Test
    public void testUuid() {
        Wire wire = createWire();
        UUID uuid = UUID.randomUUID();
        wire.write().uuid(uuid)
                .write().uuid(new UUID(0, 0))
                .write().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE));
        wire.flip();
        wire.read().uuid(t -> assertEquals(uuid, t))
                .read().uuid(t -> assertEquals(new UUID(0, 0), t))
                .read().uuid(t -> assertEquals(new UUID(Long.MAX_VALUE, Long.MAX_VALUE), t));
    }

    @Test
    public void testSequence() {

    }

}
