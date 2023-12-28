/**
 * Reads a LocalTime from the wire and applies it to a given object using the provided BiConsumer.
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.Resettable;
import net.openhft.chronicle.core.io.ValidatableUtil;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.*;

/**
 * Represents an interface for reading values in various formats from a serialized data source.
 * This interface is part of the Chronicle Wire library, which is designed for high-performance
 * serialization and deserialization of data. It provides methods to read data types like text,
 * binary, numeric, and temporal values, as well as support for more complex types like collections
 * and marshallable objects.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface ValueIn {
    Consumer<ValueIn> DISCARD = v -> {
    };

    /*
     * Text / Strings.
     */
    @NotNull
    default <T> WireIn text(T t, @NotNull BiConsumer<T, String> ts) {
        @Nullable final String text = text();
        ts.accept(t, text);
        return wireIn();
    }

    @NotNull
    default WireIn text(@NotNull StringBuilder sb) {
        if (textTo(sb) == null)
            sb.setLength(0);
        return wireIn();
    }

    default char character() {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            @Nullable CharSequence cs = textTo(stlSb.get());
            if (cs == null || cs.length() == 0)
                return '\u0000';

            return cs.charAt(0);
        }
    }

    @NotNull
    default WireIn text(@NotNull Bytes<?> sdo) {
        sdo.clear();
        textTo(sdo);
        return wireIn();
    }

    @Nullable
    String text();

    @Nullable
    StringBuilder textTo(@NotNull StringBuilder sb);

    @Nullable
    Bytes<?> textTo(@NotNull Bytes<?> bytes);

    @NotNull
    WireIn bytes(@NotNull BytesOut<?> toBytes);

    default WireIn bytes(@NotNull BytesOut<?> toBytes, boolean clearBytes) {
        if (clearBytes)
            toBytes.clear();
        return bytes(toBytes);
    }

    @NotNull
    default WireIn bytesLiteral(@NotNull BytesOut<?> toBytes) {
        return bytes(toBytes);
    }

    @Nullable
    default BytesStore bytesLiteral() {
        return bytesStore();
    }

    @Nullable
    WireIn bytesSet(@NotNull PointerBytesStore toBytes);

    @NotNull
    WireIn bytesMatch(@NotNull BytesStore compareBytes, BooleanConsumer consumer);

    @NotNull
    WireIn bytes(@NotNull ReadBytesMarshallable bytesMarshallable);

    default byte @Nullable [] bytes() {
        return bytes((byte[]) null);
    }

    byte @Nullable [] bytes(byte[] using);

    @Nullable
    default BytesStore bytesStore() {
        byte @Nullable [] bytes = bytes();
        return bytes == null ? null : BytesStore.wrap(bytes);
    }

    default void byteBuffer(@NotNull ByteBuffer bb) {
        bb.put(bytes());
    }

    @NotNull
    WireIn wireIn();

    /**
     * the length of the field as bytes including any encoding and header character
     */
    long readLength();

    @NotNull
    WireIn skipValue();

    @NotNull <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> tFlag);

    /**
     * Reads an 8-bit integer (byte) value and applies an ObjByteConsumer with the provided object and the read value.
     *
     * @param <T> The type of object to be passed to the ObjByteConsumer.
     * @param t   The object to be passed to the ObjByteConsumer.
     * @param tb  The ObjByteConsumer that accepts the object and the read 8-bit integer value.
     * @return The WireIn instance for method chaining.
     */
    @NotNull <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb);

    /**
     * Reads an unsigned 8-bit integer (short) value and applies an ObjShortConsumer with the provided object and the read value.
     *
     * @param <T> The type of object to be passed to the ObjShortConsumer.
     * @param t   The object to be passed to the ObjShortConsumer.
     * @param ti  The ObjShortConsumer that accepts the object and the read unsigned 8-bit integer value.
     * @return The WireIn instance for method chaining.
     */
    @NotNull <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti);

    /**
     * Reads a 16-bit integer (short) value and applies an ObjShortConsumer with the provided object and the read value.
     *
     * @param <T> The type of object to be passed to the ObjShortConsumer.
     * @param t   The object to be passed to the ObjShortConsumer.
     * @param ti  The ObjShortConsumer that accepts the object and the read 16-bit integer value.
     * @return The WireIn instance for method chaining.
     */
    @NotNull <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti);

    /**
     * Reads an unsigned 16-bit integer (int) value and applies an ObjIntConsumer with the provided object and the read value.
     *
     * @param <T> The type of object to be passed to the ObjIntConsumer.
     * @param t   The object to be passed to the ObjIntConsumer.
     * @param ti  The ObjIntConsumer that accepts the object and the read unsigned 16-bit integer value.
     * @return The WireIn instance for method chaining.
     */
    @NotNull <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti);

    /**
     * Reads a 32-bit integer (int) value and applies an ObjIntConsumer with the provided object and the read value.
     *
     * @param <T> The type of object to be passed to the ObjIntConsumer.
     * @param t   The object to be passed to the ObjIntConsumer.
     * @param ti  The ObjIntConsumer that accepts the object and the read 32-bit integer value.
     * @return The WireIn instance for method chaining.
     */
    @NotNull <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti);

    /**
     * Reads an unsigned 32-bit integer (long) value and applies an ObjLongConsumer with the provided object and the read value.
     *
     * @param <T> The type of object to be passed to the ObjLongConsumer.
     * @param t   The object to be passed to the ObjLongConsumer.
     * @param tl  The ObjLongConsumer that accepts the object and the read unsigned 32-bit integer value.
     * @return The WireIn instance for method chaining.
     */
    @NotNull <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl);

    /**
     * Reads a 64-bit integer (long) value and applies an ObjLongConsumer with the provided object and the read value.
     *
     * @param <T> The type of object to be passed to the ObjLongConsumer.
     * @param t   The object to be passed to the ObjLongConsumer.
     * @param tl  The ObjLongConsumer that accepts the object and the read 64-bit integer value.
     * @return The WireIn instance for method chaining.
     */
    @NotNull <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl);

    /**
     * Reads a 32-bit floating-point (float) value and applies an ObjFloatConsumer with the provided object and the read value.
     *
     * @param <T> The type of object to be passed to the ObjFloatConsumer.
     * @param t   The object to be passed to the ObjFloatConsumer.
     * @param tf  The ObjFloatConsumer that accepts the object and the read 32-bit floating-point value.
     * @return The WireIn instance for method chaining.
     */
    @NotNull <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf);

    /**
     * Reads a 64-bit floating-point (double) value and applies an ObjDoubleConsumer with the provided object and the read value.
     *
     * @param <T> The type of object to be passed to the ObjDoubleConsumer.
     * @param t   The object to be passed to the ObjDoubleConsumer.
     * @param td  The ObjDoubleConsumer that accepts the object and the read 64-bit floating-point value.
     * @return The WireIn instance for method chaining.
     */
    @NotNull <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td);

    @NotNull <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime);

    /**
     * Reads a ZonedDateTime from the wire and applies it to a given object using the provided BiConsumer.
     *
     * @param <T> The type of object to be passed to the BiConsumer.
     * @param t   The object to be passed to the BiConsumer.
     * @param tZonedDateTime The BiConsumer that accepts the object and the read ZonedDateTime.
     * @return The WireIn instance for method chaining.
     */
    @NotNull <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime);

    /**
     * Reads a LocalDate from the wire and applies it to a given object using the provided BiConsumer.
     *
     * @param <T> The type of object to be passed to the BiConsumer.
     * @param t   The object to be passed to the BiConsumer.
     * @param tLocalDate The BiConsumer that accepts the object and the read LocalDate.
     * @return The WireIn instance for method chaining.
     */
    @NotNull <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate);

    /**
     * Reads a LocalDate directly from the wire.
     *
     * @return The LocalDate read from the wire.
     */
    default LocalDate date() {
        return WireInternal.intern(LocalDate.class, text());
    }

    /**
     * Reads a LocalTime directly from the wire.
     *
     * @return The LocalTime read from the wire.
     */
    default LocalTime time() {
        return WireInternal.intern(LocalTime.class, text());
    }

    /**
     * Reads a LocalDateTime directly from the wire.
     *
     * @return The LocalDateTime read from the wire.
     */
    default LocalDateTime dateTime() {
        return WireInternal.intern(LocalDateTime.class, text());
    }

    /**
     * Reads a ZonedDateTime directly from the wire.
     *
     * @return The ZonedDateTime read from the wire.
     */
    default ZonedDateTime zonedDateTime() {
        return WireInternal.intern(ZonedDateTime.class, text());
    }

    /**
     * Checks if there is another element to read in a sequence or collection.
     *
     * @return True if there is another element to read, false otherwise.
     */
    boolean hasNext();

    /**
     * Checks if there is another item in a sequence.
     *
     * @return True if there is another sequence item, false otherwise.
     */
    boolean hasNextSequenceItem();

    /**
     * Reads a UUID from the wire and applies it to a given object using the provided BiConsumer.
     *
     * @param <T> The type of object to be passed to the BiConsumer.
     * @param t   The object to be passed to the BiConsumer.
     * @param tuuid The BiConsumer that accepts the object and the read UUID.
     * @return The WireIn instance for method chaining.
     */
    @NotNull <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid);

    /**
     * Reads a LongArrayValues from the wire and applies it to a given object using the provided BiConsumer.
     *
     * @param <T> The type of object to be passed to the BiConsumer.
     * @param values The LongArrayValues to read the data into.
     * @param t   The object to be passed to the BiConsumer.
     * @param setter The BiConsumer that accepts the object and the LongArrayValues.
     * @return The WireIn instance for method chaining.
     */
    @NotNull <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter);

    /**
     * Reads a 128-bit integer value from the wire into the specified TwoLongValue.
     *
     * @param value The TwoLongValue to read the 128-bit integer into.
     * @return The WireIn instance for method chaining.
     */
    @NotNull
    default WireIn int128(@NotNull TwoLongValue value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Reads a 64-bit integer value from the wire into the specified LongValue.
     *
     * @param value The LongValue to read the 64-bit integer into.
     * @return The WireIn instance for method chaining.
     */
    @NotNull
    WireIn int64(@NotNull LongValue value);

    /**
     * Reads a 32-bit integer value from the wire into the specified IntValue.
     *
     * @param value The IntValue to read the 32-bit integer into.
     * @return The WireIn instance for method chaining.
     */
    @NotNull
    WireIn int32(@NotNull IntValue value);

    @NotNull
    default LongValue int64ForBinding(@Nullable LongValue value) {
        @NotNull LongValue ret = value == null ? wireIn().newLongReference() : value;
        int64(ret);
        return ret;
    }

    WireIn bool(@NotNull BooleanValue ret);

    @NotNull <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter);

    @NotNull <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter);

    <T> boolean sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader);

