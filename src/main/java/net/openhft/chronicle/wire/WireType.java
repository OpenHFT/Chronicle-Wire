/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.ref.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.LicenceCheck;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.openhft.chronicle.core.io.IOTools.*;

/**
 * A selection of prebuilt wire types.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public enum WireType implements Function<Bytes<?>, Wire>, LicenceCheck {

    TEXT {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return new TextWire(bytes).useBinaryDocuments();
        }

        @Override
        public Supplier<LongValue> newLongReference() {
            return TextLongReference::new;
        }

        @Override
        public Supplier<LongArrayValues> newLongArrayReference() {
            return TextLongArrayReference::new;
        }

        @Nullable
        @Override
        public <T> T fromString(Class<T> tClass, @NotNull CharSequence cs) {
            Bytes<?> bytes = Bytes.allocateElasticDirect(cs.length());
            try {
                bytes.appendUtf8(cs);
                @NotNull TextWire wire = (TextWire) apply(bytes);
                wire.consumePadding();
                wire.consumeDocumentStart();
                return wire.getValueIn().object(tClass);
            } finally {
                bytes.releaseLast();
            }
        }

        @Override
        public boolean isText() {
            return true;
        }
    },
    /**
     * Use this ONLY if intend to use Delta and Binary. Otherwise, use {@link #BINARY_LIGHT}
     */
    BINARY {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return new BinaryWire(bytes);
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) {
            return fromHexString(cs);
        }
    },
    /**
     * Use this when only need to use Binary (does not support DeltaWire)
     */
    BINARY_LIGHT {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return BinaryWire.binaryOnly(bytes);
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) {
            return fromHexString(cs);
        }
    },
    DEFAULT_ZERO_BINARY {
        @NotNull
        @Override
        public Wire apply(Bytes<?> bytes) {

            try {
                return (Wire) Class.forName("software.chronicle.wire.DefaultZeroWire")
                        .getDeclaredConstructor(Bytes.class)
                        .newInstance(bytes);

            } catch (Exception e) {
                @NotNull IllegalStateException licence = new IllegalStateException(
                        "A Chronicle Wire Enterprise licence is required to run this code " +
                                "because you are using DefaultZeroWire which is a licence product. " +
                                "Please contact sales@chronicle.software");
                Jvm.warn().on(getClass(), licence);
                throw licence;
            }
        }

        @Override
        public void licenceCheck() {
            if (isAvailable())
                return;

            @NotNull final IllegalStateException licence = new IllegalStateException("A Chronicle Wire " +
                    "Enterprise licence is required to run this code because you are using " +
                    "DEFAULT_ZERO_BINARY which is a licence product. " +
                    "Please contact sales@chronicle.software");
            Jvm.warn().on(getClass(), licence);
            throw licence;
        }

        @Override
        public boolean isAvailable() {
            return IS_DEFAULT_ZERO_AVAILABLE;
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) {
            return fromHexString(cs);
        }
    },
    DELTA_BINARY {
        @NotNull
        @Override
        public Wire apply(Bytes<?> bytes) {

            try {
                @NotNull
                Class<Wire> aClass = (Class) Class.forName("software.chronicle.wire.DeltaWire");
                final Constructor<Wire> declaredConstructor = aClass.getDeclaredConstructor(Bytes.class);
                return declaredConstructor.newInstance(bytes);

            } catch (Exception e) {
                licenceCheck();

                // this should never happen
                throw new AssertionError(e);
            }
        }

        @Override
        public void licenceCheck() {
            if (isAvailable())
                return;

            @NotNull final IllegalStateException licence = new IllegalStateException("A Chronicle-Wire-" +
                    "Enterprise licence is required to run this code because you are using " +
                    "DELTA_BINARY which is a licence product. " +
                    "Please contact sales@chronicle.software");
            Jvm.error().on(WireType.class,  licence);
            throw licence;
        }

        @Override
        public boolean isAvailable() {
            return IS_DELTA_AVAILABLE;
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) {
            return fromHexString(cs);
        }
    },
    FIELDLESS_BINARY {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return new BinaryWire(bytes, false, false, true, Integer.MAX_VALUE, "binary", false);
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) {
            return fromHexString(cs);
        }
    },
    COMPRESSED_BINARY {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return new BinaryWire(bytes, false, false, false, COMPRESSED_SIZE, "lzw", true);
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) {
            return fromHexString(cs);
        }
    },
    JSON {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return new JSONWire(bytes).useBinaryDocuments();
        }

        @Override
        public boolean isText() {
            return true;
        }
    },
    JSON_ONLY {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return new JSONWire(bytes).useTypes(true).trimFirstCurly(false).useTextDocuments();
        }

        @Override
        public boolean isText() {
            return true;
        }
    },
    YAML {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return new YamlWire(bytes).useBinaryDocuments();
        }

        @Override
        public boolean isText() {
            return true;
        }
    },
    YAML_ONLY {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return new YamlWire(bytes).useTextDocuments();
        }

        @Override
        public boolean isText() {
            return true;
        }
    },
    RAW {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return new RawWire(bytes);
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) {
            return fromHexString(cs);
        }
    },
    CSV {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return new CSVWire(bytes);
        }

        @Override
        public boolean isText() {
            return true;
        }
    },
    READ_ANY {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return new ReadAnyWire(bytes);
        }
    };

    private static final int COMPRESSED_SIZE = Integer.getInteger("WireType.compressedSize", 128);
    private static final boolean IS_DELTA_AVAILABLE = isDeltaAvailable();
    private static final boolean IS_DEFAULT_ZERO_AVAILABLE = isDefaultZeroAvailable();

    private static boolean isDeltaAvailable() {
        try {
            Class.forName("software.chronicle.wire.DeltaWire").getDeclaredConstructor(Bytes.class);
            return true;
        } catch (Exception fallback) {
            return false;
        }
    }

    private static boolean isDefaultZeroAvailable() {
        try {
            Class.forName("software.chronicle.wire.DefaultZeroWire").getDeclaredConstructor(Bytes.class);
            return true;
        } catch (Exception var4) {
            return false;
        }
    }

    @NotNull
    static Bytes<?> getBytesForToString() {
        return Wires.acquireBytesForToString();
    }

    @NotNull
    static Bytes<?> getBytes2() {
        // when in debug, the output becomes confused if you reuse the buffer.
        if (Jvm.isDebug())
            return Bytes.allocateElasticOnHeap();
        return Wires.acquireAnotherBytes();
    }

    @Nullable
    public static WireType valueOf(@Nullable Wire wire) {

        if (wire == null)
            return null;

        if (wire instanceof AbstractAnyWire)
            wire = ((AbstractAnyWire) wire).underlyingWire();

        if (wire instanceof YamlWire)
            return WireType.YAML;

        if (wire instanceof JSONWire)
            return WireType.JSON;

        if (wire instanceof TextWire)
            return WireType.TEXT;

        if ("DeltaWire".equals(wire.getClass().getSimpleName())) {
            return DELTA_BINARY;
        }

        // this must be above BinaryWire
        if ("DefaultZeroWire".equals(wire.getClass().getSimpleName())) {
            return DEFAULT_ZERO_BINARY;
        }

        if (wire instanceof BinaryWire) {
            @NotNull BinaryWire binaryWire = (BinaryWire) wire;
            return binaryWire.fieldLess() ? FIELDLESS_BINARY : WireType.BINARY;
        }

        if (wire instanceof RawWire) {
            return WireType.RAW;
        }

        throw new IllegalStateException("unknown type");
    }

    public Supplier<IntValue> newIntReference() {
        return BinaryIntReference::new;
    }

    public Supplier<BooleanValue> newBooleanReference() {
        return BinaryBooleanReference::new;
    }

    public Supplier<LongValue> newLongReference() {
        return BinaryLongReference::new;
    }

    public Supplier<TwoLongValue> newTwoLongReference() {
        return BinaryTwoLongReference::new;
    }

    public Supplier<LongArrayValues> newLongArrayReference() {
        return BinaryLongArrayReference::new;
    }

    public String asString(Object marshallable) {
        Bytes<?> bytes = asBytes(marshallable);
        return bytes.toString();
    }

    @NotNull
    private Bytes<?> asBytes(Object marshallable) {
        Bytes<?> bytes = getBytesForToString();
        Wire wire = apply(bytes);
        wire.usePadding(AbstractWire.DEFAULT_USE_PADDING);
        @NotNull final ValueOut valueOut = wire.getValueOut();

        if (marshallable instanceof WriteMarshallable)
            valueOut.typedMarshallable((WriteMarshallable) marshallable);
        else if (marshallable instanceof Map)
            wire.getValueOut().marshallable((Map) marshallable, Object.class, Object.class, false);
        else if (marshallable instanceof Iterable)
            wire.getValueOut().sequence((Iterable) marshallable);
        else if (marshallable instanceof Serializable)
            valueOut.typedMarshallable((Serializable) marshallable);
        else {
            valueOut.typedMarshallable(Wires.typeNameFor(marshallable),
                    w -> Wires.writeMarshallable(marshallable, w));
        }
        return bytes;
    }

    /**
     * deserializes with an optimistic cast
     *
     * @param cs  text to deserialize
     * @param <T> the type to expect
     * @return the object deserialized
     * @throws ClassCastException if the object is not a T
     */
    @Nullable
    public <T> T fromString(@NotNull CharSequence cs) {
        return (T) fromString(/* Allow Marshallable tuples by not requesting  Object */ null, cs);
    }

    /**
     * deserializes as a given class
     *
     * @param tClass to serialize as
     * @param cs     text to deserialize
     * @return the object deserialized
     */
    public <T> T fromString(Class<T> tClass, @NotNull CharSequence cs) {
        if (cs.length() == 0)
            throw new IllegalArgumentException("cannot deserialize an empty string");
        Bytes<?> bytes = getBytes2();
        bytes.appendUtf8(cs);
        Wire wire = apply(bytes);
        return wire.getValueIn().object(tClass);
    }

    @NotNull
    public <T> T fromFile(String filename) throws IOException {
        return (T) fromFile(Marshallable.class, filename);
    }

    @Nullable
    public <T> T fromFile(@NotNull Class<T> expectedType, String filename) throws IOException {
        File file = new File(filename);
        URL url = null;
        if (!file.exists()) {
            url = urlFor(expectedType, filename);
            file = new File(url.getFile());
        }
        //: MappedFile.readOnly(file).acquireBytesForRead(0);

        Bytes<?> bytes = Bytes.wrapForRead(readAsBytes(url == null ? new FileInputStream(file) : open(url)));
        if (bytes.readRemaining() == 0)
            throw new IOException("File " + file + " was empty");
        try {
            return apply(bytes).getValueIn().object(expectedType);
        } finally {
            bytes.releaseLast();
        }
    }

    @NotNull
    public <T> Stream<T> streamFromFile(String filename) throws IOException {
        return streamFromFile((Class) Marshallable.class, filename);
    }

    @NotNull
    public <T> Stream<T> streamFromFile(@NotNull Class<T> expectedType, String filename) throws IOException {
        Bytes<?> b = BytesUtil.readFile(filename);
        return streamFromBytes(expectedType, b);
    }

    @NotNull
    public <T> Stream<T> streamFromBytes(@NotNull Class<T> expectedType, Bytes<?> b) {
        Wire wire = apply(b);
        ValueIn valueIn = wire.getValueIn();
        return StreamSupport.stream(
                new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.IMMUTABLE) {
                    @Override
                    public boolean tryAdvance(@NotNull Consumer<? super T> action) {
                        Bytes<?> bytes = wire.bytes();
                        if (valueIn.hasNext()) {
                            action.accept(valueIn.object(expectedType));
                            if (wire instanceof TextWire) {
                                wire.consumePadding();
                                if (bytes.peekUnsignedByte() == '-' &&
                                        bytes.peekUnsignedByte(bytes.readPosition() + 1) == '-' &&
                                        bytes.peekUnsignedByte(bytes.readPosition() + 2) == '-') {
                                    bytes.readSkip(3);
                                    while (bytes.peekUnsignedByte() == '-')
                                        bytes.readSkip(1);
                                }
                            }
                            return true;
                        }
                        if (bytes.refCount() > 0)
                            bytes.releaseLast();
                        return false;
                    }
                }, false);
    }

    @NotNull
    public <T> Map<String, T> fromFileAsMap(String filename, @NotNull Class<T> tClass) throws IOException {
        @NotNull Map<String, T> map = new LinkedHashMap<>();
        Wire wire = apply(BytesUtil.readFile(filename));
        @NotNull StringBuilder sb = new StringBuilder();
        while (wire.hasMore()) {
            wire.readEventName(sb)
                    .object(tClass, map, (m, o) -> m.put(sb.toString(), o));
        }
        return map;
    }

    public <T extends Marshallable> void toFileAsMap(@NotNull String filename, @NotNull Map<String, T> map)
            throws IOException {
        toFileAsMap(filename, map, false);
    }

    public <T extends Marshallable> void toFileAsMap(@NotNull String filename, @NotNull Map<String, T> map, boolean compact)
            throws IOException {
        Bytes<?> bytes = WireInternal.acquireInternalBytes();
        Wire wire = apply(bytes);
        for (@NotNull Map.Entry<String, T> entry : map.entrySet()) {
            @NotNull ValueOut valueOut = wire.writeEventName(entry::getKey);
            boolean wasLeaf = valueOut.swapLeaf(compact);
            valueOut.marshallable(entry.getValue());
            valueOut.swapLeaf(wasLeaf);
        }
        String tempFilename = IOTools.tempName(filename);
        IOTools.writeFile(tempFilename, bytes.toByteArray());
        @NotNull File file2 = new File(tempFilename);
        @NotNull File dest = new File(filename);
        if (!file2.renameTo(dest)) {
            if (dest.delete() && file2.renameTo(dest))
                return;
            file2.delete();
            throw new IOException("Failed to rename " + tempFilename + " to " + filename);
        }
    }

    public void toFile(@NotNull String filename, WriteMarshallable marshallable) throws IOException {
        Bytes<?> bytes = WireInternal.acquireInternalBytes();
        Wire wire = apply(bytes);
        wire.getValueOut().typedMarshallable(marshallable);
        String tempFilename = IOTools.tempName(filename);
        IOTools.writeFile(tempFilename, bytes.toByteArray());
        @NotNull File file2 = new File(tempFilename);
        if (!file2.renameTo(new File(filename))) {
            file2.delete();
            throw new IOException("Failed to rename " + tempFilename + " to " + filename);
        }
    }

    @NotNull
    String asHexString(Object marshallable) {
        Bytes<?> bytes = asBytes(marshallable);
        return bytes.toHexString();
    }

    @Nullable <T> T fromHexString(@NotNull CharSequence s) {
        Bytes<?> bytes = Bytes.fromHexString(s.toString());
        try {
            Wire wire = apply(bytes);
            return wire.getValueIn().typedMarshallable();
        } finally {
            bytes.releaseLast();
        }
    }

    @Nullable
    public Map<String, Object> asMap(@NotNull CharSequence cs) {
        Bytes<?> bytes = getBytes2();
        bytes.appendUtf8(cs);
        Wire wire = apply(bytes);
        return wire.getValueIn().marshallableAsMap(String.class, Object.class);
    }

    @Override
    public void licenceCheck() {
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    public boolean isText() {
        return false;
    }
}
