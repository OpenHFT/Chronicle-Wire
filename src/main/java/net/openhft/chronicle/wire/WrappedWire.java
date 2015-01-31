package net.openhft.chronicle.wire;

import net.openhft.chronicle.util.BooleanConsumer;
import net.openhft.chronicle.util.ByteConsumer;
import net.openhft.chronicle.util.FloatConsumer;
import net.openhft.chronicle.util.ShortConsumer;
import net.openhft.lang.io.Bytes;
import net.openhft.lang.values.IntValue;
import net.openhft.lang.values.LongValue;

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

    public void readSequenceEnd() {
        wire.readSequenceEnd();
    }

    public boolean hasMapping() {
        return wire.hasMapping();
    }

    public Wire writeDocumentStart() {
        wire.writeDocumentStart();
        return _this();
    }

    public void writeDocumentEnd() {
        wire.writeDocumentEnd();
    }

    public boolean hasDocument() {
        return wire.hasDocument();
    }

    public void consumeDocumentEnd() {
        wire.consumeDocumentEnd();
    }

    public void flip() {
        wire.flip();
    }

    public void clear() {
        wire.clear();
    }

    public Wire writeComment(CharSequence s) {
        wire.writeComment(s);
        return _this();
    }

    public Wire readComment(StringBuilder s) {
        wire.readComment(s);
        return _this();
    }

    protected abstract Wire _this();

    public void addPadding(int paddingToAdd) {
        wire.addPadding(paddingToAdd);
    }

    class WrappedValueOut implements ValueOut {
        private final ValueOut valueOut;

        WrappedValueOut(ValueOut valueOut) {
            this.valueOut = valueOut;
        }

        public ValueOut sequenceStart() {
            valueOut.sequenceStart();
            return this;
        }

        public Wire sequenceEnd() {
            valueOut.sequenceEnd();
            return _this();
        }

        public Wire text(CharSequence s) {
            valueOut.text(s);
            return _this();
        }

        public Wire type(CharSequence typeName) {
            valueOut.type(typeName);
            return _this();
        }

        @Override
        public WireOut uuid(UUID uuid) {
            valueOut.uuid(uuid);
            return _this();
        }

        @Override
        public ValueOut cacheAlign() {
            valueOut.cacheAlign();
            return this;
        }

        @Override
        public WireOut int64(LongValue readReady) {
            valueOut.int64(readReady);
            return _this();
        }

        @Override
        public WireOut int32(IntValue value) {
            throw new UnsupportedOperationException();
        }

        public Wire bool(Boolean flag) {
            valueOut.bool(flag);
            return _this();
        }

        public Wire utf8(int codepoint) {
            valueOut.utf8(codepoint);
            return _this();
        }

        public Wire hint(CharSequence s) {
            valueOut.hint(s);
            return _this();
        }

        public Wire mapStart() {
            valueOut.mapStart();
            return _this();
        }

        public Wire mapEnd() {
            valueOut.mapEnd();
            return _this();
        }

        public Wire time(LocalTime localTime) {
            valueOut.time(localTime);
            return _this();
        }

        public Wire zonedDateTime(ZonedDateTime zonedDateTime) {
            valueOut.zonedDateTime(zonedDateTime);
            return _this();
        }

        public Wire date(LocalDate zonedDateTime) {
            valueOut.date(zonedDateTime);
            return _this();
        }

        public Wire object(Marshallable type) {
            valueOut.object(type);
            return _this();
        }

        public Wire int8(int i8) {
            valueOut.int8(i8);
            return _this();
        }

        public Wire uint8(int u8) {
            valueOut.uint8(u8);
            return _this();
        }

        public Wire int16(int i16) {
            valueOut.int16(i16);
            return _this();
        }

        public Wire uint16(int u16) {
            valueOut.uint16(u16);
            return _this();
        }

        public Wire int32(int i32) {
            valueOut.int32(i32);
            return _this();
        }

        public Wire uint32(long u32) {
            valueOut.uint32(u32);
            return _this();
        }

        public Wire float32(float f) {
            valueOut.float32(f);
            return _this();
        }

        public Wire float64(double d) {
            valueOut.float64(d);
            return _this();
        }

        public Wire int64(long i64) {
            valueOut.int64(i64);
            return _this();
        }
    }

    class WrappedValueIn implements ValueIn {


        private final ValueIn valueIn;

        WrappedValueIn(ValueIn valueIn) {
            this.valueIn = valueIn;
        }

        public ValueIn sequenceStart() {
            valueIn.sequenceStart();
            return this;
        }

        public Wire sequenceEnd() {
            valueIn.sequenceEnd();
            return _this();
        }

        public boolean hasNext() {
            return valueIn.hasNext();
        }

        public Wire type(StringBuilder s) {
            valueIn.type(s);
            return _this();
        }

        public Wire text(StringBuilder s) {
            valueIn.text(s);
            return _this();
        }

        public Wire bool(BooleanConsumer flag) {
            valueIn.bool(flag);
            return _this();
        }

        public Wire int8(ByteConsumer i) {
            valueIn.int8(i);
            return _this();
        }

        public Wire uint8(ShortConsumer i) {
            valueIn.uint8(i);
            return _this();
        }

        public Wire int16(ShortConsumer i) {
            valueIn.int16(i);
            return _this();
        }

        public Wire uint16(IntConsumer i) {
            valueIn.uint16(i);
            return _this();
        }

        public Wire uint32(LongConsumer i) {
            valueIn.uint32(i);
            return _this();
        }

        public Wire int32(IntConsumer i) {
            valueIn.int32(i);
            return _this();
        }

        public Wire float32(FloatConsumer v) {
            valueIn.float32(v);
            return _this();
        }

        public Wire float64(DoubleConsumer v) {
            valueIn.float64(v);
            return _this();
        }

        public Wire int64(LongConsumer i) {
            valueIn.int64(i);
            return _this();
        }

        public Wire mapStart() {
            valueIn.mapStart();
            return _this();
        }

        public Wire mapEnd() {
            valueIn.mapEnd();
            return _this();
        }

        public Wire time(Consumer<LocalTime> localTime) {
            valueIn.time(localTime);
            return _this();
        }

        public Wire zonedDateTime(Consumer<ZonedDateTime> zonedDateTime) {
            valueIn.zonedDateTime(zonedDateTime);
            return _this();
        }

        public Wire date(Consumer<LocalDate> zonedDateTime) {
            valueIn.date(zonedDateTime);
            return _this();
        }

        public Wire object(Supplier<Marshallable> type) {
            valueIn.object(type);
            return _this();
        }

        public WireIn text(Consumer<String> s) {
            valueIn.text(s);
            return _this();
        }

        public WireIn expectText(CharSequence s) {
            valueIn.expectText(s);
            return _this();
        }

        public WireIn uuid(Consumer<UUID> uuid) {
            valueIn.uuid(uuid);
            return _this();
        }

        public WireIn int64(LongValue value) {
            valueIn.int64(value);
            return _this();
        }

        @Override
        public WireIn int32(IntValue value) {
            throw new UnsupportedOperationException();
        }
    }
}
