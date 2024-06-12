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
import net.openhft.chronicle.bytes.CommonMarshallable;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.pool.EnumCache;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.core.util.ReadResolvable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static net.openhft.chronicle.wire.BracketType.UNKNOWN;

@SuppressWarnings({"rawtypes", "unchecked"})
/**
 * Enumerates the available serialization strategies, each implementing the {@link SerializationStrategy} interface.
 * These strategies cater to different serialization requirements and support specific object types.
 */
public enum SerializationStrategies implements SerializationStrategy {

    /**
     * A serialization strategy for objects implementing the {@link Marshallable} interface.
     * This strategy supports both self-describing messages and raw byte data serialization.
     */
    MARSHALLABLE {
        /**
         * Reads an object from the provided input source using a wire format.
         * The object can be deserialized either as a self-describing message or as raw byte data,
         * based on its type.
         *
         * @return The populated object.
         */
        @NotNull
        @Override
        public Object readUsing(Class clazz, @NotNull Object o, @NotNull ValueIn in, BracketType bracketType) throws InvalidMarshallableException {
            WireIn wireIn = in.wireIn();
            if (wireIn.useSelfDescribingMessage((CommonMarshallable) o) && o instanceof ReadMarshallable) {
                ((ReadMarshallable) o).readMarshallable(wireIn);
            } else {
                ((ReadBytesMarshallable) o).readMarshallable(wireIn.bytes());
            }
            return o;
        }

        /**
         * Returns the type of object this serialization strategy supports, which is {@link Marshallable}.
         *
         * @return The {@link Marshallable} class.
         */
        @NotNull
        @Override
        public Class type() {
            return Marshallable.class;
        }

        /**
         * Creates a new instance of the specified type.
         * If the type represents an interface or an abstract class, {@code null} is returned.
         *
         * @return A new instance of the provided type or {@code null} if the type is an interface or abstract.
         */
        @Nullable
        @Override
        public Object newInstanceOrNull(@NotNull Class type) {
            return type.isInterface() || Modifier.isAbstract(type.getModifiers()) ? null : super.newInstanceOrNull(type);
        }
    },

    /**
     * A serialization strategy that supports any object type.
     * This strategy infers the object type during deserialization.
     */
    ANY_OBJECT {
        /**
         * Reads an object from the provided input source, inferring its type.
         * The type is not explicitly described in the serialized data.
         *
         * @return The populated object with its type inferred from the input.
         */
        @Nullable
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) throws InvalidMarshallableException {
            return in.objectWithInferredType(o, ANY_NESTED, null);
        }

        /**
         * Returns the most generic type of object this serialization strategy supports, which is {@link Object}.
         *
         * @return The {@link Object} class.
         */
        @NotNull
        @Override
        public Class type() {
            return Object.class;
        }

