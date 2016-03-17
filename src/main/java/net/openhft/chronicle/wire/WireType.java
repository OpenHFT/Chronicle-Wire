/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.bytes.ref.BinaryLongReference;
import net.openhft.chronicle.bytes.ref.TextLongArrayReference;
import net.openhft.chronicle.bytes.ref.TextLongReference;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A selection of prebuilt wire types.
 */
public enum WireType implements Function<Bytes, Wire> {
    TEXT {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new TextWire(bytes);
        }

        @Override
        public Supplier<LongValue> newLongReference() {
            return TextLongReference::new;
        }

        @Override
        public Supplier<LongArrayValues> newLongArrayReference() {
            return TextLongArrayReference::new;
        }

    }, BINARY {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new BinaryWire(bytes);
        }

        @Override
        public String asString(WriteMarshallable marshallable) {
            return asHexString(marshallable);
        }

        @Override
        public <T> T fromString(CharSequence cs) {
            return fromHexString(cs);
        }
    }, FIELDLESS_BINARY {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new BinaryWire(bytes, false, false, true, Integer.MAX_VALUE, "binary");
        }

        @Override
        public String asString(WriteMarshallable marshallable) {
            return asHexString(marshallable);
        }

        @Override
        public <T> T fromString(CharSequence cs) {
            return fromHexString(cs);
        }
    }, COMPRESSED_BINARY {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new BinaryWire(bytes, false, false, false, COMPRESSED_SIZE, "lzw");
        }

        @Override
        public String asString(WriteMarshallable marshallable) {
            return asHexString(marshallable);
        }

        @Override
        public <T> T fromString(CharSequence cs) {
            return fromHexString(cs);
        }
    }, JSON {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new JSONWire(bytes);
        }
    }, RAW {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new RawWire(bytes);
        }

        @Override
        public String asString(WriteMarshallable marshallable) {
            return asHexString(marshallable);
        }

        @Override
        public <T> T fromString(CharSequence cs) {
            return fromHexString(cs);
        }
    }, CSV {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new CSVWire(bytes);
        }
    },
    READ_ANY {
        @Override
        public Wire apply(@NotNull Bytes bytes) {
            return new ReadAnyWire(bytes);
        }
    };

    static final ThreadLocal<Bytes> bytesTL = ThreadLocal.withInitial(Bytes::allocateElasticDirect);
    private static final int COMPRESSED_SIZE = Integer.getInteger("WireType.compressedSize", 128);

    static Bytes getBytes() {
        // when in debug, the output becomes confused if you reuse the buffer.
        if (Jvm.isDebug())
            return Bytes.allocateElasticDirect();
        Bytes bytes = bytesTL.get();
        bytes.clear();
        return bytes;
    }

    public static WireType valueOf(Wire wire) {

        if (wire instanceof AbstractAnyWire)
            wire = ((AbstractAnyWire) wire).underlyingWire();

        if (wire instanceof TextWire)
            return WireType.TEXT;

        if (wire instanceof BinaryWire) {
            BinaryWire binaryWire = (BinaryWire) wire;
            return binaryWire.fieldLess() ? FIELDLESS_BINARY : WireType.BINARY;
        }

        throw new IllegalStateException("unknown type");
    }

    public Supplier<LongValue> newLongReference() {
        return BinaryLongReference::new;
    }

    public Supplier<LongArrayValues> newLongArrayReference() {
        return BinaryLongArrayReference::new;
    }

    public String asString(WriteMarshallable marshallable) {
        Bytes bytes = getBytes();
        Wire wire = apply(bytes);
        wire.getValueOut().typedMarshallable(marshallable);
        return bytes.toString();
    }

    public <T> T fromString(CharSequence cs) {
        Bytes bytes = getBytes();
        bytes.appendUtf8(cs);
        Wire wire = apply(bytes);
        return wire.getValueIn().typedMarshallable();
    }

    public <T> T fromFile(String filename) throws IOException {
        return (T) (apply(Bytes.wrapForRead(IOTools.readFile(filename))).getValueIn().typedMarshallable());
    }

    public <T> Map<String, T> fromFileAsMap(String filename, Class<T> tClass) throws IOException {
        Map<String, T> map = new LinkedHashMap<>();
        Wire wire = apply(Bytes.wrapForRead(IOTools.readFile(filename)));
        StringBuilder sb = new StringBuilder();
        while (wire.hasMore()) {
            wire.readEventName(sb)
                    .object(tClass, map, (m, o) -> m.put(sb.toString(), o));
        }
        return map;
    }

    public <T extends Marshallable> void toFileAsMap(String filename, Map<String, T> map) throws IOException {
        toFileAsMap(filename, map, false);
    }

    public <T extends Marshallable> void toFileAsMap(String filename, Map<String, T> map, boolean compact) throws IOException {
        Bytes bytes = getBytes();
        Wire wire = apply(bytes);
        for (Map.Entry<String, T> entry : map.entrySet()) {
            ValueOut valueOut = wire.writeEventName(entry::getKey);
            valueOut.leaf(compact).marshallable(entry.getValue());
        }
        String tempFilename = IOTools.tempName(filename);
        IOTools.writeFile(tempFilename, bytes.toByteArray());
        File file2 = new File(tempFilename);
        File dest = new File(filename);
        if (!file2.renameTo(dest)) {
            if (dest.delete() && file2.renameTo(dest))
                return;
            file2.delete();
            throw new IOException("Failed to rename " + tempFilename + " to " + filename);
        }
    }

    public <T> void toFile(String filename, WriteMarshallable marshallable) throws IOException {
        Bytes bytes = getBytes();
        Wire wire = apply(bytes);
        wire.getValueOut().typedMarshallable(marshallable);
        String tempFilename = IOTools.tempName(filename);
        IOTools.writeFile(tempFilename, bytes.toByteArray());
        File file2 = new File(tempFilename);
        if (!file2.renameTo(new File(filename))) {
            file2.delete();
            throw new IOException("Failed to rename " + tempFilename + " to " + filename);
        }
    }

    String asHexString(WriteMarshallable marshallable) {
        Bytes bytes = getBytes();
        Wire wire = apply(bytes);
        wire.getValueOut().typedMarshallable(marshallable);
        return bytes.toHexString();
    }

    <T> T fromHexString(CharSequence s) {
        Wire wire = apply(Bytes.fromHexString(s.toString()));
        return wire.getValueIn().typedMarshallable();
    }
}