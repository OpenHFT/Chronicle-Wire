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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.pool.EnumCache;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.scoped.ScopedResourcePool;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.wire.internal.StringConsumerMarshallableOut;
import net.openhft.compiler.CachedCompiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static net.openhft.chronicle.core.util.ReadResolvable.readResolve;
import static net.openhft.chronicle.wire.SerializationStrategies.*;
import static net.openhft.chronicle.wire.WireType.TEXT;
import static net.openhft.chronicle.wire.WireType.YAML_ONLY;

@SuppressWarnings({"rawtypes", "unchecked"})
/**
 * The {@code Wires} enum encapsulates constants and utility methods related to wire operations.
 * It defines flags, masks, and utility methods that facilitate operations on wires.
 * For example, it provides constants like {@code NOT_COMPLETE} to indicate a wire message's completeness status.
 * This enum doesn't have any specific enumeration values but provides a utility-based structure.
 */
public enum Wires {
    ; // No specific enumeration values

    // Constants defining various masks and flags for wire operations
    public static final int LENGTH_MASK = -1 >>> 2;
    public static final int NOT_COMPLETE = 0x8000_0000;
    public static final int META_DATA = 1 << 30;
    public static final int UNKNOWN_LENGTH = 0x0;
    // value to use when the message is not ready and of an unknown length
    public static final int NOT_COMPLETE_UNKNOWN_LENGTH = NOT_COMPLETE;
    // value to use when no more data is possible e.g. on a roll.
    public static final int END_OF_DATA = NOT_COMPLETE | META_DATA;
    public static final int NOT_INITIALIZED = 0x0;
    public static final Bytes<?> NO_BYTES = BytesStore.empty().bytesForRead();
    // Size of the SPB header
    public static final int SPB_HEADER_SIZE = 4;
    public static final List<Function<Class<?>, SerializationStrategy>> CLASS_STRATEGY_FUNCTIONS = new CopyOnWriteArrayList<>();

    static final ClassLocal<SerializationStrategy> CLASS_STRATEGY = ClassLocal.withInitial(c -> {
        for (@NotNull Function<Class<?>, SerializationStrategy> func : CLASS_STRATEGY_FUNCTIONS) {
            final SerializationStrategy strategy = func.apply(c);
            if (strategy != null)
                return strategy;
        }
        return ANY_OBJECT;
    });

    // Class local storage for field information lookup based on class
    static final ClassLocal<FieldInfoPair> FIELD_INFOS = ClassLocal.withInitial(FieldInfo::lookupClass);

    // Function to produce Marshallable objects based on a given class type
    static final ClassLocal<Function<String, Marshallable>> MARSHALLABLE_FUNCTION = ClassLocal.withInitial(tClass -> {
        Class[] interfaces = {Marshallable.class, tClass};
        if (tClass == Marshallable.class)
            interfaces = new Class[]{Marshallable.class};
        try {
            @SuppressWarnings("deprecation")
            Class<?> proxyClass = Proxy.getProxyClass(tClass.getClassLoader(), interfaces);
            Constructor<?> constructor = proxyClass.getConstructor(InvocationHandler.class);
            constructor.setAccessible(true);
            return typeName -> newInstance(constructor, typeName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    });
    static final ScopedResourcePool<StringBuilder> STRING_BUILDER_SCOPED_RESOURCE_POOL = StringBuilderPool.createThreadLocal();
    // Thread local storage for BinaryWire instances
    static final ThreadLocal<BinaryWire> WIRE_TL = ThreadLocal.withInitial(() -> new BinaryWire(Bytes.allocateElasticOnHeap()));
    // Flag to determine if code dump is enabled
    static final boolean DUMP_CODE_TO_TARGET = Jvm.getBoolean("dumpCodeToTarget", Jvm.isDebug());
    // Constants related to thread identifiers
    private static final int TID_MASK = 0b00111111_11111111_11111111_11111111;
    private static final int INVERSE_TID_MASK = ~TID_MASK;
    // Flag to enable tuple generation
    public static boolean GENERATE_TUPLES = Jvm.getBoolean("wire.generate.tuples");
    // Flags to assist with state management and logging
    static volatile boolean warnedUntypedBytesOnce = false;
    // Thread local storage for string builders
    static ThreadLocal<StringBuilder> sb = ThreadLocal.withInitial(StringBuilder::new);
    // Compiler cache for dynamic code generation
    static CachedCompiler CACHED_COMPILER = null;

    /**
     * Static initializer block for the Wires enum. It populates the list of
     * class strategy functions and adds some default strategies for serialization.
     * It also sets up wire aliases.
     */
    static {
        Jvm.addToClassPath(Wires.class);
        CLASS_STRATEGY_FUNCTIONS.add(SerializeEnum.INSTANCE);
        CLASS_STRATEGY_FUNCTIONS.add(SerializeJavaLang.INSTANCE);
        CLASS_STRATEGY_FUNCTIONS.add(SerializeBytes.INSTANCE);
        CLASS_STRATEGY_FUNCTIONS.add(SerializeMarshallables.INSTANCE); // must be after SerializeBytes.
        WireInternal.addAliases();
    }

    /**
     * A utility method to force the static initialization of this enum.
     * Calling this method ensures that the static initializer block is executed.
     */
    public static void init() {
        // Do nothing here
    }

    /**
     * Creates and returns a proxy of the specified interface. The proxy writes inputs into
     * the specified PrintStream in Yaml format. This is useful when there's a need to serialize
     * method calls in a readable Yaml format.
     *
     * @param tClass the specified interface class to create a proxy of
     * @param ps     the PrintStream used to write serialized method calls into, in Yaml format
     * @param <T>    the type of the specified interface
     * @return a proxy of the specified interface that writes to the PrintStream in Yaml
     */
    public static <T> T recordAsYaml(Class<T> tClass, PrintStream ps) {
        MarshallableOut out = new StringConsumerMarshallableOut(s -> {
            if (!s.startsWith("---\n"))
                ps.print("---\n");
            ps.print(s);
            if (!s.endsWith("\n"))
                ps.print("\n");
        }, YAML_ONLY);
        return out.methodWriter(tClass);
    }

    /**
     * Reads the content of a specified Yaml file and replays the serialized method calls
     * to the specified object. This can be used to re-execute or simulate a sequence of
     * operations captured in the Yaml file.
     *
     * @param file the name of the input Yaml file containing serialized method calls
     * @param obj  the target object that the method calls will be replayed on
     * @throws IOException is thrown if there's an error reading the file
     * @throws InvalidMarshallableException is thrown if the serialized data is invalid or corrupted
     */
    public static void replay(String file, Object obj) throws IOException, InvalidMarshallableException {
        Bytes bytes = BytesUtil.readFile(file);
        Wire wire = new YamlWire(bytes).useTextDocuments();
        MethodReader readerObj = wire.methodReader(obj);
        while (readerObj.readOne()) {
        }
        bytes.releaseLast();
    }

    /**
     * This decodes some Bytes where the first 4-bytes is the length.  e.g. Wire.writeDocument wrote
     * it. <a href="https://github.com/OpenHFT/RFC/tree/master/Size-Prefixed-Blob">Size Prefixed
     * Blob</a>
     *
     * @param bytes to decode
     * @return as String
     */
    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes) {
        return WireDumper.of(bytes).asString();
    }

    /**
     * Converts the provided bytes, which represent aligned size-prefixed blobs,
     * into a readable string format.
     *
     * @param bytes the bytes representing aligned size-prefixed blobs
     * @return the readable string representation of the blobs
     */
    public static String fromAlignedSizePrefixedBlobs(@NotNull Bytes<?> bytes) {
        return WireDumper.of(bytes, true).asString();
    }

    /**
     * Converts the provided bytes, representing size-prefixed blobs,
     * into a readable string format with the option to abbreviate.
     *
     * @param bytes   the bytes representing size-prefixed blobs
     * @param abbrev  if {@code true}, the output string will be abbreviated
     * @return the readable string representation of the blobs
     */
    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes, boolean abbrev) {
        return WireDumper.of(bytes).asString(abbrev);
    }