        /**
         * Indicates that the bracket type for this strategy is unknown.
         *
         * @return The {@link BracketType#UNKNOWN}.
         */
        @NotNull
        @Override
        public BracketType bracketType() {
            return UNKNOWN;
        }
    },

    /**
     * A serialization strategy that supports any scalar value.
     * Scalar values are typically primitive types or their boxed equivalents.
     */
    ANY_SCALAR {
        /**
         * Reads a scalar object from the provided input source, inferring its type.
         *
         * @return The populated scalar object with its type inferred from the input.
         */
        @Nullable
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) throws InvalidMarshallableException {
            return in.objectWithInferredType(o, ANY_NESTED, null);
        }

        /**
         * Returns the most generic type of scalar this serialization strategy supports, which is {@link Object}.
         *
         * @return The {@link Object} class.
         */
        @NotNull
        @Override
        public Class type() {
            return Object.class;
        }

        /**
         * Indicates that this strategy does not use any bracketing for serialization.
         *
         * @return The {@link BracketType#NONE}.
         */
        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.NONE;
        }
    },

    /**
     * A serialization strategy specifically designed for handling enumerations (enums).
     */
    ENUM {
        /**
         * Reads an enum value from the provided input source, represented as text.
         *
         * @return The textual representation of the enum value.
         */
        @Nullable
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) {
            return in.text();
        }

        /**
         * Returns the type of object this serialization strategy supports, which is {@link Enum}.
         *
         * @return The {@link Enum} class.
         */
        @NotNull
        @Override
        public Class type() {
            return Enum.class;
        }

        /**
         * Indicates that this strategy does not use any bracketing for serialization.
         *
         * @return The {@link BracketType#NONE}.
         */
        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.NONE;
        }
    },

    /**
     * A serialization strategy designed for handling dynamic enumerations (enums).
     * Dynamic enums are enums whose values can be altered or enhanced at runtime.
     * This strategy incorporates custom handling to ensure proper deserialization
     * of dynamic enums from the input source.
     */
    DYNAMIC_ENUM {

        // Reflective field access to the ordinal of the Enum class
        private Field ordinal = Jvm.getField(Enum.class, "ordinal");

        /**
         * Reads a dynamic enum value from the provided input source.
         * This method accounts for multiple input representations,
         * such as direct textual names or custom serialized formats
         * for more complex dynamic enums.
         *
         * @return The deserialized dynamic enum object or its textual representation.
         */
        @Nullable
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) throws InvalidMarshallableException {
            if (bracketType != BracketType.MAP || !(o instanceof ReadMarshallable)) {
                String text = in.text();
                if (o != null) {
                    EnumCache<?> cache = EnumCache.of(o.getClass());
                    Object ret = cache.valueOf(text);
                    if (ret == null)
                        throw new IORuntimeException("No enum value '" + text + "' defined for " + o.getClass());
                    return ret;
                }
                return text;
            }
            ((ReadMarshallable) o).readMarshallable(in.wireIn());
            return o;
        }

        /**
         * Returns the type of object this serialization strategy supports, which is {@link Enum}.
         *
         * @return The {@link Enum} class.
         */
        @NotNull
        @Override
        public Class type() {
            return Enum.class;
        }

        /**
         * Indicates that the bracket type used for serialization is unknown.
         *
         * @return The {@link BracketType#UNKNOWN}.
         */
        @NotNull
        @Override
        public BracketType bracketType() {
            return UNKNOWN;
        }

        /**
         * Constructs a new instance of a dynamic enum.
         * The constructed instance is left in an unset state, where the name is
         * marked as "[unset]" and the ordinal is set to -1.
         *
         * @return The constructed dynamic enum instance.
         */
        @Override
        public Object newInstanceOrNull(Class type) {
            try {
                DynamicEnum o = (DynamicEnum) UnsafeMemory.INSTANCE.allocateInstance(type);
                o.setField("name", "[unset]");
                if (o instanceof Enum)
                    ordinal.set(o, -1);
                return o;
            } catch (Exception e) {
                throw new IORuntimeException(e);
            }
        }
    },

    /**
     * A serialization strategy designed for handling nested objects.
     * This strategy reads nested objects without explicitly knowing
     * their exact type, treating them as generic objects and performing
     * a generic deserialization.
     */
    ANY_NESTED {

        /**
         * Reads the nested object from the provided input source.
         * If the input is null, it returns null. Otherwise, it
         * deserializes the object and returns the constructed instance.
         *
         * @return The deserialized nested object.
         */
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) throws InvalidMarshallableException {
            if (in.isNull()) {
                return null;
            }
            if (o == null)
                o = ObjectUtils.newInstance(clazz);
            Wires.readMarshallable(clazz, o, in.wireIn(), true);
            return o;
        }

        /**
         * Returns the type of object this serialization strategy supports, which is {@link Object}.
         *
         * @return The {@link Object} class.
         */
        @NotNull
        @Override
        public Class type() {
            return Object.class;
        }

        /**
         * Indicates that the bracket type used for serialization is unknown.
         *
         * @return The {@link BracketType#UNKNOWN}.
         */
        @Override
        public @NotNull BracketType bracketType() {
            return UNKNOWN;
        }
    },

    /**
     * A serialization strategy specifically designed for handling
     * {@link Demarshallable} objects. {@link Demarshallable} represents
     * an object that can be deserialized from a wire format.
     */
    DEMARSHALLABLE {

        /**
         * Reads the {@link Demarshallable} object from the provided input source.
         * This method has logic to handle multiple formats of input including
         * wrapped objects and direct serialized formats.
         *
         * @return The deserialized {@link Demarshallable} object or wrapper.
         */
        @NotNull
        @Override
        public Object readUsing(Class clazz, Object using, @NotNull ValueIn in, BracketType bracketType) throws InvalidMarshallableException {
            if (using instanceof DemarshallableWrapper) {
                @NotNull final DemarshallableWrapper wrapper = (DemarshallableWrapper) using;
                wrapper.demarshallable = Demarshallable.newInstance(wrapper.type, in.wireIn());
                return wrapper;
            } else if (using instanceof ReadMarshallable) {
                return in.object(using, Object.class);
            } else {
                return Demarshallable.newInstance((Class) using.getClass(), in.wireIn());
            }
        }

        /**
         * Returns the type of object this serialization strategy supports, which is {@link Demarshallable}.
         *
         * @return The {@link Demarshallable} class.
         */
        @NotNull
        @Override
        public Class type() {
            return Demarshallable.class;
        }

        /**
         * Constructs a new instance of a {@link Demarshallable} wrapped in a {@link DemarshallableWrapper}.
         *
         * @return The constructed {@link DemarshallableWrapper}.
         */
        @NotNull
        @Override
        public Object newInstanceOrNull(@NotNull Class type) {
            return new DemarshallableWrapper(type);
        }
    },

    /**
     * A serialization strategy for handling objects that implement the
     * {@link java.io.Serializable} interface. This strategy checks if the object
     * is also an instance of {@link Externalizable}, and if so, uses the
     * {@link #EXTERNALIZABLE} strategy. Otherwise, it defaults to the
     * {@link #ANY_OBJECT} strategy.
     */
    SERIALIZABLE {

        /**
         * Reads the {@link Serializable} object from the provided input source.
         * Delegates the deserialization to the appropriate strategy based on
         * whether the object is an instance of {@link Externalizable}.
         *
         * @return The deserialized {@link Serializable} object.
         */
        @Override
        public Object readUsing(Class clazz, Object o, ValueIn in, BracketType bracketType) throws InvalidMarshallableException {
            SerializationStrategies strategies = o instanceof Externalizable ? EXTERNALIZABLE : ANY_OBJECT;
            strategies.readUsing(clazz, o, in, bracketType);
            return o;
        }

        /**
         * Returns the type of object this serialization strategy supports,
         * which is {@link Serializable}.
         *
         * @return The {@link Serializable} class.
         */
        @NotNull
        @Override
        public Class type() {
            return Serializable.class;
        }
    },

    /**
     * A serialization strategy specifically designed for handling
     * {@link Externalizable} objects. Externalizable objects provide their
     * own custom serialization mechanism that this strategy leverages.
     */
    EXTERNALIZABLE {

        /**
         * Reads the {@link Externalizable} object from the provided input source
         * using the object's custom serialization logic.
         *
         * @return The deserialized {@link Externalizable} object.
         */
        @NotNull
        @Override
        public Object readUsing(Class clazz, @NotNull Object o, @NotNull ValueIn in, BracketType bracketType) {
            try {
                ((Externalizable) o).readExternal(in.wireIn().objectInput());
            } catch (@NotNull IOException | ClassNotFoundException e) {
                throw new IORuntimeException(e);
            }
            return o;
        }

        /**
         * Returns the type of object this serialization strategy supports,
         * which is {@link Externalizable}.
         *
         * @return The {@link Externalizable} class.
         */
        @NotNull
        @Override
        public Class type() {
            return Externalizable.class;
        }

        /**
         * Indicates that the bracket type used for serialization is SEQ.
         *
         * @return The {@link BracketType#SEQ}.
         */
        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    },

    /**
     * A serialization strategy for handling objects that implement the
     * {@link java.util.Map} interface. This strategy deserializes a sequence
     * of key-value pairs into a Map instance.
     */
    MAP {

        /**
         * Reads the {@link Map} object from the provided input source, mapping
         * each key-value pair in the sequence.
         *
         * @return The deserialized {@link Map} object.
         */
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) throws InvalidMarshallableException {
            @NotNull Map<Object, Object> map = (o == null ? new LinkedHashMap<>() : (Map<Object, Object>) o);
            @NotNull final WireIn wireIn = in.wireIn();
            long pos = wireIn.bytes().readPosition();
            while (in.hasNext()) {
                Object key = wireIn.readEvent(Object.class);
                map.put(key, in.object());

                // make sure we are progressing.
                long pos2 = wireIn.bytes().readPosition();
                if (pos2 <= pos && !Jvm.isDebug())
                    throw new IllegalStateException(wireIn.bytes().toDebugString());
                pos = pos2;
            }
            return map;
        }

        /**
         * Creates a new instance of a {@link Map}, either {@link LinkedHashMap} or
         * {@link TreeMap} based on the type.
         *
         * @return The new {@link Map} instance.
         */
        @NotNull
        @Override
        public Object newInstanceOrNull(@Nullable Class type) {

            if (type == null)
                return new LinkedHashMap<>();

            return SortedMap.class.isAssignableFrom(type) ? new TreeMap<>() : new LinkedHashMap<>();
        }

        /**
         * Returns the type of object this serialization strategy supports,
         * which is {@link Map}.
         *
         * @return The {@link Map} class.
         */
        @NotNull
        @Override
        public Class type() {
            return Map.class;
        }
    },

    /**
     * A serialization strategy for handling objects that implement the
     * {@link java.util.Set} interface. This strategy deserializes a sequence
     * of items into a Set instance.
     */
    SET {

        /**
         * Reads the {@link Set} object from the provided input source, adding
         * each item in the sequence to the set.
         *
         * @return The deserialized {@link Set} object.
         */
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) throws InvalidMarshallableException {
            @NotNull Set<Object> set = (o == null ? new LinkedHashSet<>() : (Set<Object>) o);
            @NotNull final WireIn wireIn = in.wireIn();
            @NotNull final Bytes<?> bytes = wireIn.bytes();
            long pos = bytes.readPosition();
            while (in.hasNextSequenceItem()) {
                @Nullable final Object object = in.object();
                // make sure we are progressing.
                long pos2 = bytes.readPosition();
                if (pos2 <= pos && !Jvm.isDebug())
                    throw new IllegalStateException(bytes.toDebugString());
                pos = pos2;
                set.add(object);
            }
            return set;
        }

        /**
         * Creates a new instance of a {@link Set}, either {@link LinkedHashSet} or
         * {@link TreeSet} based on the type.
         *
         * @return The new {@link Set} instance.
         */
        @NotNull
        @Override
        public Object newInstanceOrNull(@NotNull Class type) {
            return SortedSet.class.isAssignableFrom(type) ? new TreeSet<>() : new LinkedHashSet<>();
        }

        /**
         * Returns the type of object this serialization strategy supports,
         * which is {@link Set}.
         *
         * @return The {@link Set} class.
         */
        @NotNull
        @Override
        public Class type() {
            return Set.class;
        }

        /**
         * Specifies the bracket type associated with this strategy.
         *
         * @return The bracket type.
         */
        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    },

    /**
     * A serialization strategy for handling objects that implement the
     * {@link java.util.List} interface. This strategy deserializes a sequence
     * of items into a List instance.
     */
    LIST {

        /**
         * Reads the {@link List} object from the provided input source, adding
         * each item in the sequence to the list.
         *
         * @return The deserialized {@link List} object.
         */
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) throws InvalidMarshallableException {
            @NotNull List<Object> list = (o == null ? new ArrayList<>() : (List<Object>) o);
            @NotNull final WireIn wireIn = in.wireIn();
            long pos = wireIn.bytes().readPosition();
            int count = 0;
            while (in.hasNextSequenceItem()) {
                if (list.size() > count) {
                    list.set(count, in.object(list.get(count), Object.class));
                } else {
                    list.add(in.object());
                }
                count++;
                // make sure we are progressing.
                long pos2 = wireIn.bytes().readPosition();
                if (pos2 <= pos && !Jvm.isDebug())
                    throw new IllegalStateException(wireIn.bytes().toDebugString());
                pos = pos2;
            }
            while (list.size() > count)
                list.remove(list.size() - 1);
            return list;
        }

        /**
         * Creates a new instance of an {@link ArrayList}.
         *
         * @return The new {@link List} instance.
         */
        @NotNull
        @Override
        public Object newInstanceOrNull(Class type) {
            return new ArrayList<>();
        }

        /**
         * Returns the type of object this serialization strategy supports,
         * which is {@link List}.
         *
         * @return The {@link List} class.
         */
        @NotNull
        @Override
        public Class type() {
            return List.class;
        }

        /**
         * Specifies the bracket type associated with this strategy.
         *
         * @return The bracket type.
         */
        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    },

    /**
     * A serialization strategy for handling arrays. This strategy deserializes
     * a sequence of items into an array instance.
     */
    ARRAY {

        /**
         * Reads an array object from the provided input source, adding each
         * item in the sequence to a list, which is then converted into an array.
         * <p>
         * If the 'using' parameter is an instance of ArrayWrapper, the method
         * first deserializes items into a list and then wraps them into an array
         * with the correct component type. Otherwise, it deserializes items into
         * a list and then converts that list into an array.
         *
         * @return The deserialized array or an ArrayWrapper containing the array.
         */
        @NotNull
        @Override
        public Object readUsing(Class clazz, Object using, @NotNull ValueIn in, BracketType bracketType) throws InvalidMarshallableException {
            if (using instanceof ArrayWrapper) {
                @NotNull ArrayWrapper wrapper = (ArrayWrapper) using;
                final Class componentType = wrapper.type.getComponentType();
                @NotNull List list = new ArrayList<>();
                while (in.hasNextSequenceItem())
                    list.add(in.object(componentType));
                wrapper.array = list.toArray((Object[]) Array.newInstance(componentType, list.size()));
                return wrapper;
            } else {
                @NotNull List list = new ArrayList<>();
                while (in.hasNextSequenceItem())
                    list.add(in.object());
                return list.toArray();
            }
        }

        /**
         * Returns the type of object this serialization strategy supports,
         * which is an array.
         *
         * @return The Object[].class.
         */
        @NotNull
        @Override
        public Class type() {
            return Object[].class;
        }

        /**
         * Creates a new instance of an ArrayWrapper, which is a wrapper for
         * arrays that is used during the deserialization process.
         *
         * @return The new ArrayWrapper instance.
         */
        @NotNull
        @Override
        public Object newInstanceOrNull(@NotNull Class type) {
            return new ArrayWrapper(type);
        }

        /**
         * Specifies the bracket type associated with this strategy.
         *
         * @return The bracket type.
         */
        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    },

    /**
     * A serialization strategy for handling primitive arrays. This strategy
     * deserializes a sequence of items into an array instance, dynamically
     * resizing the array as required during deserialization.
     */
    PRIM_ARRAY {

        /**
         * Reads a primitive array object from the provided input source,
         * dynamically resizing the array during the deserialization process.
         * <p>
         * The method starts with an empty array and doubles its size as needed
         * to accommodate the incoming data. After all items have been read,
         * the array is resized to fit the number of items that were read.
         *
         * @return The PrimArrayWrapper containing the deserialized primitive array.
         */
        @NotNull
        @Override
        public Object readUsing(Class clazz, Object using, @NotNull ValueIn in, BracketType bracketType) throws InvalidMarshallableException {
            @NotNull PrimArrayWrapper wrapper = (PrimArrayWrapper) using;
            final Class<?> componentType = wrapper.type.getComponentType();
            int i = 0;
            int len = 0;
            Object array = Array.newInstance(componentType, 0);
            while (in.hasNextSequenceItem()) {
                if (i >= len) {
                    int len2 = len * 2 + 2;
                    Object array2 = Array.newInstance(componentType, len2);
                    System.arraycopy(array, 0, array2, 0, len);
                    len = len2;
                    array = array2;
                }
                Array.set(array, i++, in.object(componentType));
            }
            if (i < len) {
                Object array2 = Array.newInstance(componentType, i);
                System.arraycopy(array, 0, array2, 0, i);
                array = array2;
            }
            wrapper.array = array;
            return wrapper;
        }

        /**
         * Returns the type of object this serialization strategy supports.
         * For this strategy, the type is a generic Object class because it
         * covers all types of primitive arrays.
         *
         * @return The Object.class.
         */
        @NotNull
        @Override
        public Class type() {
            return Object.class;
        }

        /**
         * Creates a new instance of a PrimArrayWrapper, which is a wrapper
         * for primitive arrays used during the deserialization process.
         *
         * @return The new PrimArrayWrapper instance.
         */
        @NotNull
        @Override
        public Object newInstanceOrNull(@NotNull Class type) {
            return new PrimArrayWrapper(type);
        }

        /**
         * Specifies the bracket type associated with this strategy.
         *
         * @return The bracket type.
         */
        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    };

    /**
     * The provided methods and class are related to an object's instantiation and its bracket type
     * definition for serialization purposes.
     * <p>
     * Attempts to create a new instance of the given class.
     *
     * @param type The class for which a new instance is to be created.
     * @return A new instance of the given class or {@code null} if the instantiation fails.
     */
    @Nullable
    @Override
    public Object newInstanceOrNull(Class type) {
        return ObjectUtils.newInstanceOrNull(type);
    }

    /**
     * Specifies the bracket type associated with this strategy.
     *
     * @return The bracket type. For this strategy, it is defined as MAP.
     */
    @NotNull
    @Override
    public BracketType bracketType() {
        return BracketType.MAP;
    }

    /**
     * A wrapper class for arrays which provides a mechanism for deserialization
     * by resolving the actual array when being read from a serialized source.
     */
    static class ArrayWrapper implements ReadResolvable<Object[]> {

        /**
         * The class type of the elements in the array.
         */
        @NotNull
        final Class type;

        /**
         * The actual array wrapped by this wrapper.
         */
        Object[] array;

        /**
         * Constructs an ArrayWrapper for a specified type.
         *
         * @param type The class type of the elements in the array.
         */
        ArrayWrapper(@NotNull Class type) {
            this.type = type;
        }

        /**
         * Provides a deserialization mechanism which returns the actual array
         * when this wrapper is being read from a serialized source.
         *
         * @return The actual array wrapped by this wrapper.
         */
        @NotNull
        @Override
        public Object @NotNull [] readResolve() {
            return array;
        }
    }

    /**
     * The following classes are wrappers that facilitate deserialization processes for
     * specific types of objects. They implement the ReadResolvable interface to define
     * the deserialization mechanism.
     * <p>
     * A wrapper class for primitive arrays which provides a mechanism for deserialization
     * by resolving the actual array when being read from a serialized source.
     */
    static class PrimArrayWrapper implements ReadResolvable<Object> {

        /**
         * The class type of the elements in the primitive array.
         */
        @NotNull
        final Class type;

        /**
         * The actual primitive array wrapped by this wrapper.
         */
        Object array;

        /**
         * Constructs a PrimArrayWrapper for a specified type.
         *
         * @param type The class type of the elements in the primitive array.
         */
        PrimArrayWrapper(@NotNull Class type) {
            this.type = type;
        }

        /**
         * Provides a deserialization mechanism which returns the actual primitive array
         * when this wrapper is being read from a serialized source.
         *
         * @return The actual primitive array wrapped by this wrapper.
         */
        @NotNull
        @Override
        public Object readResolve() {
            return array;
        }
    }

    /**
     * A wrapper class for Demarshallable objects which provides a mechanism for deserialization
     * by resolving the actual Demarshallable object when being read from a serialized source.
     */
    static class DemarshallableWrapper implements ReadResolvable<Demarshallable> {

        /**
         * The class type of the Demarshallable object.
         */
        @NotNull
        final Class type;

        /**
         * The actual Demarshallable object wrapped by this wrapper.
         */
        Demarshallable demarshallable;

        /**
         * Constructs a DemarshallableWrapper for a specified type.
         *
         * @param type The class type of the Demarshallable object.
         */
        DemarshallableWrapper(@NotNull Class type) {
            this.type = type;
        }

        /**
         * Provides a deserialization mechanism which returns the actual Demarshallable object
         * when this wrapper is being read from a serialized source.
         *
         * @return The actual Demarshallable object wrapped by this wrapper.
         */
        @NotNull
        @Override
        public Demarshallable readResolve() {
            return demarshallable;
        }
    }
}
