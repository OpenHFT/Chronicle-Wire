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
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by peter on 10/05/16.
 */
public enum SerializationStrategies implements SerializationStrategy {
    MARSHALLABLE {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            ((ReadMarshallable) o).readMarshallable(in.wireIn());
            return o;
        }

        @Override
        public Class type() {
            return Marshallable.class;
        }

        @Override
        public Object newInstance(Class type) {
            return type.isInterface() ? null : super.newInstance(type);
        }
    },
    ANY_OBJECT {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            return in.objectWithInferredType(o, ANY_NESTED, null);
        }

        @Override
        public Class type() {
            return Object.class;
        }

        @Override
        public BracketType bracketType() {
            return BracketType.UNKNOWN;
        }
    },

    ANY_SCALAR {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            return in.objectWithInferredType(o, ANY_NESTED, null);
        }

        @Override
        public Class type() {
            return Object.class;
        }

        @Override
        public BracketType bracketType() {
            return BracketType.NONE;
        }
    },
    ENUM {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            return in.objectWithInferredType(o, ANY_NESTED, null);
        }

        @Override
        public Class type() {
            return Enum.class;
        }

        @Override
        public BracketType bracketType() {
            return BracketType.NONE;
        }
    },
    ANY_NESTED {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            Wires.readMarshallable(o, in.wireIn(), true);
            return o;
        }

        @Override
        public Class type() {
            return Object.class;
        }

    },
    DEMARSHALLABLE {
        @Override
        public Object readUsing(Object using, ValueIn in) {
            final DemarshallableWrapper wrapper = (DemarshallableWrapper) using;
            wrapper.demarshallable = Demarshallable.newInstance(wrapper.type, in.wireIn());
            return wrapper;
        }

        @Override
        public Class type() {
            return Demarshallable.class;
        }

        @Override
        public Object newInstance(Class type) {
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

        @Override
        public Class type() {
            return Serializable.class;
        }
    },
    EXTERNALIZABLE {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            try {
                ((Externalizable) o).readExternal(in.wireIn().objectInput());
            } catch (IOException | ClassNotFoundException e) {
                throw new IORuntimeException(e);
            }
            return o;
        }

        @Override
        public Class type() {
            return Externalizable.class;
        }

        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    },
    MAP {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            Map<Object, Object> map = (Map<Object, Object>) o;
            final WireIn wireIn = in.wireIn();
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

        @Override
        public Object newInstance(@Nullable Class type) {

            if (type == null)
                return new LinkedHashMap<>();

            return SortedMap.class.isAssignableFrom(type) ? new TreeMap<>() : new LinkedHashMap<>();
        }

        @Override
        public Class type() {
            return Map.class;
        }
    },
    SET {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            Set<Object> set = (Set<Object>) o;
            final WireIn wireIn = in.wireIn();
            final Bytes<?> bytes = wireIn.bytes();
            long pos = bytes.readPosition();
            while (in.hasNextSequenceItem()) {
                final Object object = in.object();
                set.add(object);

                // make sure we are progressing.
                long pos2 = bytes.readPosition();
                if (pos2 <= pos)
                    if (!Jvm.isDebug())
                        throw new IllegalStateException(bytes.toDebugString());
                pos = pos2;
            }
            return o;
        }

        @Override
        public Object newInstance(Class type) {
            return SortedSet.class.isAssignableFrom(type) ? new TreeSet<>() : new LinkedHashSet<>();
        }

        @Override
        public Class type() {
            return Set.class;
        }

        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    },
    LIST {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            List<Object> list = (List<Object>) o;
            final WireIn wireIn = in.wireIn();
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

        @Override
        public Object newInstance(Class type) {
            return new ArrayList<>();
        }

        @Override
        public Class type() {
            return List.class;
        }

        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    },
    ARRAY {
        @Override
        public Object readUsing(Object using, ValueIn in) {
            ArrayWrapper wrapper = (ArrayWrapper) using;
            final Class componentType = wrapper.type.getComponentType();
            List list = new ArrayList<>();
            while (in.hasNextSequenceItem())
                list.add(in.object(componentType));
            wrapper.array = list.toArray((Object[]) Array.newInstance(componentType, list.size()));
            return wrapper;
        }

        @Override
        public Class type() {
            return Object[].class;
        }

        @Override
        public Object newInstance(Class type) {
            return new ArrayWrapper(type);
        }

        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    }, PRIM_ARRAY {
        @Override
        public Object readUsing(Object using, ValueIn in) {
            PrimArrayWrapper wrapper = (PrimArrayWrapper) using;
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

        @Override
        public Class type() {
            return Object.class;
        }

        @Override
        public Object newInstance(Class type) {
            return new PrimArrayWrapper(type);
        }

        @Override
        public BracketType bracketType() {
            return BracketType.SEQ;
        }
    };

    @Override
    public Object newInstance(Class type) {
        return ObjectUtils.newInstance(type);
    }

    @Override
    public BracketType bracketType() {
        return BracketType.MAP;
    }

    static class ArrayWrapper implements ReadResolvable<Object[]> {
        final Class type;
        Object[] array;

        ArrayWrapper(Class type) {
            this.type = type;
        }

        @Override
        public Object[] readResolve() {
            return array;
        }
    }

    static class PrimArrayWrapper implements ReadResolvable<Object> {
        final Class type;
        Object array;

        PrimArrayWrapper(Class type) {
            this.type = type;
        }

        @Override
        public Object readResolve() {
            return array;
        }
    }

    static class DemarshallableWrapper implements ReadResolvable<Demarshallable> {
        final Class type;
        Demarshallable demarshallable;

        DemarshallableWrapper(Class type) {
            this.type = type;
        }

        @Override
        public Demarshallable readResolve() {
            return demarshallable;
        }
    }
}