/**
 * Processes a sequence of values from the wire, storing them in a list. It uses a buffer to minimize object creation.
 *
 * @param list The list to store the processed items.
 * @param buffer The buffer for reusing objects.
 * @param bufferAdd Supplier to provide new instances for the buffer.
 * @param reader0 The reader that processes each item in the sequence.
 * @return True if the sequence was processed, false otherwise.
 */
    <T> boolean sequence(List<T> list, @NotNull List<T> buffer, Supplier<T> bufferAdd, Reader reader0) throws InvalidMarshallableException;

/**
 * Processes a sequence of values from the wire using a SerializationStrategy.
 *
 * @param <T> The type of the object to be processed.
 * @param t The object to be processed.
 * @param tReader The SerializationStrategy to process the object.
 * @return True if the sequence was processed, false otherwise.
 */
    default <T> boolean sequence(@NotNull T t, @NotNull SerializationStrategy<T> tReader) throws InvalidMarshallableException {
        return sequence(t, (using, in) -> tReader.readUsing(null, using, in, BracketType.UNKNOWN));
    }

/**
 * Helper method to read a sequence of values into a list using a buffer.
 *
 * @param v The ValueIn to read from.
 * @param list The list to store the read values.
 * @param buffer The buffer for reusing objects.
 * @param bufferAdd Supplier to provide new instances for the buffer.
 */
    default <T> void reader0(ValueIn v, List<T> list, List<T> buffer, Supplier<T> bufferAdd) throws InvalidMarshallableException {
        while (v.hasNextSequenceItem()) {
            int size = list.size();
            if (buffer.size() <= size) {
                buffer.add(bufferAdd.get());
            }

            final Object t = buffer.get(size);
            if (t instanceof Resettable) ((Resettable) t).reset();
            list.add((T) v.object(t, t.getClass()));
        }
    }

    /**
     * sequence to use when using a cached buffer
     *
     * @param list      of items to populate
     * @param buffer    of objects of the same type to reuse
     * @param bufferAdd supplier to call when the buffer needs extending
     * @return true if there is any data.
     */
    default <T> boolean sequence(@NotNull List<T> list,
                                 @NotNull List<T> buffer,
                                 @NotNull Supplier<T> bufferAdd) throws InvalidMarshallableException {
        list.clear();
        return sequence(list, buffer, bufferAdd, this::reader0);
    }

