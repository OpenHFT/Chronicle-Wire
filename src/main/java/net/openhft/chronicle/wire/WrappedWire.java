package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.util.BooleanConsumer;
import net.openhft.chronicle.util.ByteConsumer;
import net.openhft.chronicle.util.FloatConsumer;
import net.openhft.chronicle.util.ShortConsumer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.*;

/**
 * Created by peter.lawrey on 30/01/15.
 */
public abstract class WrappedWire {
    private Wire wire;

    public WrappedWire(Wire wire) {
        this.wire = wire;
    }

    protected void setWire(Wire wire) {
        this.wire = wire;
    }

    public Bytes bytes() {
        return wire.bytes();
    }

    public void copyTo(WireOut wire) {
        this.wire.copyTo(wire);
    }

    public ValueOut write() {
        return wire.write();
    }

    public ValueOut writeValue() {
        return wire.writeValue();
    }

    public ValueOut write(WireKey key) {
        return wire.write(key);
    }

    public ValueOut write(CharSequence name, WireKey template) {
        return wire.write(name, template);
    }

    public ValueIn read() {
        return wire.read();
    }

    public ValueIn read(WireKey key) {
        return wire.read(key);
    }

    public ValueIn read(StringBuilder name, WireKey template) {
        return wire.read(name, template);
    }

    public boolean hasNextSequenceItem() {
        return wire.hasNextSequenceItem();
    }

    public boolean hasMapping() {
        return wire.hasMapping();
    }

    public void writeDocument(Runnable writer) {
        wire.writeDocument(writer);
    }

    public <T> T readDocument(Function<WireIn, T> reader, Consumer<WireIn> metaDataReader) {
        return wire.readDocument(reader, metaDataReader);
    }

    public void writeMetaData(Runnable writer) {
        wire.writeMetaData(writer);
    }

    public boolean hasDocument() {
        return wire.hasDocument();
    }

    public void flip() {
        wire.flip();
    }

    public void clear() {
        wire.clear();
    }

    public WireOut writeComment(CharSequence s) {
        wire.writeComment(s);
        return thisWireOut();
    }

    public WireIn readComment(StringBuilder s) {
        wire.readComment(s);
        return thisWireIn();
    }

    protected abstract WireOut thisWireOut();

    protected abstract WireIn thisWireIn();

    public WireOut addPadding(int paddingToAdd) {
        wire.addPadding(paddingToAdd);
        return thisWireOut();
    }


    class WrappedValueOut implements ValueOut {
        private final ValueOut valueOut;

        WrappedValueOut(ValueOut valueOut) {
            this.valueOut = valueOut;
        }

        @Override
        public WireOut sequence(Runnable writer) {
            throw new UnsupportedOperationException();
        }

        public WireOut text(CharSequence s) {
            valueOut.text(s);
            return thisWireOut();
        }

        public WireOut type(CharSequence typeName) {
            valueOut.type(typeName);
            return thisWireOut();
        }

        @Override
        public WireOut uuid(UUID uuid) {
            valueOut.uuid(uuid);
            return thisWireOut();
        }

        @Override
        public WireOut int64(LongValue readReady) {
            valueOut.int64(readReady);
            return thisWireOut();
        }

        @Override
        public WireOut int32(IntValue value) {
            throw new UnsupportedOperationException();
        }

        public WireOut bool(Boolean flag) {
            valueOut.bool(flag);
            return thisWireOut();
        }

        public WireOut utf8(int codepoint) {
            valueOut.utf8(codepoint);
            return thisWireOut();
        }

        public WireOut time(LocalTime localTime) {
            valueOut.time(localTime);
            return thisWireOut();
        }

        public WireOut zonedDateTime(ZonedDateTime zonedDateTime) {
            valueOut.zonedDateTime(zonedDateTime);
            return thisWireOut();
        }

        public WireOut date(LocalDate localDate) {
            valueOut.date(localDate);
            return thisWireOut();
        }

        public WireOut int8(byte i8) {
            valueOut.int8(i8);
            return thisWireOut();
        }

        @Override
        public WireOut bytes(Bytes fromBytes) {
            valueOut.bytes(fromBytes);
            return thisWireOut();
        }

        @Override
        public ValueOut writeLength(long remaining) {
            valueOut.writeLength(remaining);
            return this;
        }

