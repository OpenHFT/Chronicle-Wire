/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
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
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.ValidatableUtil;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URL;
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
 * Enumerates a selection of prebuilt wire types. These wire types define specific ways
 * data can be serialized and deserialized.
 * <p>
 * It also provides methods to acquire bytes,
 * useful in serialization operations.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public enum WireType implements Function<Bytes<?>, Wire>, LicenceCheck {

    TEXT {
        private final boolean TEXT_AS_YAML = Jvm.getBoolean("wire.testAsYaml");

        @SuppressWarnings("deprecation")
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            if (TEXT_AS_YAML)
                return YAML.apply(bytes);
            final TextWire wire = new TextWire(bytes).useBinaryDocuments();
            wire.usePadding(true);
            return wire;
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
        public <T> T fromString(Class<T> tClass, @NotNull CharSequence cs) throws InvalidMarshallableException {
            Bytes<?> bytes = Bytes.allocateElasticDirect(cs.length());
            try {
                bytes.appendUtf8(cs);
                @NotNull Wire wire = apply(bytes);
                wire.consumePadding();
                if (!TEXT_AS_YAML)
                    ((TextWire) wire).consumeDocumentStart();
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
     * With the removal of DeltaWire, this is the same as BINARY_LIGHT
     */
    BINARY {
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
        public <T> T fromString(@NotNull CharSequence cs) throws InvalidMarshallableException {
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
        public <T> T fromString(@NotNull CharSequence cs) throws InvalidMarshallableException {
            return fromHexString(cs);
        }
    },
    FIELDLESS_BINARY {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return new BinaryWire(bytes, false, false, true, Integer.MAX_VALUE, "binary");
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) throws InvalidMarshallableException {
            return fromHexString(cs);
        }
    },
    COMPRESSED_BINARY {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            return new BinaryWire(bytes, false, false, false, COMPRESSED_SIZE, "lzw");
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) throws InvalidMarshallableException {
            return fromHexString(cs);
        }
    },
    // for backward compatibility, this doesn't support types
    JSON {
        @SuppressWarnings("deprecation")
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            final JSONWire wire = new JSONWire(bytes);
            wire.useBinaryDocuments();
            wire.usePadding(true);
            return wire;
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
        @SuppressWarnings("deprecation")
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes<?> bytes) {
            final YamlWire wire = new YamlWire(bytes).useBinaryDocuments();
            wire.usePadding(true);
            return wire;
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
        public <T> T fromString(@NotNull CharSequence cs) throws InvalidMarshallableException {
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

    // Size after which data is compressed.
    private static final int COMPRESSED_SIZE = Integer.getInteger("WireType.compressedSize", 128);

    /**
     * Determines the  of a given {@link Wire} instance. This method inspects
     * the underlying type of the provided wire instance and maps it to its corresponding
     * WireType.
     *
     * @param wire The wire instance whose type needs to be determined.
     * @return The corresponding WireType of the given wire, or null if the input wire is null.
     * @throws IllegalStateException If the wire type is unrecognized.
     */
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

        if (wire instanceof BinaryWire) {
            @NotNull BinaryWire binaryWire = (BinaryWire) wire;
            return binaryWire.fieldLess() ? FIELDLESS_BINARY : WireType.BINARY;
        }

        if (wire instanceof RawWire) {
            return WireType.RAW;
        }

        throw new IllegalStateException("unknown type");
    }

    /**
     * Provides a supplier for a new {@link IntValue} reference using {@link BinaryIntReference}.
     *
     * @return A supplier that creates a new BinaryIntReference.
     */
    public Supplier<IntValue> newIntReference() {
        return BinaryIntReference::new;
    }

    /**
     * Provides a supplier for a new {@link BooleanValue} reference using {@link BinaryBooleanReference}.
     *
     * @return A supplier that creates a new BinaryBooleanReference.
     */
    public Supplier<BooleanValue> newBooleanReference() {
        return BinaryBooleanReference::new;
    }

    /**
     * Provides a supplier for a new {@link LongValue} reference using {@link BinaryLongReference}.
     *
     * @return A supplier that creates a new BinaryLongReference.
     */
    public Supplier<LongValue> newLongReference() {
        return BinaryLongReference::new;
    }

    /**
     * Provides a supplier for a new {@link TwoLongValue} reference using {@link BinaryTwoLongReference}.
     *
     * @return A supplier that creates a new BinaryTwoLongReference.
     */
    public Supplier<TwoLongValue> newTwoLongReference() {
        return BinaryTwoLongReference::new;
    }

    /**
     * Provides a supplier for a new {@link LongArrayValues} reference using {@link BinaryLongArrayReference}.
     *
     * @return A supplier that creates a new BinaryLongArrayReference.
     */
    public Supplier<LongArrayValues> newLongArrayReference() {
        return BinaryLongArrayReference::new;
    }

    /**
     * Converts a given marshallable object to its string representation.
     * This method ensures the object is first converted to a byte buffer,
     * and then the buffer's contents are returned as a string.
     *
     * @param marshallable The object to be converted to string.
     * @return The string representation of the object.
     */
    public String asString(Object marshallable) {
        ValidatableUtil.startValidateDisabled();
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            final Bytes<?> bytes = stlBytes.get();
            asBytes(marshallable, bytes);
            return bytes.toString();
        } finally {
            ValidatableUtil.endValidateDisabled();
        }
    }

    /**
     * Converts the given marshallable object to a {@link Bytes} buffer.
     * This method uses various strategies to serialize different types of
     * objects to a byte buffer, e.g., WriteMarshallable, Map, Iterable, etc.
     *
     * @param marshallable The object to be converted to bytes.
     * @return A Bytes buffer containing the serialized form of the object.
     * @throws InvalidMarshallableException If the object cannot be serialized properly.
     */
    @NotNull
    private void asBytes(Object marshallable, Bytes<?> bytes) throws InvalidMarshallableException {
        Wire wire = apply(bytes);
        wire.usePadding(wire.isBinary() && AbstractWire.DEFAULT_USE_PADDING);
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
    public <T> T fromString(@NotNull CharSequence cs) throws InvalidMarshallableException {
        return (T) fromString(/* Allow Marshallable tuples by not requesting  Object */ null, cs);
    }

    /**
     * deserializes as a given class
     *
     * @param tClass to serialize as
     * @param cs     text to deserialize
     * @return the object deserialized
     */
    public <T> T fromString(Class<T> tClass, @NotNull CharSequence cs) throws InvalidMarshallableException {
        if (cs.length() == 0)
            throw new IllegalArgumentException("cannot deserialize an empty string");
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            bytes.appendUtf8(cs);
            Wire wire = apply(bytes);
            return wire.getValueIn().object(tClass);
        }
    }

    /**
     * Deserializes an object of generic type from a file.
     *
     * @param filename The path to the file containing the serialized object.
     * @param <T> The type of the object to be deserialized.
     * @return The deserialized object.
     * @throws IOException If there's an error reading the file.
     * @throws InvalidMarshallableException If the object cannot be properly deserialized.
     */
    @NotNull
    public <T> T fromFile(String filename) throws IOException, InvalidMarshallableException {
        return (T) fromFile(Marshallable.class, filename);
    }

    /**
     * Deserializes an object of a specified type from a file.
     *
     * @param expectedType The expected type of the object to be deserialized.
     * @param filename The path to the file containing the serialized object.
     * @param <T> The type of the object to be deserialized.
     * @return The deserialized object, or null if the object could not be deserialized.
     * @throws IOException If there's an error reading the file.
     * @throws InvalidMarshallableException If the object cannot be properly deserialized.
     */
    @Nullable
    public <T> T fromFile(@NotNull Class<T> expectedType, String filename) throws IOException, InvalidMarshallableException {
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

    /**
     * Streams objects of generic type from a file.
     *
     * @param filename The path to the file containing the serialized objects.
     * @param <T> The type of the objects to be streamed.
     * @return A stream of the deserialized objects.
     * @throws IOException If there's an error reading the file.
     */
    @NotNull
    public <T> Stream<T> streamFromFile(String filename) throws IOException {
        return streamFromFile((Class) Marshallable.class, filename);
    }

    /**
     * Streams objects of a specified type from a file.
     *
     * @param expectedType The expected type of the objects to be streamed.
     * @param filename The path to the file containing the serialized objects.
     * @param <T> The type of the objects to be streamed.
     * @return A stream of the deserialized objects.
     * @throws IOException If there's an error reading the file.
     */
    @NotNull
    public <T> Stream<T> streamFromFile(@NotNull Class<T> expectedType, String filename) throws IOException {
        Bytes<?> b = BytesUtil.readFile(filename);
        return streamFromBytes(expectedType, b);
    }

    /**
     * Streams objects of a specified type from a {@link Bytes} instance.
     *
     * @param expectedType The expected type of the objects to be streamed.
     * @param b The {@link Bytes} instance containing the serialized objects.
     * @param <T> The type of the objects to be streamed.
     * @return A stream of the deserialized objects.
     */
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

    /**
     * Writes a {@link WriteMarshallable} object to a file.
     *
     * @param filename The name of the file to write to.
     * @param marshallable The object to write.
     * @throws IOException If there's an error writing to the file.
     * @throws InvalidMarshallableException If the object cannot be properly serialized.
     */
    public void toFile(@NotNull String filename, WriteMarshallable marshallable) throws IOException, InvalidMarshallableException {
        String tempFilename = IOTools.tempName(filename);
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = apply(bytes);
            wire.getValueOut().typedMarshallable(marshallable);
            IOTools.writeFile(tempFilename, bytes.toByteArray());
        }
        @NotNull File file2 = new File(tempFilename);
        if (!file2.renameTo(new File(filename))) {
            file2.delete();
            throw new IOException("Failed to rename " + tempFilename + " to " + filename);
        }
    }

    /**
     * Converts a Marshallable object to its HexString representation.
     *
     * @param marshallable The object to convert.
     * @return A HexString representation of the object.
     */
    @NotNull
    String asHexString(Object marshallable) {
        ValidatableUtil.startValidateDisabled();
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            final Bytes<?> bytes = stlBytes.get();
            asBytes(marshallable, bytes);
            return bytes.toHexString();
        } finally {
            ValidatableUtil.endValidateDisabled();
        }
    }

    /**
     * Deserializes an object from its HexString representation.
     *
     * @param s The HexString to deserialize from.
     * @param <T> The type of the deserialized object.
     * @return The deserialized object.
     * @throws InvalidMarshallableException If the HexString cannot be properly deserialized.
     */
    @Nullable <T> T fromHexString(@NotNull CharSequence s) throws InvalidMarshallableException {
        Bytes<?> bytes = Bytes.fromHexString(s.toString());
        try {
            Wire wire = apply(bytes);
            return wire.getValueIn().typedMarshallable();
        } finally {
            bytes.releaseLast();
        }
    }

    /**
     * Converts the provided CharSequence into a Map&lt;String, Object&gt; representation using Wire.
     *
     * @param cs The CharSequence to be converted.
     * @return A Map with String keys and Object values.
     * @throws InvalidMarshallableException If the CharSequence cannot be properly deserialized.
     */
    @Nullable
    public Map<String, Object> asMap(@NotNull CharSequence cs) throws InvalidMarshallableException {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            bytes.appendUtf8(cs);
            Wire wire = apply(bytes);
            return wire.getValueIn().marshallableAsMap(String.class, Object.class);
        }
    }

    @Override
    public void licenceCheck() {
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Indicates if this WireType is of a textual nature.
     * This implementation returns false, indicating it's not textual.
     *
     * @return true if the WireType is textual; false otherwise.
     */
    public boolean isText() {
        return false;
    }
}