/**
 * Processes a sequence of values from the wire, applying a TriConsumer to each item in the sequence.
 *
 * @param <T> The type of the first object to be passed to the TriConsumer.
 * @param <K> The type of the second object to be passed to the TriConsumer.
 * @param t The first object to be passed to the TriConsumer.
 * @param k The second object to be passed to the TriConsumer.
 * @param tReader The TriConsumer that processes each item in the sequence.
 * @return The WireIn instance for method chaining.
 */
    @NotNull <T, K> WireIn sequence(@NotNull T t, K k, @NotNull TriConsumer<T, K, ValueIn> tReader) throws InvalidMarshallableException;

    default <T> int sequenceWithLength(@NotNull T t, @NotNull ToIntBiFunction<ValueIn, T> tReader) {
        int[] length = {0};
        sequence(t, (tt, in) -> length[0] = tReader.applyAsInt(in, tt));
        return length[0];
    }

/**
 * Reads an array of Bytes objects from the wire, populating the provided array.
 *
 * @param array The array of Bytes objects to be populated.
 * @return The number of Bytes objects read and populated.
 */
    default int array(Bytes[] array) {
        return sequenceWithLength(array, (in, a) -> {
            int i = 0;
            while (in.hasNextSequenceItem() && i < a.length) {
                if (a[i] == null)
                    a[i] = Bytes.allocateElasticOnHeap(32);
                bytes(a[i++]);
            }
            return i;
        });
    }

