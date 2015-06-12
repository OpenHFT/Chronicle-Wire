package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;

import java.util.function.Function;

public enum WireType implements Function<Bytes, Wire> {
    TEXT {
        @Override
        public Wire apply(Bytes bytes) {
            return new TextWire(bytes);
        }
    }, BINARY {
        @Override
        public Wire apply(Bytes bytes) {
            return new BinaryWire(bytes);
        }
    }, RAW {
        @Override
        public Wire apply(Bytes bytes) {
            return new RawWire(bytes);
        }
    };
}