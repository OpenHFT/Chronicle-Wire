package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("deprecation")
final class WireSmallIntTestSupport {
    private WireSmallIntTestSupport() {
    }

    static void writeInt8Triplet(Wire wire) {
        wire.write().int8(1);
        wire.write(BWKey.field1).int8(2);
        wire.write(() -> "Test").int8(3);
    }

    static void readInt8Triplet(Wire wire) {
        @NotNull AtomicInteger i = new AtomicInteger();
        readThree(wire, () -> wire.read().int8(i, AtomicInteger::set), i);
    }

    static void writeInt16Triplet(Wire wire) {
        wire.write().int16(1);
        wire.write(BWKey.field1).int16(2);
        wire.write(() -> "Test").int16(3);
    }

    static void readInt16Triplet(Wire wire) {
        @NotNull AtomicInteger i = new AtomicInteger();
        readThree(wire, () -> wire.read().int16(i, AtomicInteger::set), i);
    }

    static void writeUint8Triplet(Wire wire) {
        wire.write().uint8(1);
        wire.write(BWKey.field1).uint8(2);
        wire.write(() -> "Test").uint8(3);
    }

    static void readUint8Triplet(Wire wire) {
        @NotNull AtomicInteger i = new AtomicInteger();
        readThree(wire, () -> wire.read().uint8(i, AtomicInteger::set), i);
    }

    static void writeUint16Triplet(Wire wire) {
        wire.write().uint16(1);
        wire.write(BWKey.field1).uint16(2);
        wire.write(() -> "Test").uint16(3);
    }

    static void readUint16Triplet(Wire wire) {
        @NotNull AtomicInteger i = new AtomicInteger();
        readThree(wire, () -> wire.read().uint16(i, AtomicInteger::set), i);
    }

    static void writeUint32Triplet(Wire wire) {
        wire.write().uint32(1);
        wire.write(BWKey.field1).uint32(2);
        wire.write(() -> "Test").uint32(3);
    }

    static void readUint32Triplet(Wire wire) {
        @NotNull AtomicLong i = new AtomicLong();
        readThree(wire, () -> wire.read().uint32(i, AtomicLong::set), i);
    }

    static void writeInt32Triplet(Wire wire) {
        wire.write().int32(1);
        wire.write(BWKey.field1).int32(2);
        wire.write(() -> "Test").int32(3);
    }

    static void readInt32Triplet(Wire wire) {
        @NotNull AtomicInteger i = new AtomicInteger();
        readThree(wire, () -> wire.read().int32(i, AtomicInteger::set), i);
    }

    static void expectTextLayout(Wire wire, String expectedSnake, String expectedText) {
        assertEquals(expectedText, wire.toString());
    }

    static void expectBinaryDebug(Bytes<?> bytes, String expectedDebug) {
        assertEquals(expectedDebug, bytes.toDebugString());
    }

    private static <T> void readThree(Wire wire, Runnable reader, T holder) {
        for (int expected = 1; expected <= 3; expected++) {
            reader.run();
            if (holder instanceof AtomicInteger) {
                assertEquals(expected, ((AtomicInteger) holder).get());
            } else if (holder instanceof AtomicLong) {
                assertEquals(expected, ((AtomicLong) holder).get());
            }
        }
    }
}