/**
 * Reads an array of double values from the wire, populating the provided array.
 *
 * @param array The array of double values to be populated.
 * @return The number of double values read and populated.
 */
    default int array(double[] array) {
        return sequenceWithLength(array, (in, a) -> {
            int i = 0;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.float64();
            return i;
        });
    }

/**
 * Reads an array of double values with delta compression from the wire, populating the provided array.
 *
 * @param array The array of double values to be populated.
 * @return The number of double values read and populated.
 */
    default int arrayDelta(double[] array) {
        return sequenceWithLength(array, (in, a) -> {
            if (!in.hasNextSequenceItem() || a.length == 0)
                return 0;
            double a0 = a[0] = in.float64();
            int i = 1;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.float64() + a0;
            return i;
        });
    }

/**
 * Reads an array of boolean values from the wire, populating the provided array.
 *
 * @param array The array of boolean values to be populated.
 * @return The number of boolean values read and populated.
 */
    default int array(boolean[] array) {
        return sequenceWithLength(array, (in, a) -> {
            int i = 0;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.bool();
            return i;
        });
    }

/**
 * Reads an array of long values from the wire, populating the provided array.
 *
 * @param array The array of long values to be populated.
 * @return The number of long values read and populated.
 */
    default int array(long[] array) {
        return sequenceWithLength(array, (in, a) -> {
            int i = 0;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.int64();
            return i;
        });
    }

/**
 * Reads an array of long values with delta compression from the wire, populating the provided array.
 *
 * @param array The array of long values to be populated.
 * @return The number of long values read and populated.
 */
    default int arrayDelta(long[] array) {
        return sequenceWithLength(array, (in, a) -> {
            if (!in.hasNextSequenceItem() || a.length == 0)
                return 0;
            long a0 = a[0] = in.int64();
            int i = 1;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.int64() + a0;
            return i;
        });
    }

/**
 * Reads an array of int values from the wire, populating the provided array.
 *
 * @param array The array of int values to be populated.
 * @return The number of int values read and populated.
 */
    default int array(int[] array) {
        return sequenceWithLength(array, (in, a) -> {
            int i = 0;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.int32();
            return i;
        });
    }

/**
 * Reads an array of byte values from the wire, populating the provided array.
 *
 * @param array The array of byte values to be populated.
 * @return The number of byte values read and populated.
 */
    default int array(byte[] array) {
        return sequenceWithLength(array, (in, a) -> {
            int i = 0;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.int8();
            return i;
        });
    }

/**
 * Reads a Set of objects of a specified type from the wire.
 *
 * @param <T> The type of objects in the Set.
 * @param t The Class object representing the type T.
 * @return A Set containing objects of type T, or throws InvalidMarshallableException in case of an error.
 */
    default <T> Set<T> set(Class<T> t) throws InvalidMarshallableException {
        return collection(LinkedHashSet::new, t);
    }

/**
 * Reads a List of objects of a specified type from the wire.
 *
 * @param <T> The type of objects in the List.
 * @param t The Class object representing the type T.
 * @return A List containing objects of type T, or throws InvalidMarshallableException in case of an error.
 */
    default <T> List<T> list(Class<T> t) throws InvalidMarshallableException {
        return collection(ArrayList::new, t);
    }

/**
 * Reads a Collection of objects of a specified type from the wire, using the provided supplier for the Collection type.
 *
 * @param <T> The type of objects in the Collection.
 * @param <C> The type of the Collection.
 * @param supplier A Supplier providing instances of the Collection type C.
 * @param t The Class object representing the type T.
 * @return A Collection containing objects of type T, or throws InvalidMarshallableException in case of an error.
 */
    default <T, C extends Collection<T>> C collection(@NotNull Supplier<C> supplier, Class<T> t) throws InvalidMarshallableException {
        C list = supplier.get();
        sequence(list, t, (s, kls, v) -> {
            while (v.hasNextSequenceItem())
                s.add(v.object(kls));
        });
        return list;
    }

/**
 * Reads a Set of ReadMarshallable objects from the wire, and applies them to a provided object using the function.
 *
 * @param <O> The type of the object to apply the Set to.
 * @param <T> The type of the ReadMarshallable objects.
 * @param o The object to apply the Set to.
 * @param tSupplier A Function providing instances of type T.
 * @return The WireIn instance for method chaining.
 */
    @NotNull
    default <O, T extends ReadMarshallable> WireIn set(@NotNull O o, Function<O, T> tSupplier) throws InvalidMarshallableException {
        return collection(o, tSupplier);
    }