        @Override
        public WireOut bytes(byte[] fromBytes) {
            valueOut.bytes(fromBytes);
            return thisWireOut();
        }

        public WireOut uint8checked(int u8) {
            valueOut.uint8(u8);
            return thisWireOut();
        }

        public WireOut int16(short i16) {
            valueOut.int16(i16);
            return thisWireOut();
        }

        public WireOut uint16checked(int u16) {
            valueOut.uint16(u16);
            return thisWireOut();
        }

        public WireOut int32(int i32) {
            valueOut.int32(i32);
            return thisWireOut();
        }

        public WireOut uint32checked(long u32) {
            valueOut.uint32(u32);
            return thisWireOut();
        }

        public WireOut float32(float f) {
            valueOut.float32(f);
            return thisWireOut();
        }

        public WireOut float64(double d) {
            valueOut.float64(d);
            return thisWireOut();
        }

        public WireOut int64(long i64) {
            valueOut.int64(i64);
            return thisWireOut();
        }

        public WireOut writeMarshallable(Marshallable object) {
            valueOut.writeMarshallable(object);
            return thisWireOut();
        }
    }

    class WrappedValueIn implements ValueIn {
        private final ValueIn valueIn;

        WrappedValueIn(ValueIn valueIn) {
            this.valueIn = valueIn;
        }

        public boolean hasNext() {
            return valueIn.hasNext();
        }

        public WireIn type(StringBuilder s) {
            valueIn.type(s);
            return thisWireIn();
        }

        public WireIn text(StringBuilder s) {
            valueIn.text(s);
            return thisWireIn();
        }

        public WireIn bool(BooleanConsumer flag) {
            valueIn.bool(flag);
            return thisWireIn();
        }

        public WireIn int8(ByteConsumer i) {
            valueIn.int8(i);
            return thisWireIn();
        }

        @Override
        public WireIn wireIn() {
            return thisWireIn();
        }

        @Override
        public long readLength() {
            return valueIn.readLength();
        }

        public WireIn uint8(ShortConsumer i) {
            valueIn.uint8(i);
            return thisWireIn();
        }

        public WireIn int16(ShortConsumer i) {
            valueIn.int16(i);
            return thisWireIn();
        }

        public WireIn uint16(IntConsumer i) {
            valueIn.uint16(i);
            return thisWireIn();
        }

        public WireIn uint32(LongConsumer i) {
            valueIn.uint32(i);
            return thisWireIn();
        }

        public WireIn int32(IntConsumer i) {
            valueIn.int32(i);
            return thisWireIn();
        }

        public WireIn float32(FloatConsumer v) {
            valueIn.float32(v);
            return thisWireIn();
        }

        public WireIn float64(DoubleConsumer v) {
            valueIn.float64(v);
            return thisWireIn();
        }

        public WireIn int64(LongConsumer i) {
            valueIn.int64(i);
            return thisWireIn();
        }

        @Override
        public long int64() {
            return valueIn.int64();
        }

        public WireIn time(Consumer<LocalTime> localTime) {
            valueIn.time(localTime);
            return thisWireIn();
        }

        public WireIn zonedDateTime(Consumer<ZonedDateTime> zonedDateTime) {
            valueIn.zonedDateTime(zonedDateTime);
            return thisWireIn();
        }

        public WireIn date(Consumer<LocalDate> localDate) {
            valueIn.date(localDate);
            return thisWireIn();
        }

        public WireIn text(Consumer<String> s) {
            valueIn.text(s);
            return thisWireIn();
        }

        public WireIn expectText(CharSequence s) {
            valueIn.expectText(s);
            return thisWireIn();
        }

        public WireIn uuid(Consumer<UUID> uuid) {
            valueIn.uuid(uuid);
            return thisWireIn();
        }

        public WireIn int64(LongValue value) {
            valueIn.int64(value);
            return thisWireIn();
        }

        @Override
        public WireIn int32(IntValue value) {
            valueIn.int32(value);
            return thisWireIn();
        }

        @Override
        public WireIn sequence(Consumer<ValueIn> reader) {
            valueIn.sequence(reader);
            return thisWireIn();
        }

        @Override
        public WireIn readMarshallable(Marshallable object) {
            valueIn.readMarshallable(object);
            return thisWireIn();
        }


    }
}
