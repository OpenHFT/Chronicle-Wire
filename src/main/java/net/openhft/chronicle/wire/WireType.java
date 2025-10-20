/*
 * Copyright 2016-2025 chronicle.software
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
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collection;
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
 * data can be serialised and deserialised. Each enum constant acts as a factory
 * ({@code Function&lt;Bytes&lt;?>, Wire>}) creating a concrete {@link Wire} instance
 * initialised with a provided {@link Bytes} buffer.
 * <p>
 * Helper methods are supplied to obtain temporary {@link Bytes} for
 * serialisation operations.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public enum WireType implements Function<Bytes<?>, Wire>, LicenceCheck {

    /**
     * A human-readable, YAML-like text wire format. If the system property
     * {@code wire.testAsYaml} is true it behaves like {@link #YAML}.
     * Uses binary documents and padding by default.
     */
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
     * High performance binary wire format. This is an alias for
     * {@link #BINARY_LIGHT}.
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
     * High performance binary wire format optimised for speed and size.
     * Does not support legacy DeltaWire features.
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
    /**
     * Compact binary wire format that omits field names. Deserialisation relies
     * on DTO field order.
     */
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
    /**
     * <b>Deprecated: To be removed in x.29. Use {@link #BINARY_LIGHT}.</b>
     * Binary wire format that formerly supported LZW style compression for
     * messages exceeding {@link #COMPRESSED_SIZE}.
     */
    @Deprecated
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
    /**
     * JSON-compliant text wire format. Uses binary documents and padding by
     * default and does not include type prefixes unless configured.
     */
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

        @Override
        public String asString(Object marshallable) {
            return asUtf8String(marshallable);
        }
    },
    /**
     * JSON-compliant text wire format that uses explicit type prefixes and
     * text document delimiters. The first curly brace is not trimmed by
     * default.
     */
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

        @Override
        public String asString(Object marshallable) {
            return asUtf8String(marshallable);
        }
    },
    /**
     * YAML-compliant text wire format. Uses binary documents and padding by
     * default.
     */
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
    /**
     * YAML-compliant text wire format using text document delimiters
     * such as {@code ---} and {@code ...}.
     */
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
    /**
     * Raw binary wire format with minimal metadata. Ideal for fixed-layout
     * data and offers the highest performance.
     */
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
    /**
     * Comma Separated Values text wire format.
     */
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
    /**
     * Special wire type that detects either {@link #TEXT} or {@link #BINARY}
     * (including {@link #FIELDLESS_BINARY}) by inspecting the input stream.
     */
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
     * Serialises {@code marshallable} using this wire type into an in-memory
     * buffer and returns the result as a UTF-8 string.
     */
    protected @NotNull String asUtf8String(Object marshallable) {
        ValidatableUtil.startValidateDisabled();
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            final Bytes<?> bytes = stlBytes.get();
            asBytes(marshallable, bytes);
            return bytes.toUtf8String();
        } finally {
            ValidatableUtil.endValidateDisabled();
        }
    }

    /**
     * Determines the {@code WireType} of a given {@link Wire}. The concrete
     * wire is inspected and mapped to the matching enum constant.
     *
     * @param wire the wire instance to examine
     * @return the matching {@code WireType} or {@code null} if {@code wire} is null
     * @throws IllegalStateException if the type is unknown
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
     * Deserialises an object of an inferred type from the provided text.
     *
     * @param cs  text to read
     * @param <T> expected type
     * @return the object deserialised
     * @throws ClassCastException if the object is not of type {@code T}
     */
    @Nullable
    public <T> T fromString(@NotNull CharSequence cs) throws InvalidMarshallableException {
        return (T) fromString(/* Allow Marshallable tuples by not requesting  Object */ null, cs);
    }

    /**
     * Deserialises an object of the given type from the provided text.
     *
     * @param tClass the expected type
     * @param cs     text to parse
     * @return the object deserialised
     */
    public <T> T fromString(Class<T> tClass, @NotNull CharSequence cs) throws InvalidMarshallableException {
        if (cs.length() == 0)
            throw new IllegalArgumentException("cannot deserialize an empty string");
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            bytes.appendUtf8(cs);
            Wire wire = apply(bytes);

            T object = wire.getValueIn().object(tClass);
            cleanNullCollections(object);
            return object;
        }
    }

    /**
     * Internal helper that removes a single {@code null} element from
     * collections created when reading certain empty lists.
     */
    private void cleanNullCollections(Object object) {
        if (object == null) return;
        Field[] declaredFields = object.getClass().getDeclaredFields();

        for (int i=0; i<declaredFields.length; i++) {
            Field field = declaredFields[i];
            if (!Collection.class.isAssignableFrom(field.getType())) continue;

            try {
                field.setAccessible(true);
                Object fieldValue = field.get(object);

                if (fieldValue instanceof Collection) {
                    Collection<?> collection = (Collection<?>) fieldValue;
                    if (collection.size() == 1 && collection.iterator().next() == null) {
                        field.set(object, null);
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new InvalidMarshallableException("Failed cleaning null collection during processing field: " + field.getName());
            }
        }
    }

    /**
     * Deserialises a {@link Marshallable} from the given file.
     *
     * @param filename path to the input file
     * @param <T>      the desired type
     * @return the object read from the file
     * @throws IOException                   if the file cannot be read
     * @throws InvalidMarshallableException if decoding fails
     */
    @NotNull
    public <T> T fromFile(String filename) throws IOException, InvalidMarshallableException {
        return (T) fromFile(Marshallable.class, filename);
    }

    /**
     * Deserialises an object of the given type from a file produced in this
     * wire format.
     *
     * @param expectedType type to read
     * @param filename     input file path
     * @param <T>          object type
     * @return the object read from the file
     * @throws IOException                   if the file cannot be read
     * @throws InvalidMarshallableException if decoding fails
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
     * Deserialises a sequence of objects from the specified file.
     *
     * @param filename file to read
     * @param <T>      object type
     * @return a stream of objects
     * @throws IOException if the file cannot be read
     */
    @NotNull
    public <T> Stream<T> streamFromFile(String filename) throws IOException {
        return streamFromFile((Class) Marshallable.class, filename);
    }

    /**
     * Deserialises objects of the given type from a file.
     *
     * @param expectedType expected object type
     * @param filename     file to read
     * @param <T>          object type
     * @return a stream of objects
     * @throws IOException if the file cannot be read
     */
    @NotNull
    public <T> Stream<T> streamFromFile(@NotNull Class<T> expectedType, String filename) throws IOException {
        Bytes<?> b = BytesUtil.readFile(filename);
        return streamFromBytes(expectedType, b);
    }

    /**
     * Creates a {@link Stream} of objects by deserialising documents from the
     * provided {@link Bytes} buffer.
     *
     * @param expectedType expected object type
     * @param b            source bytes
     * @param <T>          object type
     * @return a stream of objects
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
     * Serialises {@code marshallable} to {@code filename} using this wire type.
     * The data is first written to a temporary file which is then renamed for
     * atomicity.
     *
     * @param filename     target file
     * @param marshallable object to write
     * @throws IOException                   if the file cannot be written
     * @throws InvalidMarshallableException if encoding fails
     */
    public void toFile(@NotNull String filename, WriteMarshallable marshallable) throws IOException, InvalidMarshallableException {
        String tempFilename = tempName(filename);
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = apply(bytes);
            wire.getValueOut().typedMarshallable(marshallable);
            writeFile(tempFilename, bytes.toByteArray());
        }
        @NotNull File file2 = new File(tempFilename);
        if (!file2.renameTo(new File(filename))) {
            file2.delete();
            throw new IOException("Failed to rename " + tempFilename + " to " + filename);
        }
    }

    /**
     * Serialises {@code marshallable} and returns a hexadecimal string
     * representation. Used by binary wire types for their text form.
     *
     * @param marshallable object to encode
     * @return hex string of the encoded data
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
     * Deserialises an object from a hexadecimal string. Used by binary wire
     * types for {@link #fromString(CharSequence)} implementations.
     *
     * @param s   hex string to parse
     * @param <T> object type
     * @return the deserialised object
     * @throws InvalidMarshallableException if decoding fails
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
     * Deserialises the supplied text into a {@code Map<String, Object>}.
     *
     * @param cs text formatted according to this wire type
     * @return a map representation
     * @throws InvalidMarshallableException if parsing fails
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