/**
 * Reads a List of ReadMarshallable objects from the wire, and applies them to a provided object using the function.
 *
 * @param <O> The type of the object to apply the List to.
 * @param <T> The type of the ReadMarshallable objects.
 * @param o The object to apply the List to.
 * @param tSupplier A Function providing instances of type T.
 * @return The WireIn instance for method chaining.
 */
    @NotNull
    default <O, T extends ReadMarshallable> WireIn list(@NotNull O o, Function<O, T> tSupplier) throws InvalidMarshallableException {
        return collection(o, tSupplier);
    }

    @NotNull
    default <O, T extends ReadMarshallable> WireIn collection(@NotNull O o, Function<O, T> tSupplier) throws InvalidMarshallableException {
        sequence(o, tSupplier, (o2, ts, v) -> {
            while (v.hasNextSequenceItem()) {
                T t = ts.apply(o2);
                v.marshallable(t);
            }
        });
        return wireIn();
    }

/**
 * Reads a Map of marshallable key-value pairs as specified by the provided classes.
 *
 * @param <K> The type of keys in the Map.
 * @param <V> The type of values in the Map.
 * @param kClass The class of the keys.
 * @param vClass The class of the values.
 * @return A Map containing the key-value pairs, or null if the Map could not be read.
 * @throws InvalidMarshallableException if the key or value types are invalid
 */
    @Nullable
    default <K, V> Map<K, V> marshallableAsMap(Class<K> kClass, @NotNull Class<V> vClass) throws InvalidMarshallableException {
        return marshallableAsMap(kClass, vClass, new LinkedHashMap<>());
    }

/**
 * Reads a Map of marshallable key-value pairs into the provided Map.
 *
 * @param <K> The type of keys in the Map.
 * @param <V> The type of values in the Map.
 * @param kClass The class of the keys.
 * @param vClass The class of the values.
 * @param map The Map to populate with the read key-value pairs.
 * @return The provided Map populated with key-value pairs, or null if the Map could not be read.
 */
    @Nullable
    default <K, V> Map<K, V> marshallableAsMap(Class<K> kClass, @NotNull Class<V> vClass, @NotNull Map<K, V> map) {
        return marshallable(m -> m.readAllAsMap(kClass, vClass, map)) ? map : null;
    }

/**
 * Applies a Function to this ValueIn, interpreting it as a WireIn for a marshallable object.
 *
 * @param <T> The type of the result from the Function.
 * @param marshallableReader The Function to apply to this ValueIn.
 * @return The result of applying the Function to this ValueIn, or null if it cannot be applied.
 */
    @Nullable <T> T applyToMarshallable(Function<WireIn, T> marshallableReader);

/**
 * Reads a typed marshallable object from the wire.
 *
 * @return The marshallable object read from the wire, or null if it cannot be read.
 * @throws IORuntimeException if an I/O error occurs
 * @throws InvalidMarshallableException if the marshallable object is invalid
 */
    @Nullable <T> T typedMarshallable() throws IORuntimeException, InvalidMarshallableException;

/**
 * Reads a typed marshallable object from the wire, using the provided function to create instances.
 *
 * @param marshallableFunction The function to create marshallable object instances.
 * @return The marshallable object read from the wire, or null if it cannot be read.
 * @throws IORuntimeException if an I/O error occurs
 * @throws InvalidMarshallableException if the marshallable object is invalid
 */
    @Nullable
    default <T> T typedMarshallable(@NotNull Function<Class, ReadMarshallable> marshallableFunction)
            throws IORuntimeException, InvalidMarshallableException {
        @Nullable final Class aClass = typePrefix();

        if (ReadMarshallable.class.isAssignableFrom(aClass)) {
            final ReadMarshallable marshallable = marshallableFunction.apply(aClass);
            marshallable(marshallable);
            return (T) marshallable;
        }
        return object(null, aClass);
    }

/**
 * Reads a type prefix and applies it to a given object using the provided BiConsumer.
 *
 * @param <T> The type of the object to apply the type prefix to.
 * @param t The object to apply the type prefix to.
 * @param ts The BiConsumer that accepts the object and the type prefix.
 * @return The WireIn instance for method chaining.
 */
    @NotNull <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts);

    @NotNull <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer)
            throws IORuntimeException, BufferUnderflowException;

    ClassLookup classLookup();


    /**
 * Reads a type literal as text and applies it to a given object using the provided BiConsumer.
 *
 * @param <T> The type of the object to apply the type literal to.
 * @param t The object to apply the type literal to.
 * @param classNameConsumer The BiConsumer that accepts the object and the type literal.
 * @return The WireIn instance for method chaining.
 * @throws IORuntimeException if an I/O error occurs
 * @throws BufferUnderflowException if the buffer underflows while reading
 */
    @NotNull
    default <T> WireIn typeLiteral(T t, @NotNull BiConsumer<T, Class> classConsumer) throws IORuntimeException {
        return typeLiteralAsText(t, (o, x) ->
                classConsumer.accept(o, classLookup().forName(x))
        );
    }

