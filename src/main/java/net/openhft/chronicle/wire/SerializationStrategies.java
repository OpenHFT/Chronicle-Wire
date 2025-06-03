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

/**
 * Enumerates the available serialization strategies, each implementing the {@link SerializationStrategy} interface.
 * These strategies cater to different serialization requirements and support specific object types.
 */
@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
public enum SerializationStrategies implements SerializationStrategy {

    /**
     * Strategy for objects implementing {@link Marshallable}.
     * Supports both self-describing and raw byte forms.
     */
    MARSHALLABLE {
        /**
         * Reads the object either via {@link ReadMarshallable#readMarshallable(WireIn)}
         * or {@link ReadBytesMarshallable#readMarshallable(Bytes)}. The choice is
         * governed by {@link WireIn#useSelfDescribingMessage(CommonMarshallable)}.
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
         * Returns {@code null} for interfaces or abstract types.
         */
        @Nullable
        @Override
        public Object newInstanceOrNull(@NotNull Class type) {
            return type.isInterface() || Modifier.isAbstract(type.getModifiers()) ? null : super.newInstanceOrNull(type);
        }
    },

    /**
     * Handles objects of any type. The actual strategy is chosen at read time
     * once the type has been inferred from the wire.
     */
    ANY_OBJECT {
        /**
         * Infers the type from the wire and then delegates to {@link #ANY_NESTED}
         * or another appropriate strategy.
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
     * Handles scalar values such as primitives and boxed types. Scalars are
     * written without explicit map or list brackets.
     */
    ANY_SCALAR {
        /**
         * Reads a scalar value and infers the target type from the wire.
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
     * Strategy for standard {@link Enum} types. Values are written as their {@code name()}.
     */
    ENUM {
        /**
         * Reads an enum by consuming its textual name.
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
     * Supports dynamic enums whose values may change at run time. Special logic
     * deals with both simple text forms and map based representations.
     */
    DYNAMIC_ENUM {

        // Reflective field access to the ordinal of the Enum class
        private final Field ordinal = Jvm.getField(Enum.class, "ordinal");

        /**
         * Reads a dynamic enum. A plain text name is resolved via {@link EnumCache}.
         * If {@code o} implements {@link ReadMarshallable} and a map is present,
         * that map is read into the instance.
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
     * Deserialises nested objects without knowing the target type. Data is
     * typically encoded in a map style structure.
     */
    ANY_NESTED {

        /**
         * Reads a nested object or returns {@code null} if the input is null.
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
     * Handles {@link Demarshallable} objects. A wrapper may be used during
     * reading to defer construction until {@link #readUsing} is invoked.
     */
    DEMARSHALLABLE {

        /**
         * Reads a {@link Demarshallable}. If {@code using} is a
         * {@link DemarshallableWrapper}, a new instance is created and stored in
         * the wrapper. Otherwise the existing object is populated.
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
         * Returns a {@link DemarshallableWrapper} used as a temporary holder
         * until the real object is read.
         */
        @NotNull
        @Override
        public Object newInstanceOrNull(@NotNull Class type) {
            return new DemarshallableWrapper(type);
        }
    },

    /**
     * Handles {@link Serializable} objects. If an instance is also
     * {@link Externalizable} its own read logic is used, otherwise the data is
     * read via {@link #ANY_OBJECT}.
     */
    SERIALIZABLE {

        /**
         * Delegates to {@link #EXTERNALIZABLE} when required, otherwise to
         * {@link #ANY_OBJECT}.
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
     * Handles {@link Externalizable} objects and calls their
     * {@link java.io.Externalizable#readExternal(java.io.ObjectInput)} method.
     */
    EXTERNALIZABLE {

        /**
         * Delegates to {@link java.io.Externalizable#readExternal(java.io.ObjectInput)}
         * using an {@link java.io.ObjectInput} facade over the wire.
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
     * Reads key value pairs into a {@link Map}.
     */
    MAP {

        /**
         * Consumes key value pairs until no more entries remain in the current
         * map context.
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
     * Reads a sequence of items into a {@link Set} implementation.
     */
    SET {

        /**
         * Consumes items from the wire and adds them to the given set, creating
         * one if necessary.
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
     * Reads a sequence of items into a {@link List} implementation.
     */
    LIST {

        /**
         * Populates the supplied list. Existing entries are reused where
         * possible; surplus entries are removed.
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
     * Reads a sequence of items into an array. Uses {@link ArrayWrapper} when
     * the target type is known at construction time.
     */
    ARRAY {

        /**
         * Collects items into a list and then converts that list to an array or
         * populates the supplied {@link ArrayWrapper}.
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
     * Reads primitive arrays using a {@link PrimArrayWrapper}. The array grows
     * as items are read and is trimmed to size when complete.
     */
    PRIM_ARRAY {

        /**
         * Reads items into the primitive array held by the wrapper, expanding the
         * array when it becomes full and finally shrinking it to the exact length.
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
     * Temporary holder for arrays during deserialization. {@link #readResolve()}
     * returns the actual array so callers see only the final result.
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
         * {@inheritDoc}
         */
        @NotNull
        @Override
        public Object @NotNull [] readResolve() {
            return array;
        }
    }

    /**
     * Temporary holder for primitive arrays during deserialization.
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
         * {@inheritDoc}
         */
        @NotNull
        @Override
        public Object readResolve() {
            return array;
        }
    }

    /**
     * Wrapper used when reading {@link Demarshallable} objects lazily.
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
         * {@inheritDoc}
         */
        @NotNull
        @Override
        public Demarshallable readResolve() {
            return demarshallable;
        }
    }
}
