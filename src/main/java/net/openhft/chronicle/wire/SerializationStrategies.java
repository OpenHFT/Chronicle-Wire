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
import net.openhft.chronicle.bytes.CommonMarshallable;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.io.IORuntimeException;
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
public enum SerializationStrategies implements SerializationStrategy {
    MARSHALLABLE {
        @NotNull
        @Override
        public Object readUsing(Class clazz, @NotNull Object o, @NotNull ValueIn in, BracketType bracketType) {
            WireIn wireIn = in.wireIn();
            if (wireIn.useSelfDescribingMessage((CommonMarshallable) o) && o instanceof ReadMarshallable) {
                ((ReadMarshallable) o).readMarshallable(wireIn);
            } else {
                ((ReadBytesMarshallable) o).readMarshallable(wireIn.bytes());
            }
            return o;
        }

        @NotNull
        @Override
        public Class type() {
            return Marshallable.class;
        }

        @Nullable
        @Override
        public Object newInstanceOrNull(@NotNull Class type) {
            return type.isInterface() || Modifier.isAbstract(type.getModifiers()) ? null : super.newInstanceOrNull(type);
        }
    },

    ANY_OBJECT {
        @Nullable
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) {
            return in.objectWithInferredType(o, ANY_NESTED, null);
        }

        @NotNull
        @Override
        public Class type() {
            return Object.class;
        }

        @NotNull
        @Override
        public BracketType bracketType() {
            return UNKNOWN;
        }
    },

    ANY_SCALAR {
        @Nullable
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) {
            return in.objectWithInferredType(o, ANY_NESTED, null);
        }

        @NotNull
        @Override
        public Class type() {
            return Object.class;
        }

        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.NONE;
        }
    },

    ENUM {
        @Nullable
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) {
            return in.text();
        }

        @NotNull
        @Override
        public Class type() {
            return Enum.class;
        }

        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.NONE;
        }
    },

    DYNAMIC_ENUM {
        private Field ordinal = Jvm.getField(Enum.class, "ordinal");

        @Nullable
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) {
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

        @NotNull
        @Override
        public Class type() {
            return Enum.class;
        }

        @NotNull
        @Override
        public BracketType bracketType() {
            return UNKNOWN;
        }

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

    ANY_NESTED {
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) {
            if (in.isNull()) {
                return null;
            }
            if (o == null)
                o = ObjectUtils.newInstance(clazz);
            Wires.readMarshallable(clazz, o, in.wireIn(), true);
            return o;
        }

        @NotNull
        @Override
        public Class type() {
            return Object.class;
        }

        @Override
        public @NotNull BracketType bracketType() {
            return UNKNOWN;
        }
    },

    DEMARSHALLABLE {
        @NotNull
        @Override
        public Object readUsing(Class clazz, Object using, @NotNull ValueIn in, BracketType bracketType) {
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

        @NotNull
        @Override
        public Class type() {
            return Demarshallable.class;
        }

        @NotNull
        @Override
        public Object newInstanceOrNull(@NotNull Class type) {
            return new DemarshallableWrapper(type);
        }
    },

    SERIALIZABLE {
        @Override
        public Object readUsing(Class clazz, Object o, ValueIn in, BracketType bracketType) {
            SerializationStrategies strategies = o instanceof Externalizable ? EXTERNALIZABLE : ANY_OBJECT;
            strategies.readUsing(clazz, o, in, bracketType);
            return o;
        }

        @NotNull
        @Override
        public Class type() {
            return Serializable.class;
        }
    },

    EXTERNALIZABLE {
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

        @NotNull
        @Override
        public Class type() {
            return Externalizable.class;
        }

        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    },

    MAP {
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) {
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

        @NotNull
        @Override
        public Object newInstanceOrNull(@Nullable Class type) {

            if (type == null)
                return new LinkedHashMap<>();

            return SortedMap.class.isAssignableFrom(type) ? new TreeMap<>() : new LinkedHashMap<>();
        }

        @NotNull
        @Override
        public Class type() {
            return Map.class;
        }
    },

    SET {
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) {
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

        @NotNull
        @Override
        public Object newInstanceOrNull(@NotNull Class type) {
            return SortedSet.class.isAssignableFrom(type) ? new TreeSet<>() : new LinkedHashSet<>();
        }

        @NotNull
        @Override
        public Class type() {
            return Set.class;
        }

        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    },

    LIST {
        @Override
        public Object readUsing(Class clazz, Object o, @NotNull ValueIn in, BracketType bracketType) {
            @NotNull List<Object> list = (o == null ? new ArrayList<>() : (List<Object>) o);
            @NotNull final WireIn wireIn = in.wireIn();
            long pos = wireIn.bytes().readPosition();
            while (in.hasNextSequenceItem()) {
                list.add(in.object());

                // make sure we are progressing.
                long pos2 = wireIn.bytes().readPosition();
                if (pos2 <= pos && !Jvm.isDebug())
                    throw new IllegalStateException(wireIn.bytes().toDebugString());
                pos = pos2;
            }
            return list;
        }

        @NotNull
        @Override
        public Object newInstanceOrNull(Class type) {
            return new ArrayList<>();
        }

        @NotNull
        @Override
        public Class type() {
            return List.class;
        }

        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    },

    ARRAY {
        @NotNull
        @Override
        public Object readUsing(Class clazz, Object using, @NotNull ValueIn in, BracketType bracketType) {
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

        @NotNull
        @Override
        public Class type() {
            return Object[].class;
        }

        @NotNull
        @Override
        public Object newInstanceOrNull(@NotNull Class type) {
            return new ArrayWrapper(type);
        }

        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    },

    PRIM_ARRAY {
        @NotNull
        @Override
        public Object readUsing(Class clazz, Object using, @NotNull ValueIn in, BracketType bracketType) {
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

        @NotNull
        @Override
        public Class type() {
            return Object.class;
        }

        @NotNull
        @Override
        public Object newInstanceOrNull(@NotNull Class type) {
            return new PrimArrayWrapper(type);
        }

        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    };

    @Nullable
    @Override
    public Object newInstanceOrNull(Class type) {
        return ObjectUtils.newInstanceOrNull(type);
    }

    @NotNull
    @Override
    public BracketType bracketType() {
        return BracketType.MAP;
    }

    static class ArrayWrapper implements ReadResolvable<Object[]> {
        @NotNull
        final Class type;
        Object[] array;

        ArrayWrapper(@NotNull Class type) {
            this.type = type;
        }

        @NotNull
        @Override
        public Object @NotNull [] readResolve() {
            return array;
        }
    }

    static class PrimArrayWrapper implements ReadResolvable<Object> {
        @NotNull
        final Class type;
        Object array;

        PrimArrayWrapper(@NotNull Class type) {
            this.type = type;
        }

        @NotNull
        @Override
        public Object readResolve() {
            return array;
        }
    }

    static class DemarshallableWrapper implements ReadResolvable<Demarshallable> {
        @NotNull
        final Class type;
        Demarshallable demarshallable;

        DemarshallableWrapper(@NotNull Class type) {
            this.type = type;
        }

        @NotNull
        @Override
        public Demarshallable readResolve() {
            return demarshallable;
        }
    }
}