/**
 * Reads a type literal from the wire, applies it to a given object using the provided BiConsumer, and uses a default Class if necessary.
 *
 * @param <T> The type of the object to apply the type literal to.
 * @param t The object to apply the type literal to.
 * @param classConsumer The BiConsumer that accepts the object and the Class derived from the type literal.
 * @param defaultClass The default Class to use if the type literal cannot be resolved.
 * @return The WireIn instance for method chaining.
 */
    @NotNull
    default <T> WireIn typeLiteral(T t, @NotNull BiConsumer<T, Class> classConsumer, Class defaultClass) {
        return typeLiteralAsText(t, (o, x) -> {
            Class u = classLookup().forName(x);
            classConsumer.accept(o, u);
        });
    }

/**
 * Reads a marshallable object from the wire and applies a specified SerializationStrategy.
 *
 * @param object The object to populate with the marshallable data.
 * @param strategy The SerializationStrategy to use for reading the marshallable data.
 * @return The object populated with marshallable data, or null if it cannot be read.
 * @throws BufferUnderflowException if the buffer underflows while reading
 * @throws IORuntimeException if an I/O error occurs
 * @throws InvalidMarshallableException if the marshallable object is invalid
 */
    @Nullable
    Object marshallable(@NotNull Object object, @NotNull SerializationStrategy strategy)
            throws BufferUnderflowException, IORuntimeException, InvalidMarshallableException;

/**
 * Reads a Serializable object from the wire.
 *
 * @param object The Serializable object to populate with the data.
 * @return True if the object was successfully read, false otherwise.
 * @throws BufferUnderflowException if the buffer underflows while reading
 * @throws IORuntimeException if an I/O error occurs
 * @throws InvalidMarshallableException if the Serializable object is invalid
 */
    default boolean marshallable(@NotNull Serializable object) throws BufferUnderflowException, IORuntimeException, InvalidMarshallableException {
        return marshallable(object, SerializationStrategies.SERIALIZABLE) != null;
    }

/**
 * Reads a ReadMarshallable object from the wire.
 *
 * @param object The ReadMarshallable object to populate with the data.
 * @return True if the object was successfully read, false otherwise.
 * @throws BufferUnderflowException if the buffer underflows while reading
 * @throws IORuntimeException if an I/O error occurs
 * @throws InvalidMarshallableException if the ReadMarshallable object is invalid
 */
    default boolean marshallable(@NotNull ReadMarshallable object) throws BufferUnderflowException, IORuntimeException, InvalidMarshallableException {
        return marshallable(object, SerializationStrategies.MARSHALLABLE) != null;
    }

/**
 * Reads a boolean value from the wire.
 *
 * @return The boolean value read from the wire.
 */
    boolean bool();

/**
 * Reads an 8-bit integer (byte) value from the wire.
 *
 * @return The 8-bit integer value read from the wire.
 */
    byte int8();

/**
 * Reads a 16-bit integer (short) value from the wire.
 *
 * @return The 16-bit integer value read from the wire.
 */
    short int16();

/**
 * Reads an unsigned 16-bit integer (represented as an int) from the wire.
 *
 * @return The unsigned 16-bit integer value read from the wire.
 */
    int uint16();

/**
 * Reads a 32-bit integer (int) value from the wire.
 *
 * @return The 32-bit integer value read from the wire.
 */
    int int32();

/**
 * Reads a 32-bit integer (int) value from the wire with a previous value as a reference.
 *
 * @param previous The previous 32-bit integer value.
 * @return The 32-bit integer value read from the wire.
 */
    default int int32(int previous) {
        return int32();
    }

/**
 * Reads a 64-bit integer (long) value from the wire.
 *
 * @return The 64-bit integer value read from the wire.
 */
    long int64();

/**
 * Reads a 64-bit integer (long) value from the wire with a previous value as a reference.
 *
 * @param previous The previous 64-bit integer value.
 * @return The 64-bit integer value read from the wire.
 */
    default long int64(long previous) {
        return int64();
    }

    float float32();

/**
 * Reads a 32-bit floating-point (float) value from the wire with a previous value as a reference.
 *
 * @param previous The previous 32-bit floating-point value.
 * @return The 32-bit floating-point value read from the wire.
 */
    default float float32(float previous) {
        return float32();
    }

    /**
     * @return the value as a float, or -0.0 indicates that we have not been able to parse this data ( we don't throw an exception )
     */
    double float64();

/**
 * Reads a 64-bit floating-point (double) value from the wire with a previous value as a reference.
 *
 * @param previous The previous 64-bit floating-point value.
 * @return The 64-bit floating-point value read from the wire.
 */
    default double float64(double previous) {
        return float64();
    }

