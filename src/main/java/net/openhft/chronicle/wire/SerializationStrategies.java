/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.core.util.ReadResolvable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.*;

/*
 * Created by Peter Lawrey on 10/05/16.
 */
public enum SerializationStrategies implements SerializationStrategy {
    MARSHALLABLE {
        @NotNull
        @Override
        public Object readUsing(@NotNull Object o, @NotNull ValueIn in) {
            ((ReadMarshallable) o).readMarshallable(in.wireIn());
            return o;
        }

        @NotNull
        @Override
        public Class type() {
            return Marshallable.class;
        }

        @Nullable
        @Override
        public Object newInstance(@NotNull Class type) {
            return type.isInterface() || Modifier.isAbstract(type.getModifiers()) ? null : super.newInstance(type);
        }
    },
    ANY_OBJECT {
        @Nullable
        @Override
        public Object readUsing(Object o, @NotNull ValueIn in) {
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
            return BracketType.UNKNOWN;
        }
    },

    ANY_SCALAR {
        @Nullable
        @Override
        public Object readUsing(Object o, @NotNull ValueIn in) {
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
        public Object readUsing(Object o, @NotNull ValueIn in) {
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
    ANY_NESTED {
        @NotNull
        @Override
        public Object readUsing(@NotNull Object o, @NotNull ValueIn in) {
            Wires.readMarshallable(o, in.wireIn(), true);
            return o;
        }

        @NotNull
        @Override
        public Class type() {
            return Object.class;
        }

    },
    DEMARSHALLABLE {
        @NotNull
        @Override
        public Object readUsing(Object using, @NotNull ValueIn in) {
            @NotNull final DemarshallableWrapper wrapper = (DemarshallableWrapper) using;
            wrapper.demarshallable = Demarshallable.newInstance(wrapper.type, in.wireIn());
            return wrapper;
        }

        @NotNull
        @Override
        public Class type() {
            return Demarshallable.class;
        }

        @NotNull
        @Override
        public Object newInstance(@NotNull Class type) {
            return new DemarshallableWrapper(type);
        }
    },
    SERIALIZABLE {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            if (o instanceof Externalizable)
                EXTERNALIZABLE.readUsing(o, in);
            else
                ANY_OBJECT.readUsing(o, in);
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
        public Object readUsing(@NotNull Object o, @NotNull ValueIn in) {
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
        public Object readUsing(Object o, @NotNull ValueIn in) {
            @NotNull Map<Object, Object> map = (Map<Object, Object>) o;
            @NotNull final WireIn wireIn = in.wireIn();
            long pos = wireIn.bytes().readPosition();
            while (in.hasNext()) {
                Object key = wireIn.readEvent(Object.class);
                map.put(key, in.object());

                // make sure we are progressing.
                long pos2 = wireIn.bytes().readPosition();
                if (pos2 <= pos)
                    if (!Jvm.isDebug())
                        throw new IllegalStateException(wireIn.bytes().toDebugString());
                pos = pos2;
            }
            return o;
        }

        @NotNull
        @Override
        public Object newInstance(@Nullable Class type) {

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
        public Object readUsing(Object o, @NotNull ValueIn in) {
            @NotNull Set<Object> set = (Set<Object>) o;
            @NotNull final WireIn wireIn = in.wireIn();
            @NotNull final Bytes<?> bytes = wireIn.bytes();
            long pos = bytes.readPosition();
            while (in.hasNextSequenceItem()) {
                @Nullable final Object object = in.object();
                // make sure we are progressing.
                long pos2 = bytes.readPosition();
                if (pos2 <= pos)
                    if (!Jvm.isDebug())
                        throw new IllegalStateException(bytes.toDebugString());
                pos = pos2;
                set.add(object);
            }
            return o;
        }

        @NotNull
        @Override
        public Object newInstance(@NotNull Class type) {
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
        public Object readUsing(Object o, @NotNull ValueIn in) {
            @NotNull List<Object> list = (List<Object>) o;
            @NotNull final WireIn wireIn = in.wireIn();
            long pos = wireIn.bytes().readPosition();
            while (in.hasNextSequenceItem()) {
                list.add(in.object());

                // make sure we are progressing.
                long pos2 = wireIn.bytes().readPosition();
                if (pos2 <= pos)
                    if (!Jvm.isDebug())
                        throw new IllegalStateException(wireIn.bytes().toDebugString());
                pos = pos2;
            }
            return o;
        }

        @NotNull
        @Override
        public Object newInstance(Class type) {
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
        public Object readUsing(Object using, @NotNull ValueIn in) {
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
        public Object newInstance(@NotNull Class type) {
            return new ArrayWrapper(type);
        }

        @NotNull
        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    }, PRIM_ARRAY {
        @NotNull
        @Override
        public Object readUsing(Object using, @NotNull ValueIn in) {
            @NotNull PrimArrayWrapper wrapper = (PrimArrayWrapper) using;
            final Class componentType = wrapper.type.getComponentType();
            int i = 0, len = 0;
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
        public Object newInstance(@NotNull Class type) {
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
    public Object newInstance(Class type) {
        return ObjectUtils.newInstance(type);
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
        public Object[] readResolve() {
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
