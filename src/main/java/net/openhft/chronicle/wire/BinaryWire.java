package net.openhft.chronicle.wire;

import net.openhft.lang.io.Bytes;
import net.openhft.lang.pool.StringInterner;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.function.*;

/**
 * Created by peter on 15/01/15.
 */
public class BinaryWire implements Wire {
    final Bytes bytes;
    final WriteValue writeValue = new BinaryWriteValue();
    final ReadValue readValue = new BinaryReadValue();

    public BinaryWire(Bytes bytes) {
        this.bytes = bytes;
    }

    @Override
    public WriteValue write() {
        return writeValue;
    }

    @Override
    public WriteValue write(WireKey key) {
        writeField(key.name());
        return writeValue;
    }

    private void writeField(CharSequence name) {
        int len = name.length();
        if (len <= 30) {
            long pos = bytes.position();
            bytes.writeUTFΔ(name);
            bytes.writeUnsignedByte(pos, WireTypes.FIELD_NAME1.code + len - 1);
        } else {
            bytes.writeUnsignedByte(WireTypes.FIELD_NAME_ANY.code);
            bytes.writeUTFΔ(name);
        }
    }

    @Override
    public WriteValue write(CharSequence name, WireKey template) {
        writeField(name);
        return writeValue;
    }

    @Override
    public ReadValue read() {
        return readValue;
    }

    @Override
    public ReadValue read(WireKey key) {
        validateField(key.name());
        return readValue;
    }

    @Override
    public ReadValue read(Supplier<StringBuilder> name, WireKey template) {
        readField(name.get());
        return readValue;
    }

    static final ThreadLocal<StringBuilder> MyStringBuilder = new ThreadLocal<>();

    private static StringBuilder acquireStringBuilder() {
        StringBuilder sb = MyStringBuilder.get();
        if (sb == null)
            MyStringBuilder.set(sb = new StringBuilder());
        return sb;
    }

    private void validateField(String name) {
        StringBuilder sb = new StringBuilder();
        readField(sb);
        if (!StringInterner.isEqual(sb, name))
            throw new IllegalStateException("Expected " + name + " but got " + sb);
    }

    private void readField(StringBuilder builder) {
        long pos = bytes.position();
        int code = bytes.readUnsignedByte(pos);
        if (code == WireTypes.FIELD_NAME_ANY.code) {
            bytes.readUTFΔ(builder);
        } else {
            bytes.writeUnsignedByte(pos, code & 0x1f);
            try {
                bytes.readUTFΔ(builder);
            } finally {
                bytes.writeUnsignedByte(pos, code);
            }
        }
    }

    @Override
    public boolean hasNextSequenceItem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readSequenceEnd() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Wire writeComment(CharSequence s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Wire readComment(StringBuilder sb) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasMapping() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Wire writeDocumentStart() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeDocumentEnd() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasDocument() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Wire readDocumentStart() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readDocumentEnd() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flip() {
        bytes.flip();
    }

    @Override
    public void clear() {
        bytes.clear();
    }

    class BinaryWriteValue implements WriteValue {
        @Override
        public Wire sequence(Object... array) {
            return null;
        }

        @Override
        public Wire sequence(Iterable array) {
            return null;
        }

        @Override
        public Wire sequenceStart() {
            return null;
        }

        @Override
        public Wire sequenceEnd() {
            return null;
        }

        @Override
        public long startLength(int bytes) {
            return 0;
        }

        @Override
        public boolean endLength(long startPosition) {
            return false;
        }

        @Override
        public Wire text(CharSequence s) {
            return null;
        }

        @Override
        public Wire int8(int i8) {
            return null;
        }

        @Override
        public Wire uint8(int u8) {
            return null;
        }

        @Override
        public Wire int16(int i16) {
            return null;
        }

        @Override
        public Wire uint16(int u16) {
            return null;
        }

        @Override
        public Wire utf8(int codepoint) {
            return null;
        }

        @Override
        public Wire int32(int i32) {
            return null;
        }

        @Override
        public Wire uint32(long u32) {
            return null;
        }

        @Override
        public Wire float32(float f) {
            return null;
        }

        @Override
        public Wire float64(double d) {
            return null;
        }

        @Override
        public Wire int64(long i64) {
            return null;
        }

        @Override
        public Wire comment(CharSequence s) {
            return null;
        }

        @Override
        public Wire mapStart() {
            return null;
        }

        @Override
        public Wire mapEnd() {
            return null;
        }

        @Override
        public Wire time(LocalTime localTime) {
            return null;
        }

        @Override
        public Wire zonedDateTime(ZonedDateTime zonedDateTime) {
            return null;
        }

        @Override
        public Wire date(LocalDate zonedDateTime) {
            return null;
        }

        @Override
        public Wire object(Marshallable type) {
            return null;
        }
    }

    class BinaryReadValue implements ReadValue {
        @Override
        public Wire sequenceLength(IntConsumer length) {
            return null;
        }

        @Override
        public Wire sequence(Supplier<Collection> collection) {
            return null;
        }

        @Override
        public Wire text(Supplier<StringBuilder> s) {
            return null;
        }

        @Override
        public Wire int32(IntConsumer i) {
            return null;
        }

        @Override
        public Wire float64(DoubleConsumer v) {
            return null;
        }

        @Override
        public Wire int64(LongConsumer i) {
            return null;
        }

        @Override
        public Wire comment(Supplier<StringBuilder> s) {
            return null;
        }

        @Override
        public Wire mapStart() {
            return null;
        }

        @Override
        public Wire mapEnd() {
            return null;
        }

        @Override
        public Wire time(Consumer<LocalTime> localTime) {
            return null;
        }

        @Override
        public Wire zonedDateTime(Consumer<ZonedDateTime> zonedDateTime) {
            return null;
        }

        @Override
        public Wire date(Consumer<LocalDate> zonedDateTime) {
            return null;
        }

        @Override
        public Wire object(Supplier<Marshallable> type) {
            return null;
        }
    }
}