/**
 * Reads a Class type literal from the wire.
 *
 * @return The Class corresponding to the type literal read from the wire, or null if it cannot be resolved.
 * @throws IORuntimeException if an I/O error occurs
 * @throws BufferUnderflowException if the buffer underflows while reading
 */
    @Nullable
    default <T> Class<T> typeLiteral() throws IORuntimeException, BufferUnderflowException {
        return (Class<T>) typeLiteral((sb, e) -> {
            throw new IORuntimeException(e);
        });
    }

/**
 * Reads a type literal from the wire, returning it as a Type object, with a fallback for unresolved types.
 *
 * @return The Type corresponding to the literal read from the wire, or a fallback unresolved type.
 * @throws IORuntimeException if an I/O error occurs
 * @throws BufferUnderflowException if the buffer underflows while reading
 */
    @Nullable
    default Type lenientTypeLiteral() throws IORuntimeException, BufferUnderflowException {
        return typeLiteral((sb, e) -> UnresolvedType.of(sb.toString()));
    }

/**
 * Reads a type literal from the wire, applying a handler for unresolved types.
 *
 * @param unresolvedHandler The handler to apply for unresolved types.
 * @return The Type corresponding to the literal read from the wire, or the result of the unresolved handler.
 */
    Type typeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler);

/**
 * Reads a Throwable from the wire, optionally appending the current stack trace.
 *
 * @param appendCurrentStack True to append the current stack trace to the Throwable.
 * @return The Throwable read from the wire.
 * @throws InvalidMarshallableException if the Throwable object is invalid
 */
    default Throwable throwable(boolean appendCurrentStack) throws InvalidMarshallableException {
        return WireInternal.throwable(this, appendCurrentStack);
    }

/**
 * Reads an Enum value of the specified type from the wire.
 *
 * @param eClass The class of the Enum type.
 * @return The Enum value read from the wire, or null if it cannot be read.
 */
    @Nullable
    default <E extends Enum<E>> E asEnum(Class<E> eClass) {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            StringBuilder sb = stlSb.get();
            text(sb);
            return sb.length() == 0 ? null : WireInternal.internEnum(eClass, sb);
        }
    }

/**
 * Reads an Enum value of the specified type from the wire and applies it to a Consumer.
 *
 * @param eClass The class of the Enum type.
 * @param eConsumer The Consumer to apply the Enum value to.
 * @return The WireIn instance for method chaining.
 */
    @NotNull
    default <E extends Enum<E>> WireIn asEnum(Class<E> eClass, @NotNull Consumer<E> eConsumer) {
        eConsumer.accept(asEnum(eClass));
        return wireIn();
    }

/**
 * Reads an Enum value of the specified type from the wire and applies it to a given object using a BiConsumer.
 *
 * @param eClass The class of the Enum type.
 * @param t The object to apply the Enum value to.
 * @param teConsumer The BiConsumer that accepts the object and the Enum value.
 * @return The WireIn instance for method chaining.
 */
    @NotNull
    default <E extends Enum<E>, T> WireIn asEnum(Class<E> eClass, T t, @NotNull BiConsumer<T, E> teConsumer) {
        teConsumer.accept(t, asEnum(eClass));
        return wireIn();
    }

    /**
 * Reads an object of the specified class from the wire.
 *
 * @param clazz The class of the object to read.
 * @return The object read from the wire, or null if it cannot be read.
 * @throws InvalidMarshallableException if the object is invalid
     */
    @Nullable
    default <E> E object(@Nullable Class<E> clazz) throws InvalidMarshallableException {
        return Wires.object0(this, null, clazz);
    }

/**
 * Reads an object from the wire.
 *
 * @param <E> The type of the object to read.
 * @param using An instance of the object to reuse, or null to create a new instance.
 * @param clazz The class of the object to read.
 * @param bestEffort Set to true for best effort reading, which may not throw exceptions for some errors.
 * @return The object read from the wire, or null if it cannot be read.
 * @throws InvalidMarshallableException if the object is invalid
 */
    @Nullable
    default Object object() throws InvalidMarshallableException {
        @Nullable final Object o = objectWithInferredType(null, SerializationStrategies.ANY_OBJECT, null);
        return o;
    }

    /**
     * Used for logging whatever we can get
     *
     * @return the object or Throwable
     */
    default Object objectBestEffort() {
        ValidatableUtil.startValidateDisabled();
        try {
            return object();
        } catch (Throwable t) {
            return t;
        } finally {
            ValidatableUtil.endValidateDisabled();
        }
    }

    @Nullable
    default <E> E object(@Nullable E using, @Nullable Class clazz) throws InvalidMarshallableException {
        return object(using, clazz, true);
    }

    default <E> E object(@Nullable E using, @Nullable Class clazz, boolean bestEffort) throws InvalidMarshallableException {
        return Wires.object0(this, using, clazz, bestEffort);
    }