    /**
     * Converts the provided bytes, representing size-prefixed blobs,
     * into a readable string format from the specified position.
     *
     * @param bytes    the bytes representing size-prefixed blobs
     * @param position the position in bytes from which the conversion starts
     * @return the readable string representation of the blobs from the specified position
     */
    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes, long position) {
        return fromSizePrefixedBlobs(bytes, position, false);
    }

    /**
     * Converts the provided bytes, representing size-prefixed blobs,
     * into a readable string format from a specified position and with the option to pad.
     *
     * @param bytes    the bytes representing size-prefixed blobs
     * @param position the position in bytes from which the conversion starts
     * @param padding  if {@code true}, the output string will have padding
     * @return the readable string representation of the blobs from the specified position
     */
    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes, long position, boolean padding) {
        final long limit = bytes.readLimit();
        if (position > limit)
            return "";
        return WireDumper.of(bytes, padding).asString(position, limit - position);
    }

    /**
     * Converts the provided bytes, which represent size-prefixed blobs,
     * into a readable string format with options for padding and abbreviation.
     *
     * @param bytes   the bytes representing size-prefixed blobs
     * @param padding if {@code true}, the output string will have padding
     * @param abbrev  if {@code true}, the output string will be abbreviated
     * @return the readable string representation of the blobs
     */
    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes, boolean padding, boolean abbrev) {
        return WireDumper.of(bytes, padding).asString(abbrev);
    }

    /**
     * Converts the contents of the provided {@code DocumentContext}
     * which represent size-prefixed blobs into a readable string format.
     * The method supports handling of both TextWire and BinaryWire types.
     *
     * @param dc the {@code DocumentContext} holding the wire data
     * @return the readable string representation of the blobs
     */
    public static String fromSizePrefixedBlobs(@NotNull DocumentContext dc) {
        Wire wire = dc.wire();
        Bytes<?> bytes = wire.bytes();
        if (wire instanceof TextWire) {
            // Return the direct string representation for TextWire
            return bytes.toString();
        }

        long headerPosition;

        long length;
        // Check if the document context is of type 'BufferedTailer'
        if ("BufferedTailer".equals(dc.getClass().getSimpleName())) {
            // Determine the length limit for the bytes
            length = wire.bytes().readLimit();
            // Determine the metadata bit
            int metaDataBit = dc.isMetaData() ? Wires.META_DATA : 0;
            // Compute the header based on the metadata and length
            int header = metaDataBit | toIntU30(length, "Document length %,d out of 30-bit int range.");

            // Create a temporary bytes object to write data
            Bytes<?> tempBytes = Bytes.allocateElasticDirect();
            try {
                // Write the computed header to the temporary bytes
                tempBytes.writeOrderedInt(header);
                final Wire wire2 = ((BinaryReadDocumentContext) dc).wire;
                // Copy data from the original wire to the temporary bytes
                tempBytes.write(wire2.bytes(), 0, wire2.bytes().readLimit());

                // Derive the wire type and apply it to the temporary bytes
                final WireType wireType = WireType.valueOf(wire);

                assert wireType != null;
                Wire tempWire = wireType.apply(tempBytes);

                // Return the string representation of the data
                return WireDumper.of(tempWire).asString(0, length + 4);

            } finally {
                // Ensure to release the temporary bytes after use
                tempBytes.releaseLast();
            }
        } else {
            // Handle the case for 'BinaryReadDocumentContext' type
            if (dc instanceof BinaryReadDocumentContext) {
                long start = ((BinaryReadDocumentContext) dc).lastStart;
                if (start != -1)
                    headerPosition = start;
                else
                    headerPosition = bytes.readPosition() - 4;
            } else {
                // Default handling for header position calculation
                headerPosition = bytes.readPosition() - 4;
            }

            // Compute the length from the header position
            length = Wires.lengthOf(bytes.readInt(headerPosition));
        }

        // Return the string representation of the wire data from the computed header position
        return WireDumper.of(wire).asString(headerPosition, length + 4);
    }

    /**
     * Converts the contents of the provided {@code WireIn}
     * which represent size-prefixed blobs into a readable string format.
     *
     * @param wireIn the {@code WireIn} instance holding the wire data
     * @return the readable string representation of the blobs
     */
    public static String fromSizePrefixedBlobs(@NotNull WireIn wireIn) {
        return fromSizePrefixedBlobs(wireIn, false);
    }

    /**
     * Converts the contents of the provided {@code WireIn}
     * which represent size-prefixed blobs into a readable string format,
     * with the option to abbreviate the output.
     *
     * @param wireIn the {@code WireIn} instance holding the wire data
     * @param abbrev  if {@code true}, the output string will be abbreviated
     * @return the readable string representation of the blobs
     */
    public static String fromSizePrefixedBlobs(@NotNull WireIn wireIn, boolean abbrev) {
        return WireDumper.of(wireIn).asString(abbrev);
    }

    @NotNull
    public static CharSequence asText(@NotNull WireIn wireIn, Bytes<?> output) {
        ValidatableUtil.startValidateDisabled();
        try {
            return asType(wireIn, Wires::newTextWire, output);
        } finally {
            ValidatableUtil.endValidateDisabled();
        }
    }

    /**
     * Creates a new JSONWire instance.
     *
     * @param bytes the byte buffer for the wire
     * @return a new instance of JSONWire
     */
    private static Wire newJsonWire(Bytes<?> bytes) {
        return new JSONWire(bytes).useTypes(true).trimFirstCurly(false).useTextDocuments();
    }

    public static Bytes<?> asBinary(@NotNull WireIn wireIn, Bytes<?> output) throws InvalidMarshallableException {
        return asType(wireIn, BinaryWire::new, output);
    }

    /**
     * Converts the given WireIn instance into a specified wire type representation.
     *
     * @param wireIn the input wire
     * @param wireProvider a function that provides a specific type of wire based on bytes
     * @return the representation of the wire data in the specified type
     * @throws InvalidMarshallableException if marshalling fails
     */
    private static Bytes<?> asType(@NotNull WireIn wireIn, Function<Bytes, Wire> wireProvider, Bytes<?> output) throws InvalidMarshallableException {
        long pos = wireIn.bytes().readPosition();
        try {
            wireIn.copyTo(new TextWire(output).addTimeStamps(true));
            return output;
        } finally {
            wireIn.bytes().readPosition(pos);
        }
    }

    public static Bytes<?> asJson(@NotNull WireIn wireIn, Bytes<?> output) throws InvalidMarshallableException {
        return asType(wireIn, Wires::newJsonWire, output);
    }

    /**
     * Creates a new TextWire instance with timestamps.
     *
     * @param bytes the byte buffer for the wire
     * @return a new instance of TextWire with timestamps
     */
    private static Wire newTextWire(Bytes<?> bytes) {
        return new TextWire(bytes).addTimeStamps(true);
    }

    public static ScopedResource<StringBuilder> acquireStringBuilderScoped() {
        return STRING_BUILDER_SCOPED_RESOURCE_POOL.get();
    }

    /**
     * Extracts the length from the given length value.
     *
     * @param len the encoded length
     * @return the decoded length
     */
    public static int lengthOf(int len) {
        return len & LENGTH_MASK;
    }

    /**
     * Checks if the given header is ready (complete and not zero).
     *
     * @param header the input header
     * @return true if the header is ready, false otherwise
     */
    public static boolean isReady(int header) {
        return (header & NOT_COMPLETE) == 0 && header != 0;
    }

    /**
     * Checks if the given header is not complete or zero.
     *
     * @param header the input header
     * @return true if the header is not complete or zero, false otherwise
     */
    public static boolean isNotComplete(int header) {
        return (header & NOT_COMPLETE) != 0 || header == 0;
    }

    /**
     * Checks if the given header represents ready data (neither meta-data nor incomplete) and is not zero.
     *
     * @param header the input header
     * @return true if the header represents ready data, false otherwise
     */
    public static boolean isReadyData(int header) {
        return ((header & (META_DATA | NOT_COMPLETE)) == 0) && (header != 0);
    }

    /**
     * Checks if the given length represents data and not meta-data.
     *
     * @param len the encoded length
     * @return true if the length represents data, false otherwise
     */
    public static boolean isData(int len) {
        return (len & META_DATA) == 0;
    }

    /**
     * Checks if the given length represents ready meta-data.
     *
     * @param len the encoded length
     * @return true if the length represents ready meta-data, false otherwise
     */
    public static boolean isReadyMetaData(int len) {
        return (len & (META_DATA | NOT_COMPLETE)) == META_DATA;
    }

    /**
     * Checks if the given length represents a known length (neither unknown nor meta-data).
     *
     * @param len the encoded length
     * @return true if the length is known, false otherwise
     */
    public static boolean isKnownLength(int len) {
        return (len & (META_DATA | LENGTH_MASK)) != UNKNOWN_LENGTH;
    }

    /**
     * Checks if the given length is not initialized.
     *
     * @param len the encoded length
     * @return true if the length is not initialized, false otherwise
     */
    public static boolean isNotInitialized(int len) {
        return len == NOT_INITIALIZED;
    }

    /**
     * Converts a long value to an int, ensuring it falls within the 30-bit range.
     *
     * @param l     the input long value
     * @param error the error message template in case of an out-of-range value
     * @return the converted int value
     * @throws IllegalStateException if the value is out of the 30-bit range
     */
    public static int toIntU30(long l, @NotNull String error) {
        if (l < 0 || l > LENGTH_MASK)
            throw new IllegalStateException(String.format(error, l));
        return (int) l;
    }

    /**
     * Acquires a lock on the given BytesStore at the specified position.
     *
     * @param store    the byte store to lock
     * @param position the position at which to lock
     * @return true if the lock was successfully acquired, false otherwise
     */
    public static boolean acquireLock(@NotNull BytesStore<?, ?> store, long position) {
        return store.compareAndSwapInt(position, NOT_INITIALIZED, NOT_COMPLETE);
    }

    /**
     * Checks if the given length exceeds the maximum allowed length.
     *
     * @param length the length to check
     * @return true if the length exceeds the maximum, false otherwise
     */
    public static boolean exceedsMaxLength(long length) {
        return length > LENGTH_MASK;
    }

    /**
     * Writes data to the given WireOut using the provided writer.
     * It is inlined for performance optimization.
     *
     * @param wireOut the destination to write data
     * @param writer  the WriteMarshallable instance to write data
     * @return the position after writing the data
     * @throws InvalidMarshallableException if marshalling fails
     */
    @ForceInline
    public static <T extends WriteMarshallable> long writeData(
            @NotNull WireOut wireOut,
            @NotNull T writer) throws InvalidMarshallableException {
        return WireInternal.writeData(wireOut, false, false, writer);
    }

    /**
     * Reads data from a WireIn up to a specified size using a ReadMarshallable instance.
     *
     * @param wireIn          the source from which data is read
     * @param size            the maximum size of data to be read
     * @param readMarshallable the ReadMarshallable instance to interpret the data
     * @return the position in the bytes after reading
     * @throws InvalidMarshallableException if there's an issue during marshalling
     */
    @ForceInline
    public static long readWire(@NotNull WireIn wireIn, long size, @NotNull ReadMarshallable readMarshallable) throws InvalidMarshallableException {
        @NotNull final Bytes<?> bytes = wireIn.bytes();
        final long limit0 = bytes.readLimit();
        final long limit = bytes.readPosition() + size;
        try {
            bytes.readLimit(limit);
            readMarshallable.readMarshallable(wireIn);
        } finally {
            bytes.readLimit(limit0);
            bytes.readPosition(limit);
        }

        return bytes.readPosition();
    }

    /**
     * Allocates a Bytes instance that is direct and unmonitored.
     *
     * @return the created Bytes instance
     */
    static Bytes<Void> unmonitoredDirectBytes() {
        Bytes<Void> bytes = Bytes.allocateElasticDirect(128);
        IOTools.unmonitor(bytes);
        return bytes;
    }

    @NotNull
    public static ScopedResource<Bytes<Void>> acquireBytesScoped() {
        return WireInternal.BYTES_SCOPED_THREAD_LOCAL.get();
    }

    public static ScopedResource<Wire> acquireBinaryWireScoped() {
        return WireInternal.BINARY_WIRE_SCOPED_TL.get();
    }

    /**
     * Creates a string representation of a specific portion of size-prefixed blob data.
     *
     * @param bytes    the source of the blob data
     * @param position the starting position for extraction
     * @param length   the length of data to extract
     * @return a string representation of the extracted data
     */
    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes, long position, long length) {
        return WireDumper.of(bytes).asString(position, length);
    }

    /**
     * Reads a Marshallable object's fields from a WireIn instance.
     *
     * @param marshallable the object whose fields need to be populated
     * @param wire         the source from which data will be read
     * @param overwrite    a flag indicating whether existing fields should be overwritten
     * @throws InvalidMarshallableException if there's an issue during the marshalling process
     */
    public static void readMarshallable(@NotNull Object marshallable, @NotNull WireIn wire, boolean overwrite) throws InvalidMarshallableException {
        final Class<?> clazz = marshallable.getClass();
        readMarshallable(clazz, marshallable, wire, overwrite);
    }

    /**
     * Reads a Marshallable object's fields from a WireIn instance using a specified class type.
     *
     * @param clazz        the class type of the Marshallable object
     * @param marshallable the object whose fields need to be populated
     * @param wire         the source from which data will be read
     * @param overwrite    a flag indicating whether existing fields should be overwritten
     * @throws InvalidMarshallableException if there's an issue during the marshalling process
     */
    public static void readMarshallable(Class<?> clazz, @NotNull Object marshallable, @NotNull WireIn wire, boolean overwrite) throws InvalidMarshallableException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(clazz == null ? marshallable.getClass() : clazz);
        wm.readMarshallable(marshallable, wire, overwrite);
    }

    /**
     * Writes a marshallable object's fields to a WireOut instance.
     *
     * @param marshallable the object whose fields are to be written
     * @param wire         the target WireOut to write data to
     * @throws InvalidMarshallableException if there's an error during the marshalling process
     */
    public static void writeMarshallable(@NotNull Object marshallable, @NotNull WireOut wire) throws InvalidMarshallableException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass());
        wm.writeMarshallable(marshallable, wire);
    }

    /**
     * Writes a marshallable object's fields to a WireOut instance with an option to use defaults.
     *
     * @param marshallable the object to be written
     * @param wire         the target WireOut to write data to
     * @param writeDefault indicates if default values should be written
     * @throws InvalidMarshallableException if there's an error during the marshalling process
     */
    public static void writeMarshallable(@NotNull Object marshallable, @NotNull WireOut wire, boolean writeDefault) throws InvalidMarshallableException {
        WireMarshaller marshaller = WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass());
        if (writeDefault)
            marshaller.writeMarshallable(marshallable, wire);
        else
            marshaller.writeMarshallable(marshallable, wire, false);
    }

    /**
     * Writes a marshallable object's fields to a WireOut instance considering previous state.
     *
     * @param marshallable the object to be written
     * @param wire         the target WireOut to write data to
     * @param previous     the previous state of the marshallable object
     * @param copy         indicates if the previous state should be copied
     * @throws InvalidMarshallableException if there's an error during the marshalling process
     */
    public static void writeMarshallable(@NotNull Object marshallable, @NotNull WireOut wire, @NotNull Object previous, boolean copy) throws InvalidMarshallableException {
        assert marshallable.getClass() == previous.getClass();
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass());
        wm.writeMarshallable(marshallable, wire, copy);
    }

    /**
     * Writes the key associated with the marshallable object to the provided bytes.
     *
     * @param marshallable the object whose key needs to be written
     * @param bytes        the target bytes to write the key to
     */
    public static void writeKey(@NotNull Object marshallable, Bytes<?> bytes) {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass());
        wm.writeKey(marshallable, bytes);
    }

    /**
     * Creates a deep copy of the provided marshallable object.
     *
     * @param marshallable the object to be deeply copied
     * @param <T>          the type of marshallable object
     * @return a new instance that's a deep copy of the provided marshallable
     * @throws InvalidMarshallableException if there's an error during the copy process
     */
    @NotNull
    public static <T extends Marshallable> T deepCopy(@NotNull T marshallable) throws InvalidMarshallableException {
        if (Enum.class.isAssignableFrom(marshallable.getClass()))
            return marshallable;

        try (ScopedResource<Wire> wireSR = acquireBinaryWireScoped()) {
            Wire wire = wireSR.get();
            @NotNull T t = (T) ObjectUtils.newInstance(marshallable.getClass());
            boolean useSelfDescribing = t.usesSelfDescribingMessage() || !(t instanceof BytesMarshallable);
            if (useSelfDescribing) {
                marshallable.writeMarshallable(wire);
                t.readMarshallable(wire);
            } else {
                ((BytesMarshallable) marshallable).writeMarshallable(wire.bytes());
                ((BytesMarshallable) t).readMarshallable(wire.bytes());
            }
            return t;
        }
    }

    /**
     * Copy fields from source to target by marshalling out and then in. Allows copying of fields by name
     * even if there is no type relationship between the source and target
     *
     * @param source source
     * @param target dest
     * @param <T>    target type
     * @return target
     */
    @NotNull
    public static <T> T copyTo(Object source, @NotNull T target) throws InvalidMarshallableException {
        try (ScopedResource<Wire> wireSR = acquireBinaryWireScoped()) {
            ValidatableUtil.startValidateDisabled();
            Wire wire = wireSR.get();
            wire.getValueOut().object(source);
            wire.getValueIn().typePrefix(); // drop the type prefix.
            wire.getValueIn().object(target, (Class<T>) target.getClass());
            return target;
        } finally {
            ValidatableUtil.endValidateDisabled();
        }
    }

    /**
     * Projects the source object's fields into a new instance of a specified class.
     *
     * @param tClass The target class type to project to
     * @param source The source object
     * @param <T>    Type of the target class
     * @return A new instance of type T with the projected fields
     * @throws InvalidMarshallableException if there's an error during the projection process
     */
    @NotNull
    public static <T> T project(Class<T> tClass, Object source) throws InvalidMarshallableException {
        T target = ObjectUtils.newInstance(tClass);
        Wires.copyTo(source, target);
        return target;
    }

    /**
     * Checks if two objects are of the same type and equal based on their serialized representation.
     *
     * @param o1 First object
     * @param o2 Second object
     * @return true if objects are of the same type and equal, false otherwise
     */
    public static boolean isEquals(@NotNull Object o1, @NotNull Object o2) {
        return o1.getClass() == o2.getClass() && WireMarshaller.WIRE_MARSHALLER_CL.get(o1.getClass()).isEqual(o1, o2);
    }

    /**
     * Retrieves the list of field information for a given class.
     *
     * @param aClass Class to retrieve field information for
     * @return List of field information
     */
    @NotNull
    public static List<FieldInfo> fieldInfos(@NotNull Class<?> aClass) {
        return FIELD_INFOS.get(aClass).list;
    }

    /**
     * Retrieves a map of field names to their information for a given class.
     *
     * @param aClass Class to retrieve field information for
     * @return Map of field names to their information
     */
    public static @NotNull Map<String, FieldInfo> fieldInfoMap(@NotNull Class<?> aClass) {
        return FIELD_INFOS.get(aClass).map;
    }

    /**
     * Retrieves field information for a specific field of a given class.
     *
     * @param aClass Class to which the field belongs
     * @param name   Field name
     * @return Information about the field
     */
    public static FieldInfo fieldInfo(@NotNull Class<?> aClass, String name) {
        return FIELD_INFOS.get(aClass).map.get(name);
    }

    /**
     * Checks if a given integer represents the end of file marker.
     *
     * @param num Integer to check
     * @return true if the integer represents end of file, false otherwise
     */
    public static boolean isEndOfFile(int num) {
        return num == END_OF_DATA;
    }

    /**
     * Retrieves the value of a specific field from an object and converts it to the desired type.
     *
     * @param o      Object to retrieve the field value from
     * @param name   Field name
     * @param tClass Desired type to convert the field value to
     * @param <T>    Type parameter representing the desired type
     * @return The field value converted to the desired type
     * @throws NoSuchFieldException if the field doesn't exist
     */
    @Nullable
    public static <T> T getField(@NotNull Object o, String name, Class<T> tClass) throws NoSuchFieldException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(o.getClass());
        Object value = wm.getField(o, name);
        return ObjectUtils.convertTo(tClass, value);
    }

    /**
     * Retrieves the long value of a specific field from an object.
     *
     * @param o    Object to retrieve the field value from
     * @param name Field name
     * @return The long value of the specified field
     * @throws NoSuchFieldException if the specified field doesn't exist
     */
    public static long getLongField(@NotNull Object o, String name) throws NoSuchFieldException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(o.getClass());
        return wm.getLongField(o, name);
    }

    /**
     * Sets the value of a specific field in an object.
     *
     * @param o     Object in which the field value will be set
     * @param name  Field name
     * @param value New value to set
     * @throws NoSuchFieldException if the specified field doesn't exist
     */
    public static void setField(@NotNull Object o, String name, Object value) throws NoSuchFieldException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(o.getClass());
        wm.setField(o, name, value);
    }

    /**
     * Sets the long value of a specific field in an object.
     *
     * @param o     Object in which the field value will be set
     * @param name  Field name
     * @param value New long value to set
     * @throws NoSuchFieldException if the specified field doesn't exist
     */
    public static void setLongField(@NotNull Object o, String name, long value) throws NoSuchFieldException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(o.getClass());
        wm.setLongField(o, name, value);
    }

    /**
     * Resets the specified object's state to its default. The specifics of the reset
     * operation are determined by the associated WireMarshaller.
     *
     * @param o Object to be reset
     */
    public static void reset(@NotNull Object o) {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(o.getClass());
        wm.reset(o);
    }

    /**
     * Removes masked thread ID from a given header.
     *
     * @param header The header with a potentially masked thread ID
     * @return The header without the masked thread ID
     */
    public static int removeMaskedTidFromHeader(final int header) {
        return header & INVERSE_TID_MASK;
    }

    /**
     * Reads a sequence of objects using a specified serialization strategy.
     *
     * @param in       ValueIn instance providing the input data
     * @param using    Optional object to read the data into (can be null)
     * @param clazz    Class type of the object sequence, can be null for generic Object
     * @param strategy Serialization strategy to use for reading the sequence
     * @param <E>      Type parameter of the object sequence
     * @return A sequence of objects read using the provided serialization strategy
     */
    @Nullable
    public static <E> E objectSequence(ValueIn in, @Nullable E using, @Nullable Class<? extends E> clazz, SerializationStrategy strategy) {
        if (clazz == Object.class)
            strategy = LIST;
        if (using == null)
            using = strategy.newInstanceOrNull((Class<E>) clazz);

        SerializationStrategy finalStrategy = strategy;
        return in.sequence(using, (using1, in1) -> finalStrategy.readUsing(clazz, using1, in1, BracketType.UNKNOWN)) ? readResolve(using) : null;
    }

    /**
     * Reads a map of objects from the given input, using a specified serialization strategy.
     *
     * @param in       ValueIn instance providing the input data
     * @param using    Optional object to read the data into (can be null)
     * @param clazz    Class type of the object map, can be null for generic Object
     * @param strategy Serialization strategy to use for reading the map
     * @param <E>      Type parameter of the object map
     * @return A map of objects read using the provided serialization strategy
     * @throws InvalidMarshallableException If the deserialization process encounters an error
     */
    @Nullable
    public static <E> E objectMap(ValueIn in, @Nullable E using, @Nullable Class<? extends E> clazz, @NotNull SerializationStrategy strategy) throws InvalidMarshallableException {
        if (in.isNull())
            return null;

        // If the provided class is Object, use a generic map strategy.
        if (clazz == Object.class)
            strategy = MAP;

        // If no object is provided to populate, instantiate a new one using the strategy.
        if (using == null) {
            using = strategy.newInstanceOrNull((Class<E>) clazz);
        }

        // If the class represents a Throwable, deserialize it using a special method.
        if (Throwable.class.isAssignableFrom(clazz))
            return (E) WireInternal.throwable(in, false, (Throwable) using);

        // If we failed to create an instance of the object, throw an exception.
        if (using == null)
            throw new ClassNotFoundRuntimeException(new ClassNotFoundException("failed to create instance of clazz=" + clazz + " is it aliased?"));

        // Deserialize the object using the strategy.
        Object marshallable = in.marshallable(using, strategy);
        E e = readResolve(marshallable);

        // If the object is of a type that has a name (e.g., an Enum or CoreDynamicEnum), handle it specially.
        String name = nameOf(e);
        if (name != null) {
            Class<?> aClass = e.getClass();
            E e2 = (E) EnumCache.of(aClass).valueOf(name);

            // If the deserialized object and the cached version are not the same, update the cached version.
            if (e != e2) {
                try (ScopedResource<Wire> wireSR = Wires.acquireBinaryWireScoped()) {
                    Wire wire = wireSR.get();
                    WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(aClass);
                    wm.writeMarshallable(e, wire);
                    wm.readMarshallable(e2, wire, false);
                }
                return e2;
            }
        }
        return e;
    }

    /**
     * Obtains the name of the given object if it's either a CoreDynamicEnum or an Enum.
     *
     * @param e Object whose name is to be retrieved
     * @param <E> Type of the object
     * @return The name of the object or null if the object is neither a CoreDynamicEnum nor an Enum
     */
    private static <E> String nameOf(E e) {
        return e instanceof CoreDynamicEnum ? ((CoreDynamicEnum) e).name()
                : e instanceof Enum ? ((Enum) e).name() : null;
    }

    /**
     * Reads a date object from the given input.
     *
     * @param in    ValueIn instance providing the input data
     * @param using Optional Date object to read the data into (can be null)
     * @param <E>   Expected type (often Date) to be returned
     * @return A Date object representing the read date
     */
    @NotNull
    public static <E> E objectDate(ValueIn in, @Nullable E using) {
        // skip the field if it is there.
        in.wireIn().read();
        final long time = in.int64();
        if (using instanceof Date) {
            ((Date) using).setTime(time);
            return using;
        } else
            return (E) new Date(time);
    }

    /**
     * Reads and validates an object from the given input.
     *
     * @param in    ValueIn instance providing the input data
     * @param using Optional object to read the data into (can be null)
     * @param clazz Class type of the object, can be null for generic Object
     * @param <E>   Type parameter of the object
     * @return The validated object read from the input
     * @throws InvalidMarshallableException If the deserialization process encounters an error
     */
    @Nullable
    public static <E> E object0(ValueIn in, @Nullable E using, @Nullable Class<? extends E> clazz) throws InvalidMarshallableException {
        return ValidatableUtil.validate(object1(in, using, clazz, true));
    }

    /**
     * Deserialize an object from the provided input and validate it.
     *
     * @param in         The input source to read the serialized data from.
     * @param using      An optional object to populate with the deserialized data. Can be null.
     * @param clazz      The expected class type of the deserialized object. Can be null.
     * @param bestEffort Flag to determine the effort in deserialization. If false, exceptions may be thrown for mismatches.
     * @param <E>        The type of the object being deserialized.
     * @return The deserialized and validated object.
     * @throws InvalidMarshallableException If an error occurs during deserialization.
     */
    public static <E> E object0(ValueIn in, @Nullable E using, @Nullable Class<? extends E> clazz, boolean bestEffort) throws InvalidMarshallableException {
        return ValidatableUtil.validate(object1(in, using, clazz, bestEffort));
    }

    /**
     * Deserialize an object from the provided input based on the defined strategy and constraints.
     *
     * @param in         The input source to read the serialized data from.
     * @param using      An optional object to populate with the deserialized data. Can be null.
     * @param clazz      The expected class type of the deserialized object. Can be null.
     * @param bestEffort Flag to determine the effort in deserialization. If false, exceptions may be thrown for mismatches.
     * @param <E>        The type of the object being deserialized.
     * @return The deserialized object.
     * @throws InvalidMarshallableException If an error occurs during deserialization.
     */
    public static <E> E object1(ValueIn in, @Nullable E using, @Nullable Class<? extends E> clazz, boolean bestEffort) throws InvalidMarshallableException {
        Object o = in.typePrefixOrObject(clazz);
        if (o == null && using instanceof ReadMarshallable)
            o = using;
        if (o != null && !(o instanceof Class)) {
            return (E) in.marshallable(o, MARSHALLABLE);
        }
        return object2(in, using, clazz, bestEffort, (Class) o);
    }

    @Nullable
    static <E> E object2(ValueIn in, @Nullable E using, @Nullable Class<? extends E> clazz, boolean bestEffort, Class<?> o) {
        @Nullable final Class<?> clazz2 = o;
        if (clazz2 == void.class) {
            in.text();
            return null;
        } else if (clazz2 == BytesStore.class) {
            if (using == null)
                using = (E) Bytes.allocateElasticOnHeap(32);
            clazz = (Class<E>) Base64.class;
            bestEffort = true;
        }

        // Decide which class to use for deserialization.
        // The code also handles possible class mismatches and attempts to match as closely as possible.
        if (clazz2 == null && clazz != null) {
            clazz = ObjectUtils.implementationToUse(clazz);
        }

        if (clazz2 != null &&
                clazz != clazz2) {
            if (clazz == null
                    || clazz.isAssignableFrom(clazz2)
                    || ReadResolvable.class.isAssignableFrom(clazz2)
                    || !ObjectUtils.isConcreteClass(clazz)) {
                clazz = (Class<E>) clazz2;
                if (!clazz.isInstance(using))
                    using = null;
            } else if (!bestEffort && !(isScalarClass(clazz) && isScalarClass(clazz2))) {
                throw new ClassCastException("Unable to read a " + clazz2 + " as a " + clazz);
            }
        }
        if (clazz == null)
            clazz = (Class<E>) Object.class;
        Class<?> classForStrategy = clazz.isInterface() && using != null ? using.getClass() : clazz;
        SerializationStrategy strategy = CLASS_STRATEGY.get(classForStrategy);
        BracketType brackets = strategy.bracketType();
        if (brackets == BracketType.UNKNOWN)
            brackets = in.getBracketType();

        if (BitSet.class.isAssignableFrom(clazz)) {

            PrimArrayWrapper longWrapper = new PrimArrayWrapper(long[].class);
            objectSequence(in, longWrapper, PrimArrayWrapper.class, PRIM_ARRAY);

            return (using == null) ?
                    (E) BitSet.valueOf((long[]) longWrapper.array) :
                    (E) BitSetUtil.set((BitSet) using, (long[]) longWrapper.array);

        }

        // Handle various strategies for deserialization based on the BracketType.
        switch (brackets) {
            case MAP:
                return objectMap(in, using, clazz, strategy);

            case SEQ:
                return objectSequence(in, using, clazz, strategy);

            case NONE:
                @NotNull final Object e = strategy.readUsing(clazz, using, in, BracketType.NONE);
                return clazz == Base64.class || e == null
                        ? (E) e
                        : (E) WireInternal.intern(clazz, e);

            case UNKNOWN:
            case HISTORY_MESSAGE:
            default:
                throw new AssertionError();
        }
    }

    /**
     * Checks if the provided serializable object is considered scalar.
     *
     * @param object The object to check.
     * @return True if the object is scalar, false otherwise.
     */
    static boolean isScalar(Serializable object) {
        // If object implements Comparable, fetch the associated serialization strategy.
        if (object instanceof Comparable) {
            final SerializationStrategy strategy = Wires.CLASS_STRATEGY.get(object.getClass());
            // Return true only if the strategy is neither ANY_OBJECT nor ANY_NESTED.
            return strategy != ANY_OBJECT && strategy != ANY_NESTED;
        }
        return false;
    }

    /**
     * Checks if the provided class type is considered scalar.
     *
     * @param type The class type to check.
     * @return True if the class type is scalar, false otherwise.
     */
    static boolean isScalarClass(Class<?> type) {
        if (Comparable.class.isAssignableFrom(type)) {
            final SerializationStrategy strategy = Wires.CLASS_STRATEGY.get(type);
            // Return true only if the strategy is neither ANY_OBJECT nor ANY_NESTED.
            return strategy != ANY_OBJECT && strategy != ANY_NESTED;
        }
        return false;
    }

    /**
     * Determines if the provided class is an interface and qualifies as a DTO (Data Transfer Object).
     *
     * @param clazz The class to check.
     * @return True if the class is considered a DTO interface, false otherwise.
     */
    public static boolean dtoInterface(Class<?> clazz) {
        return clazz != null
                && clazz.isInterface()
                && clazz != Bytes.class
                && clazz != BytesStore.class
                && !Jvm.getPackageName(clazz).startsWith("java");
    }

    /**
     * Fetches the type name for the provided object using a default class alias pool.
     *
     * @param value The object whose type name is to be fetched.
     * @return The type name of the object.
     */
    public static String typeNameFor(@NotNull Object value) {
        return typeNameFor(ClassAliasPool.CLASS_ALIASES, value);
    }

    /**
     * Fetches the type name for the provided object using the specified class lookup mechanism.
     *
     * @param classLookup The class lookup mechanism to use.
     * @param value       The object whose type name is to be fetched.
     * @return The type name of the object.
     */
    public static String typeNameFor(ClassLookup classLookup, @NotNull Object value) {
        // If the class lookup is the default one and value is Marshallable, return the class name. Otherwise, use the lookup.
        return classLookup == ClassAliasPool.CLASS_ALIASES
                && value instanceof Marshallable
                ? ((Marshallable) value).className()
                : classLookup.nameFor(value.getClass());
    }

    /**
     * Creates a new instance of a Marshallable using a provided constructor and type name.
     *
     * @param constructor The constructor to use for instantiation.
     * @param typeName    The type name for the new instance.
     * @return A new instance of Marshallable.
     */
    static Marshallable newInstance(Constructor constructor, String typeName) {
        try {
            // Attempt to create a new instance using the provided constructor and type name.
            return (Marshallable) constructor.newInstance(new TupleInvocationHandler(typeName));
        } catch (Exception e) {
            // If any exception arises, wrap and throw it as an IllegalStateException.
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns a tuple for the specified class and type name.
     *
     * @param <T>       The type parameter.
     * @param tClass    The class type for which the tuple is required.
     * @param typeName  The type name.
     * @return A tuple of type T or null if not possible to create.
     */
    @Nullable
    public static <T> T tupleFor(Class<T> tClass, String typeName) {
        // Check if tuple generation is enabled.
        if (!GENERATE_TUPLES) {
            Jvm.warn().on(Wires.class, "Cannot find a class for " + typeName + " are you missing an alias?");
            return null;
        }

        // Set default class if not provided or provided as Object.
        if (tClass == null || tClass == Object.class)
            tClass = (Class<T>) Marshallable.class;
        // Ensure the provided class is an interface.
        if (!tClass.isInterface()) {
            Jvm.warn().on(Wires.class, "Cannot generate a class for " + typeName + " are you missing an alias?");
            return null;
        }

        // Use the appropriate function to generate the tuple.
        return (T) MARSHALLABLE_FUNCTION.get(tClass).apply(typeName);
    }

    /**
     * Determines if the value's package name is internal to Java.
     *
     * @param value The object to check.
     * @return True if the object's package name is internal, false otherwise.
     */
    public static boolean isInternal(@NotNull Object value) {
        String name = Jvm.getPackageName(value.getClass());
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("jdk.");
    }

    /**
     * Creates a BinaryWire for reading with the given input, position, and length.
     *
     * @param in        The input bytes.
     * @param position  The starting position.
     * @param length    The length of data to read.
     * @return A BinaryWire configured for reading.
     */
    @NotNull
    public static BinaryWire binaryWireForRead(Bytes<?> in, long position, long length) {
        BinaryWire wire = WIRE_TL.get();
        VanillaBytes bytes = (VanillaBytes) wire.bytes();
        wire.clear();
        bytes.bytesStore(in.bytesStore(), position, length);
        return wire;
    }

    /**
     * Creates a BinaryWire for writing with the given input, position, and length.
     *
     * @param in        The input bytes.
     * @param position  The starting position.
     * @param length    The length of data to write.
     * @return A BinaryWire configured for writing.
     */
    @NotNull
    public static BinaryWire binaryWireForWrite(Bytes<?> in, long position, long length) {
        BinaryWire wire = WIRE_TL.get();
        VanillaBytes bytes = (VanillaBytes) wire.bytes();
        bytes.bytesStore(in.bytesStore(), 0, position);
        bytes.writeLimit(position + length);
        return wire;
    }

    /**
     * Loads a Java class from the given source code string using the specified class loader.
     * If a cached compiler is not initialized, it initializes one either with default settings
     * or specific directories if they exist and `DUMP_CODE_TO_TARGET` is true.
     *
     * @param classLoader The class loader to use for loading the class.
     * @param className   The name of the class to be loaded.
     * @param code        The Java source code for the class.
     * @return The loaded class.
     * @throws ClassNotFoundException If the class could not be found or loaded.
     */
    static synchronized Class<?> loadFromJava(ClassLoader classLoader, String className, String code) throws ClassNotFoundException {
        if (CACHED_COMPILER == null) {
            final String target = OS.getTarget();
            File sourceDir = null;
            File classDir = null;

            if (new File(target).exists() && DUMP_CODE_TO_TARGET) {
                sourceDir = new File(target, "generated-test-sources");
                classDir = new File(target, "test-classes");
            }

            String compilerOptions = Jvm.getProperty("compiler.options");

            if (compilerOptions == null || compilerOptions.trim().isEmpty()) {
                CACHED_COMPILER = new CachedCompiler(sourceDir, classDir);
            } else {
                CACHED_COMPILER = new CachedCompiler(sourceDir, classDir, asList(compilerOptions.split("\\s")));
            }
        }
        try {
            // Use the CachedCompiler to load the class from the provided Java source code.
            return CACHED_COMPILER.loadFromJava(classLoader, className, code);
        } catch (Throwable t) {
            // On any error, close the CachedCompiler quietly and reset it.
            Closeable.closeQuietly(CACHED_COMPILER);
            CACHED_COMPILER = null;
            throw t;
        }
    }

    /**
     * Enum to provide serialization strategy based on the class type (Enum or DynamicEnum).
     */
    enum SerializeEnum implements Function<Class<?>, SerializationStrategy> {
        INSTANCE;

        /**
         * Determines the serialization strategy for a given class.
         *
         * @param aClass The class for which to determine the serialization strategy.
         * @return The serialization strategy or null if none matches.
         */
        @SuppressWarnings("deprecation")
        @Nullable
        static SerializationStrategy getSerializationStrategy(@NotNull Class<?> aClass) {
            if (DynamicEnum.class.isAssignableFrom(aClass))
                return DYNAMIC_ENUM;
            if (Enum.class.isAssignableFrom(aClass))
                return ENUM;
            return null;
        }

        /**
         * Applies the function to get the serialization strategy for the given class.
         *
         * @param aClass The class for which to apply the function.
         * @return The serialization strategy or null if none matches.
         */
        @Nullable
        @Override
        public SerializationStrategy apply(@NotNull Class<?> aClass) {
            return getSerializationStrategy(aClass);
        }
    }

    /**
     * Enum providing serialization strategy specifically for Java Language related classes.
     */
    enum SerializeJavaLang implements Function<Class<?>, SerializationStrategy> {
        INSTANCE;

        // Constants for date formatting.
        private static final String SDF_4_STRING = "yyyy-MM-dd";

        // SimpleDateFormat instances to format and parse dates in various formats.
        private static final SimpleDateFormat SDF = new SimpleDateFormat("EEE MMM d HH:mm:ss.S zzz yyyy");
        private static final SimpleDateFormat SDF_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS zzz");
        private static final SimpleDateFormat SDF_3 = new SimpleDateFormat("EEE MMM d HH:mm:ss.S zzz yyyy", Locale.US);
        private static final SimpleDateFormat SDF_4 = new SimpleDateFormat(SDF_4_STRING);

        // Static block to set the TimeZone to GMT for all date formats.
        static {
            SDF.setTimeZone(TimeZone.getTimeZone("GMT"));
            SDF_2.setTimeZone(TimeZone.getTimeZone("GMT"));
            SDF_3.setTimeZone(TimeZone.getTimeZone("GMT"));
            SDF_4.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        /**
         * Serializes a given Date object to a string format using the defined SimpleDateFormat and
         * writes it to the provided ValueOut object.
         *
         * @param date The Date object to be serialized.
         * @param out  The ValueOut object where the serialized date string will be written.
         * @return A WireOut object, which represents the state after the write operation.
         */
        public static WireOut writeDate(Date date, ValueOut out) {
            final String format = SDF_2.format(date);
            return out.writeString(format);
        }

                /**
         * Attempts to parse a date string from a ValueIn object and return a corresponding Date object.
         *
         * @param in The ValueIn object containing the date string.
         * @return A Date object representing the parsed date.
         * @throws IORuntimeException if the provided string cannot be parsed into a valid date.
         */
        public static Date parseDate(ValueIn in) {
            final String text = in.text().trim();

            // Check if the input string is empty.
            if (text.length() < 1) {
                throw new IORuntimeException("At least one character (e.g. '0') must be present in order to deserialize a Date object");
            }
            final char firstChar = text.charAt(0);

            // Check if the entire string is a number.
            if (firstChar == '+' || firstChar == '-' || Character.isDigit(firstChar)) {
                boolean isAllNum = true;
                for (int i = 1; i < text.length(); i++) {
                    if (!Character.isDigit(text.charAt(i))) {
                        isAllNum = false;
                        break;
                    }
                }
                if (isAllNum) {
                    try {
                        // If it's a number, interpret it as milliseconds from the epoch and return the corresponding Date.
                        return new Date(Long.parseLong(text));
                    } catch (NumberFormatException nfe) {
                        throw new IORuntimeException(nfe);
                    }
                }
            }

            // Attempt to interpret the string as a "yyyy-mm-dd" formatted date.
            if (text.length() == SDF_4_STRING.length()) {
                try {
                    synchronized (SDF_4) {
                        // Since we ruled out it is a number and SDF_$ is the only one with this length it must be SDF_4
                        return SDF_4.parse(text);
                    }
                } catch (ParseException pe) {
                    throw new IORuntimeException(pe);
                }
            }

            // Try the other remaining formats
            // Todo: optimize away exception chaining
            try {
                synchronized (SDF_2) {
                    return SDF_2.parse(text);
                }
            } catch (ParseException ignored) {
                // This exception is ignored to allow for further parsing attempts with other formats.
            }

            synchronized (SDF) {
                try {
                    return SDF.parse(text);
                } catch (ParseException ignored) {
                    // This exception is ignored to allow for further parsing attempts with other formats.
                }
            }

            // Final attempt with another format.
            try {
                synchronized (SDF_3) {
                    return SDF_3.parse(text);
                }
            } catch (ParseException pe3) {
                // If this final parsing attempt fails, throw an exception.
                throw new IORuntimeException("unable to parse: " + text, pe3);
            }
        }

        /**
         * Fetches the Class object associated with the name retrieved from the given ValueIn.
         *
         * @param o The object for which the class needs to be determined. (This parameter is unused in the method.)
         * @param in The ValueIn object which contains the class name.
         * @return The Class object associated with the name.
         */
        private static Class<?> forName(Class<?> o, ValueIn in) {
            final StringBuilder sb0 = sb.get();

            // Reset the StringBuilder to its initial state.
            sb0.setLength(0);

            // Read the text (class name in this context) into the StringBuilder.
            in.text(sb0);

            // Retrieve the Class object for the provided name using the classLookup of the ValueIn.
            return in.classLookup().forName(sb0);
        }

        @Override
        public SerializationStrategy apply(@NotNull Class<?> aClass) {
            switch (aClass.getName()) {
                case "[B":
                    return ScalarStrategy.of(byte[].class, (o, in) -> in.bytes());

                case "java.lang.StringBuilder":
                    return ScalarStrategy.of(StringBuilder.class, (o, in) -> {
                        StringBuilder builder;
                        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                             builder = (o == null)
                                    ? stlSb.get()
                                    : o;
                            in.textTo(builder);
                        }
                        return builder;
                    });

                case "java.lang.String":
                    return ScalarStrategy.of(String.class, (o, in) -> in.text());

                case "java.lang.Object":
                    return ANY_OBJECT;

                case "java.lang.Class":
                    return ScalarStrategy.of(Class.class, SerializeJavaLang::forName);

                case "java.lang.Boolean":
                    return ScalarStrategy.of(Boolean.class, (o, in) -> in.bool());

                case "java.lang.Byte":
                    return ScalarStrategy.of(Byte.class, (o, in) -> in.int8());

                case "java.lang.Short":
                    return ScalarStrategy.of(Short.class, (o, in) -> in.int16());

                case "java.lang.Character":
                    return ScalarStrategy.of(Character.class, (o, in) -> {
                        @Nullable final String text = in.text();
                        if (text == null || text.length() == 0)
                            return null;
                        return text.charAt(0);
                    });

                case "java.lang.Integer":
                    return ScalarStrategy.of(Integer.class, (o, in) -> in.int32());

                case "java.lang.Float":
                    return ScalarStrategy.of(Float.class, (o, in) -> in.float32());

                case "java.lang.Long":
                    return ScalarStrategy.of(Long.class, (o, in) -> in.int64());

                case "java.lang.Double":
                    return ScalarStrategy.of(Double.class, (o, in) -> in.float64());

                case "java.time.LocalTime":
                    return ScalarStrategy.of(LocalTime.class, (o, in) -> in.time());

                case "java.time.LocalDate":
                    return ScalarStrategy.of(LocalDate.class, (o, in) -> in.date());

                case "java.time.LocalDateTime":
                    return ScalarStrategy.of(LocalDateTime.class, (o, in) -> in.dateTime());

                case "java.time.ZonedDateTime":
                    return ScalarStrategy.of(ZonedDateTime.class, (o, in) -> in.zonedDateTime());

                case "java.sql.Time":
                    return ScalarStrategy.of(Time.class, (o, in) -> new Time(parseDate(in).getTime()));

                case "java.sql.Date":
                    return ScalarStrategy.of(java.sql.Date.class, (o, in) -> new java.sql.Date(parseDate(in).getTime()));

                case "javax.naming.CompositeName":
                    return ScalarStrategy.of(CompositeName.class, (o, in) -> {
                        try {
                            return new CompositeName(in.text());
                        } catch (InvalidNameException e) {
                            throw Jvm.rethrow(e);
                        }
                    });

                case "java.io.File":
                    return ScalarStrategy.text(File.class, File::new);

                case "java.util.UUID":
                    return ScalarStrategy.of(UUID.class, (o, in) -> in.uuid());

                case "java.math.BigInteger":
                    return ScalarStrategy.text(BigInteger.class, BigInteger::new);

                case "java.math.BigDecimal":
                    return ScalarStrategy.text(BigDecimal.class, BigDecimal::new);

                case "java.util.Date":
                    return ScalarStrategy.of(Date.class, (o, in) -> parseDate(in));

                case "java.time.Duration":
                    return ScalarStrategy.of(Duration.class, (o, in) -> Duration.parse(in.text()));

                case "java.time.Instant":
                    return ScalarStrategy.of(Instant.class, (o, in) -> Instant.parse(in.text()));

                case "java.sql.Timestamp":
                    return ScalarStrategy.of(Timestamp.class, (o, in) -> new Timestamp(parseDate(in).getTime()));

                case "java.util.GregorianCalendar":
                    return ScalarStrategy.of(GregorianCalendar.class, (o, in) -> GregorianCalendar.from(in.zonedDateTime()));

                case "java.util.Locale":
                    return ScalarStrategy.of(Locale.class, (o, in) -> Locale.forLanguageTag(in.text()));

                default:
                    if (aClass.isPrimitive())
                        return ANY_SCALAR;
                    if (aClass.isArray()) {
                        final Class<?> componentType = aClass.getComponentType();
                        if (componentType.isPrimitive())
                            return PRIM_ARRAY;
                        return ARRAY;
                    }
                    if (Enum.class.isAssignableFrom(aClass)) {
                        @Nullable final SerializationStrategy ss = SerializeMarshallables.getSerializationStrategy(aClass);
                        return ss == null ? ENUM : ss;
                    }
                    return null;
            }
        }
    }

    /**
     * This enum defines serialization strategies for various data types.
     * It provides a mechanism to determine the appropriate serialization strategy based on a class's type.
     * <p>
     * The primary function of this enum is to map a {@link Class} to its appropriate {@link SerializationStrategy}.
     * For example, classes that implement the {@link Demarshallable} interface will be associated with the DEMARSHALLABLE strategy.
     */
    enum SerializeMarshallables implements Function<Class<?>, SerializationStrategy> {
        INSTANCE;

        /**
         * Determines the appropriate serialization strategy for a given class.
         *
         * @param aClass The class to determine the serialization strategy for.
         * @return The serialization strategy, or null if there's no specific strategy for the class.
         */
        @Nullable
        static SerializationStrategy getSerializationStrategy(@NotNull Class<?> aClass) {
            if (Demarshallable.class.isAssignableFrom(aClass))
                return DEMARSHALLABLE;
            if (ReadMarshallable.class.isAssignableFrom(aClass)
                    || ReadBytesMarshallable.class.isAssignableFrom(aClass))
                return MARSHALLABLE;
            return null;
        }

        /**
         * Implementation of the {@link Function} interface.
         * Returns the appropriate {@link SerializationStrategy} for a given class.
         *
         * @param aClass The class to determine the serialization strategy for.
         * @return The corresponding serialization strategy for the given class.
         */
        @Override
        public SerializationStrategy apply(@NotNull Class<?> aClass) {
            @Nullable SerializationStrategy x = getSerializationStrategy(aClass);
            if (x != null) return x;
            if (Map.class.isAssignableFrom(aClass))
                return MAP;
            if (Set.class.isAssignableFrom(aClass))
                return SET;
            if (List.class.isAssignableFrom(aClass))
                return LIST;
            if (Externalizable.class.isAssignableFrom(aClass))
                return EXTERNALIZABLE;
            if (Serializable.class.isAssignableFrom(aClass))
                return ANY_NESTED;
            if (Comparable.class.isAssignableFrom(aClass))
                return ANY_SCALAR;
            if (aClass.isInterface())
                return null;
            return ANY_NESTED;
        }
    }

    /**
     * This enum provides serialization strategies for byte-based data types, specifically addressing
     * classes like BytesStore, Bytes, and Base64 encoded bytes. It helps in mapping specific classes
     * to the corresponding serialization strategies.
     */
    enum SerializeBytes implements Function<Class<?>, SerializationStrategy> {
        INSTANCE;

        /**
         * Decodes a Base64 encoded value into a {@link Bytes} object.
         *
         * @param o  An optional Bytes object where the decoded data will be written. If null, a new Bytes object is created.
         * @param in The input source containing the Base64 encoded value.
         * @return A Bytes object containing the decoded data.
         */
        static Bytes<?> decodeBase64(Bytes<?> o, ValueIn in) {
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                @NotNull StringBuilder sb0 = stlSb.get();
                in.text(sb0);
                String s = WireInternal.INTERNER.intern(sb0);
                byte[] decode = Base64.getDecoder().decode(s);
                if (o == null)
                    return Bytes.wrapForRead(decode);
                o.clear();
                o.write(decode);
                return o;
            }
        }

        /**
         * Returns the appropriate {@link SerializationStrategy} for the given class.
         *
         * @param aClass The class to determine the serialization strategy for.
         * @return The corresponding serialization strategy for the given class or null if not found.
         */
        @Override
        public SerializationStrategy apply(@NotNull Class<?> aClass) {
            switch (aClass.getName()) {
                case "net.openhft.chronicle.bytes.BytesStore":
                    return ScalarStrategy.of(BytesStore.class, (o, in) -> in.bytesStore());
                case "net.openhft.chronicle.bytes.Bytes":
                    return ScalarStrategy.of(Bytes.class,
                            (o, in) -> in.bytesStore().bytesForRead());
                case "java.util.Base64":
                    return ScalarStrategy.of(Bytes.class,
                            SerializeBytes::decodeBase64);
                default:
                    return null;
            }
        }
    }

    /**
     * This class encapsulates a pair of information about fields, represented as a list and a map.
     * It is designed to offer a consolidated way to store and access field-related data.
     * The static instance EMPTY represents an empty set of field information.
     */
    static class FieldInfoPair {
        static final FieldInfoPair EMPTY = new FieldInfoPair(Collections.emptyList(), Collections.emptyMap());

        @NotNull
        final List<FieldInfo> list;
        @NotNull
        final Map<String, FieldInfo> map;

        /**
         * Constructs a FieldInfoPair with the provided list and map of field information.
         *
         * @param list The list of field information.
         * @param map  The map containing field names as keys and their corresponding field information as values.
         */
        public FieldInfoPair(@NotNull List<FieldInfo> list, @NotNull Map<String, FieldInfo> map) {
            this.list = list;
            this.map = map;
        }
    }

    /**
     * This class provides a custom {@link InvocationHandler} implementation specifically tailored for handling
     * proxy invocations for tuple-like objects. The handler captures method invocations and applies custom
     * logic for specific methods like "hashCode", "equals", and "deepCopy".
     */
    static class TupleInvocationHandler implements InvocationHandler {

        // Name of the type represented by this invocation handler.
        final String typeName;

        // Map containing field names as keys and their corresponding values.
        final Map<String, Object> fields = new LinkedHashMap<>();

        /**
         * Constructs an instance of {@link TupleInvocationHandler} with the provided type name.
         *
         * @param typeName The name of the type represented by this invocation handler.
         */
        private TupleInvocationHandler(String typeName) {
            this.typeName = typeName;
        }

        /**
         * This method handles the invocations on proxy instances.
         * It provides custom logic for specific methods: "hashCode", "equals", and "deepCopy".
         *
         * @param proxy  The proxy instance on which the method was invoked.
         * @param method The method that was invoked.
         * @param args   The arguments passed to the invoked method.
         * @return The result of the method invocation.
         * @throws Exception If any error occurs during the invocation.
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
            String name = method.getName();
            switch (name) {
                case "hashCode":
                    if (args == null || args.length == 0) {
                        return (int) Maths.agitate(typeName.hashCode() * 1019L + fields.hashCode() * 10191L);
                    }
                    break;
                case "equals":
                    if (args != null && args.length == 1) {
                        return equals0(proxy, args[0]);
                    }
                    break;
                case "deepCopy":
                    if (args == null || args.length == 0) {
                        TupleInvocationHandler h2 = new TupleInvocationHandler(typeName);
                        h2.fields.putAll(fields);
                        return proxy.getClass().getDeclaredConstructor(InvocationHandler.class).newInstance(h2);
                    }
                    break;
                case "toString":
                    if (args == null || args.length == 0)
                        return TEXT.asString(proxy);
                    break;
                case "readMarshallable":
                    if (args.length == 1) {
                        WireIn in = (WireIn) args[0];
                        while (in.hasMore()) {
                            fields.put(in.readEvent(String.class), in.getValueIn().object());
                        }
                        return null;
                    }
                    break;
                case "writeMarshallable":
                    if (args.length == 1) {
                        WireOut out = (WireOut) args[0];
                        for (Map.Entry<String, Object> entry : fields.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            if (value != null) {
                                out.write(key).object(value);
                            }
                        }
                        return null;
                    }
                    break;
                case "getField":
                    if (args.length == 2) {
                        Object value = fields.get(args[0]);
                        return ObjectUtils.convertTo((Class) args[1], value);
                    }
                    break;
                case "setField":
                    if (args.length == 2) {
                        fields.put(args[0].toString(), args[1]);
                        return null;
                    }
                    break;
                case "className":
                    if (args == null || args.length == 0)
                        return typeName;
                    break;
                case "$fieldInfos":
                    if (args == null || args.length == 0) {
                        List<FieldInfo> fieldInfos = new ArrayList<>();
                        for (Map.Entry<String, Object> entry : fields.entrySet()) {
                            Class<?> valueClass = entry.getValue().getClass();
                            fieldInfos.add(new TupleFieldInfo(entry.getKey(), valueClass));
                        }
                        return fieldInfos;
                    }
                    break;
                case "usesSelfDescribingMessage":
                    return Boolean.TRUE;
            }
            if (args == null || args.length == 0) {
                Class<?> returnType = method.getReturnType();
                if (fields.containsKey(name))
                    return ObjectUtils.convertTo(returnType, fields.get(name));
                return ObjectUtils.defaultValue(returnType);
            }
            if (args.length == 1) {
                fields.put(name, args[0]);
                return proxy;
            }
            throw new UnsupportedOperationException("The class or alias " + typeName + " could not be found, so unable to call " + method);
        }

        /**
         * Compares the provided object with the proxy instance for equality.
         * The method checks for several conditions:
         * 1. If the objects are the same instance.
         * 2. If the other object is an instance of {@link Marshallable}.
         * 3. If the class names match.
         * 4. If the other object is a proxy.
         * 5. If the proxy's invocation handler is an instance of .
         * 6. If the fields map inside both  instances are equal.
         *
         * @param proxy The proxy instance being compared.
         * @param o     The object to be compared with the proxy for equality.
         * @return Boolean.TRUE if the specified object is equal to the proxy, otherwise Boolean.FALSE.
         */
        @NotNull
        private Object equals0(Object proxy, Object o) {
            if (proxy == o)
                return true;
            if (!(o instanceof Marshallable))
                return false;
            Marshallable m = (Marshallable) o;
            if (!m.className().equals(typeName))
                return false;
            if (!Proxy.isProxyClass(m.getClass()))
                return false;
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(m);
            if (!(invocationHandler instanceof TupleInvocationHandler))
                return false;
            TupleInvocationHandler tih = (TupleInvocationHandler) invocationHandler;
            return fields.equals(tih.fields);
        }
    }

    /**
     * Represents the field information specific to a tuple.
     * <p>
     * This class extends {@link AbstractFieldInfo} to provide additional functionalities
     * tailored to tuples. It mainly provides a way to deduce the {@link BracketType} of
     * a given serialization strategy and a mechanism to retrieve the fields of a tuple.
     */
    static class TupleFieldInfo extends AbstractFieldInfo {

        /**
         * @param name The name of the field.
         * @param type The type of the field.
         */
        public TupleFieldInfo(String name, Class<?> type) {
            super(type, bracketType(SerializeMarshallables.INSTANCE.apply(type)), name);
        }

        /**
         * Deduces the {@link BracketType} for a given serialization strategy.
         *
         * @param ss The serialization strategy.
         * @return The {@link BracketType} of the serialization strategy.
         */
        static BracketType bracketType(SerializationStrategy ss) {
            return ss == null ? BracketType.UNKNOWN : ss.bracketType();
        }

        /**
         * Retrieves the map of fields for a given object, assuming the object
         * is backed by a {@link TupleInvocationHandler}.
         *
         * @param o The object whose fields map needs to be fetched.
         * @return The map of fields corresponding to the object.
         */
        private Map<String, Object> getMap(Object o) {
            TupleInvocationHandler invocationHandler = (TupleInvocationHandler) Proxy.getInvocationHandler(o);
            return invocationHandler.fields;
        }

        @Nullable
        @Override
        public Object get(Object object) {
            return getMap(object).get(name);
        }

        @Override
        public long getLong(Object object) {
            return ObjectUtils.convertTo(Long.class, get(object));
        }

        @Override
        public int getInt(Object object) {
            return ObjectUtils.convertTo(Integer.class, get(object));
        }

        @Override
        public char getChar(Object object) {
            return ObjectUtils.convertTo(Character.class, get(object));
        }

        @Override
        public double getDouble(Object object) {
            return ObjectUtils.convertTo(Double.class, get(object));
        }

        @Override
        public void set(Object object, Object value) throws IllegalArgumentException {
            getMap(object).put(name, value);
        }

        @Override
        public void set(Object object, char value) throws IllegalArgumentException {
            set(name, (Object) value);
        }

        @Override
        public void set(Object object, int value) throws IllegalArgumentException {
            set(name, (Object) value);
        }

        @Override
        public void set(Object object, long value) throws IllegalArgumentException {
            set(name, (Object) value);
        }

        @Override
        public void set(Object object, double value) throws IllegalArgumentException {
            set(name, (Object) value);
        }

        @Override
        public Class<?> genericType(int index) {
            return Object.class;
        }

        @Override
        public boolean isEqual(Object a, Object b) {
            return Objects.deepEquals(get(a), get(b));
        }
    }
}