/**
 * Gets the bracket type for the current value in the wire.
 *
 * @return The BracketType of the current value.
 */
    @NotNull
    BracketType getBracketType();

/**
 * Checks if the current value in the wire is null.
 *
 * @return True if the current value is null, false otherwise.
 */
    boolean isNull();

/**
 * Reads an object of a specified class from the wire and applies it to a given object using a BiConsumer.
 *
 * @param <T> The type of the object to apply the read value to.
 * @param <E> The type of the object being read.
 * @param clazz The class of the object being read.
 * @param t The object to apply the read value to.
 * @param e The BiConsumer that accepts the object and the read value.
 * @return The WireIn instance for method chaining.
 * @throws InvalidMarshallableException if the object is invalid
 */
    @Nullable
    default <T, E> WireIn object(@NotNull Class<E> clazz, T t, @NotNull BiConsumer<T, E> e) throws InvalidMarshallableException {
        e.accept(t, object(clazz));
        return wireIn();
    }

/**
 * Checks if the current value in the wire is typed.
 *
 * @return True if the current value is typed, false otherwise.
 */
    boolean isTyped();

/**
 * Reads the type prefix from the wire.
 *
 * @return The type prefix as a Class, or null if it cannot be read.
 */
    @Nullable
    Class typePrefix();

    /**
     * read a class with a super class or actual class as a hint
     *
     * @param tClass the super-class, or actual class to use
     * @return the class or an instance of an object to use.
     * @throws ClassNotFoundRuntimeException if the specific class couldn't be found.
     */
    @Nullable
    default Object typePrefixOrObject(Class tClass) throws ClassNotFoundRuntimeException {
        return typePrefix();
    }

/**
 * Resets the internal state of this ValueIn.
 */
    void resetState();

/**
 * Reads an object from the wire with an inferred type.
 *
 * @param using An instance of the object to reuse, or null to create a new instance.
 * @param strategy The SerializationStrategy to use for reading the object.
 * @param type The type of the object to read.
 * @return The object read from the wire, or null if it cannot be read.
 * @throws InvalidMarshallableException if the object is invalid
 */
    @Nullable
    Object objectWithInferredType(Object using, SerializationStrategy strategy, Class type) throws InvalidMarshallableException;

    /**
     * Check if the value is present
     *
     * @return true if the value was present in the Wire, false otherwise
     */
    default boolean isPresent() {
        return true;
    }

/**
 * Reads a UUID from the wire.
 *
 * @return The UUID read from the wire, or null if it cannot be read.
 */
    @NotNull
    default UUID uuid() {
        return UUID.fromString(text());
    }

/**
 * Checks if the current value in the wire is binary.
 *
 * @return True if the current value is binary, false otherwise.
 */
    default boolean isBinary() {
        return false;
    }

/**
 * Reads a long value from the wire using a LongConverter.
 *
 * @param longConverter The LongConverter to use for reading the long value.
 * @return The long value read from the wire.
 */
    default long readLong(LongConverter longConverter) {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            StringBuilder sb = stlSb.get();
            text(sb);
            return longConverter.parse(sb);
        }
    }

/**
 * Reads a boolean value from the wire.
 *
 * @return The boolean value read from the wire.
 */
    default boolean readBoolean() {
        return bool();
    }

/**
 * Reads a byte value from the wire.
 *
 * @return The byte value read from the wire.
 */
    default byte readByte() {
        return int8();
    }

/**
 * Reads a char value from the wire.
 *
 * @return The char value read from the wire.
 */
    default char readChar() {
        return (char) uint16();
    }

/**
 * Reads a short value from the wire.
 *
 * @return The short value read from the wire.
 */
    default short readShort() {
        return int16();
    }

/**
 * Reads an int value from the wire.
 *
 * @return The int value read from the wire.
 */
    default int readInt() {
        return int32();
    }

/**
 * Reads a long value from the wire.
 *
 * @return The long value read from the wire.
 */
    default long readLong() {
        return int64();
    }

/**
 * Reads a float value from the wire.
 *
 * @return The float value read from the wire.
 */
    default float readFloat() {
        return float32();
    }

/**
 * Reads a double value from the wire.
 *
 * @return The double value read from the wire.
 */
    default double readDouble() {
        return float64();
    }

/**
 * Reads a String from the wire.
 *
 * @return The String read from the wire, or null if it cannot be read.
 */
    default String readString() {
        return text();
    }

/**
 * Interface for reading values from the wire into a list.
 */
    interface Reader {
        <T> void accept(ValueIn valueIn, List<T> list, List<T> buffer, Supplier<T> bufferAdd) throws InvalidMarshallableException;
    }
}
