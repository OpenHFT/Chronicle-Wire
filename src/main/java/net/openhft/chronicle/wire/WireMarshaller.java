/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.ClassLocal;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.StringUtils;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static net.openhft.chronicle.core.UnsafeMemory.UNSAFE;

/**
 * Created by peter on 16/03/16.
 */
public class WireMarshaller<T> {
   public static final ClassLocal<WireMarshaller> WIRE_MARSHALLER_CL = ClassLocal.withInitial
            (tClass ->
            Throwable.class.isAssignableFrom(tClass)
                    ? WireMarshaller.ofThrowable(tClass)
                    : WireMarshaller.of(tClass)
    );
    final FieldAccess[] fields;
    private final Class<T> tClass;
    private final boolean isLeaf;

    public WireMarshaller(Class<T> tClass, FieldAccess[] fields, boolean isLeaf) {
        this.tClass = tClass;
        this.fields = fields;
        this.isLeaf = isLeaf;
    }

    public static <T> WireMarshaller<T> of(Class<T> tClass) {
        Map<String, Field> map = new LinkedHashMap<>();
        getAllField(tClass, map);
        final FieldAccess[] fields = map.values().stream()
                .map(FieldAccess::create).toArray(FieldAccess[]::new);
        boolean isLeaf = map.values().stream()
                .map(Field::getType).noneMatch(
                        c -> WireMarshaller.class.isAssignableFrom(c) ||
                                isCollection(c));
        return new WireMarshaller<>(tClass, fields, isLeaf);
    }

    private static <T> WireMarshaller<T> ofThrowable(Class<T> tClass) {
        Map<String, Field> map = new LinkedHashMap<>();
        getAllField(tClass, map);
        final FieldAccess[] fields = map.values().stream()
                .map(FieldAccess::create).toArray(FieldAccess[]::new);
        boolean isLeaf = false;
        return new WireMarshaller<>(tClass, fields, isLeaf);
    }

    private static boolean isCollection(Class<?> c) {
        return c.isArray() ||
                Collection.class.isAssignableFrom(c) ||
                Map.class.isAssignableFrom(c);
    }

    public static void getAllField(Class clazz, Map<String, Field> map) {
        if (clazz != Object.class)
            getAllField(clazz.getSuperclass(), map);
        for (Field field : clazz.getDeclaredFields()) {
            if ((field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) != 0)
                continue;
            if (field.getName().equals("this$0"))
                throw new IllegalArgumentException("Found this$0, classes must be static or top level");
            field.setAccessible(true);
            map.put(field.getName(), field);
        }
    }

    public void writeMarshallable(T t, WireOut out) {
        try {
            for (FieldAccess field : fields) {
                field.write(t, out);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void writeMarshallable(T t, Bytes bytes) {
        for (FieldAccess field : fields) {
            try {
                field.getAsBytes(t, bytes);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    public void writeMarshallable(T t, WireOut out, T previous, boolean copy) {
        try {
            for (FieldAccess field : fields) {
                field.write(t, out, previous, copy);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void readMarshallable(T t, WireIn in, boolean overwrite) {
        try {
            for (FieldAccess field : fields) {
                field.read(t, in, overwrite);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void writeKey(T t, Bytes bytes) {
        // assume one key for now.
        try {
            fields[0].getAsBytes(t, bytes);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public boolean isEqual(Object o1, Object o2) {
        for (FieldAccess field : fields) {
            if (!field.isEqual(o1, o2))
                return false;
        }
        return true;
    }

    static abstract class FieldAccess {
        final Field field;
        final long offset;
        final WireKey key;
        Boolean isLeaf;

        FieldAccess(Field field) {
            this(field, null);
        }

        FieldAccess(Field field, Boolean isLeaf) {
            this.field = field;
            offset = UNSAFE.objectFieldOffset(field);
            key = field::getName;
            this.isLeaf = isLeaf;
//            System.out.println(field + " isLeaf=" + isLeaf);
        }

        public static Object create(Field field) {
            Class<?> type = field.getType();
            if (type.isArray())
                return new ArrayFieldAccess(field);
            if (Collection.class.isAssignableFrom(type))
                return CollectionFieldAccess.of(field);
            if (Map.class.isAssignableFrom(type))
                return new MapFieldAccess(field);

            switch (type.getName()) {
                case "boolean":
                    return new BooleanFieldAccess(field);
                case "byte":
                    return new ByteFieldAccess(field);
                case "char":
                    return new CharFieldAccess(field);
                case "short":
                    return new ShortFieldAccess(field);
                case "int":
                    return new IntegerFieldAccess(field);
                case "float":
                    return new FloatFieldAccess(field);
                case "long":
                    return new LongFieldAccess(field);
                case "double":
                    return new DoubleFieldAccess(field);
                case "java.lang.String":
                    return new StringFieldAccess(field);
                case "java.lang.StringBuilder":
                    return new StringBuilderFieldAccess(field);
                default:
                    Boolean isLeaf = null;
                    if (WireMarshaller.class.isAssignableFrom(type))
                        isLeaf = WIRE_MARSHALLER_CL.get(type).isLeaf;
                    else if (isCollection(type))
                        isLeaf = false;
                    return new ObjectFieldAccess(field, isLeaf);
            }
        }

        static Class extractClass(Type type0) {
            if (type0 instanceof Class)
                return (Class) type0;
            else if (type0 instanceof ParameterizedType)
                return (Class) ((ParameterizedType) type0).getRawType();
            else
                return Object.class;
        }

        @Override
        public String toString() {
            return "FieldAccess{" +
                    "field=" + field +
                    ", isLeaf=" + isLeaf +
                    '}';
        }

        void write(Object o, WireOut out) throws IllegalAccessException {
            ValueOut write = out.write(field.getName());
            getValue(o, write, null);
        }

        void write(Object o, WireOut out, Object previous, boolean copy) throws IllegalAccessException {
            if (sameValue(o, previous))
                return;
            ValueOut write = out.write(field.getName());
            getValue(o, write, previous);
            if (copy)
                copy(o, previous);
        }

        protected boolean sameValue(Object o, Object o2) throws IllegalAccessException {
            final Object v1 = field.get(o);
            final Object v2 = field.get(o2);
            if (v1 instanceof CharSequence && v2 instanceof CharSequence)
                return StringUtils.isEqual((CharSequence) v1, (CharSequence) v2);
            return Objects.equals(v1, v2);
        }

        protected void copy(Object from, Object to) throws IllegalAccessException {
            UNSAFE.putObject(to, offset, UNSAFE.getObject(from, offset));
        }

        protected abstract void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException;

        void read(Object o, WireIn in, boolean overwrite) throws IllegalAccessException {
            ValueIn read = in.read(key);
            if (overwrite || !(read instanceof DefaultValueIn))
                setValue(o, read, overwrite);
        }

        protected abstract void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException;

        public abstract void getAsBytes(Object o, Bytes bytes) throws IllegalAccessException;

        public boolean isEqual(Object o1, Object o2) {
            try {
                return sameValue(o1, o2);
            } catch (IllegalAccessException e) {
                return false;
            }
        }
    }

    static class StringBuilderFieldAccess extends FieldAccess {

        public StringBuilderFieldAccess(Field field) {
            super(field, true);
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            CharSequence cs = (CharSequence) UNSAFE.getObject(o, offset);
            write.text(cs);
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            StringBuilder sb = (StringBuilder) UNSAFE.getObject(o, offset);
            if (sb == null)
                UNSAFE.putObject(o, offset, sb = new StringBuilder());
            if (read.textTo(sb) == null)
                UNSAFE.putObject(o, offset, null);
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) throws IllegalAccessException {
            bytes.writeUtf8((CharSequence) UNSAFE.getObject(o, offset));
        }

        @Override
        protected boolean sameValue(Object o1, Object o2) throws IllegalAccessException {
            return StringUtils.isEqual((StringBuilder) field.get(o1), (StringBuilder) field.get(o2));
        }
    }

    static class ObjectFieldAccess extends FieldAccess {
        private final Class type;

        ObjectFieldAccess(Field field, Boolean isLeaf) {
            super(field, isLeaf);
            type = field.getType();
        }

        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            if (isLeaf != null)
                write.leaf(isLeaf);
            assert o != null;
            write.object(type, field.get(o));
        }

        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            try {
                field.set(o, read.object(field.get(o), type));
            } catch (Exception e) {
                throw new IORuntimeException("Error reading " + field, e);
            }
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) throws IllegalAccessException {
            bytes.writeUtf8(String.valueOf(field.get(o)));
        }
    }

    static class StringFieldAccess extends FieldAccess {
        StringFieldAccess(Field field) {
            super(field, false);
        }

        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            write.text((String) UNSAFE.getObject(o, offset));
        }

        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            UNSAFE.putObject(o, offset, read.text());
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) throws IllegalAccessException {
            bytes.writeUtf8((String) UNSAFE.getObject(o, offset));
        }
    }

    static class ArrayFieldAccess extends FieldAccess {
        private final Class componentType;

        ArrayFieldAccess(Field field) {
            super(field);
            componentType = field.getType().getComponentType();
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            Object arr = field.get(o);
            if (arr == null)
                write.nu11();
            else
                write.sequence(arr, (array, out) -> {
                    for (int i = 0, len = Array.getLength(array); i < len; i++)
                        out.object(componentType, Array.get(array, i));
                });
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            final Object arr = field.get(o);
            if (read.isNull()) {
                if (arr != null)
                    field.set(o, null);
                return;
            }
            List list = new ArrayList();
            read.sequence(list, (l, in) -> {
                while (in.hasNextSequenceItem())
                    l.add(in.object(componentType));
            });
            Object arr2 = Array.newInstance(componentType, list.size());
            for (int i = 0; i < list.size(); i++)
                Array.set(arr2, i, list.get(i));
            field.set(o, arr2);
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEqual(Object o1, Object o2) {
            try {
                Object a1 = field.get(o1);
                Object a2 = field.get(o2);
                if (a1 == null) return a2 == null;
                if (a2 == null) return false;
                if (a1.getClass() != a2.getClass())
                    return false;
                int len1 = Array.getLength(a1);
                int len2 = Array.getLength(a2);
                if (len1 != len2)
                    return false;
                for (int i = 0; i < len1; i++)
                    if (!Objects.equals(Array.get(a1, i), Array.get(a2, i)))
                        return false;
                return true;
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static class CollectionFieldAccess extends FieldAccess {
        final Supplier<Collection> collectionSupplier;
        private final Class componentType;
        private final Class<?> type;

        public CollectionFieldAccess(Field field, Boolean isLeaf, Supplier<Collection> collectionSupplier, Class componentType, Class<?> type) {
            super(field, isLeaf);
            this.collectionSupplier = collectionSupplier == null ? newInstance() : collectionSupplier;
            this.componentType = componentType;
            this.type = type;
        }

        static FieldAccess of(Field field) {
            final Supplier<Collection> collectionSupplier;
            final Class componentType;
            final Class<?> type;
            Boolean isLeaf = null;
            type = field.getType();
            if (type == List.class || type == Collection.class)
                collectionSupplier = ArrayList::new;
            else if (type == SortedSet.class || type == NavigableSet.class)
                collectionSupplier = TreeSet::new;
            else if (type == Set.class)
                collectionSupplier = LinkedHashSet::new;
            else
                collectionSupplier = null;
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) genericType;
                Type type0 = pType.getActualTypeArguments()[0];
                componentType = extractClass(type0);
                isLeaf = !Throwable.class.isAssignableFrom(componentType)
                        && WIRE_MARSHALLER_CL.get(componentType).isLeaf;
            } else {
                componentType = Object.class;
            }
            return componentType == String.class
                    ? new StringCollectionFieldAccess(field, true, collectionSupplier, type)
                    : new CollectionFieldAccess(field, isLeaf, collectionSupplier, componentType, type);
        }

        private Supplier<Collection> newInstance() {
            return () -> {
                try {
                    return (Collection) type.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            };
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            Collection c = (Collection) field.get(o);
            if (c == null) {
                write.nu11();
                return;
            }
            write.sequence(c, (coll, out) -> {
                if (coll instanceof RandomAccess) {
                    List list = (List) coll;
                    for (int i = 0, len = list.size(); i < len; i++) {
                        if (Boolean.TRUE.equals(isLeaf)) out.leaf();
                        out.object(componentType, list.get(i));
                    }
                } else if (coll == null) {
                    try {
                        field.set(o, null);
                    } catch (IllegalAccessException e) {
                        throw new AssertionError(e);
                    }
                } else {
                    for (Object element : coll) {
                        if (Boolean.TRUE.equals(isLeaf)) out.leaf();
                        out.object(componentType, element);
                    }
                }
            });

        }

        @Override
        void read(Object o, WireIn in, boolean overwrite) throws IllegalAccessException {
            ValueIn read = in.read(key);
            Collection coll = (Collection) field.get(o);
            if (coll == null) {
                coll = collectionSupplier.get();
                field.set(o, coll);
            } else {
                coll.clear();
            }
            if (!read.sequence(coll, (c, in2) -> {
                while (in2.hasNextSequenceItem())
                    c.add(in2.object(componentType));
            })) {
                field.set(o, null);
            }
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean sameValue(Object o, Object o2) throws IllegalAccessException {
            return super.sameValue(o, o2);
        }
    }

    static class StringCollectionFieldAccess extends FieldAccess {
        final Supplier<Collection> collectionSupplier;
        private final Class<?> type;
        private BiConsumer<Collection, ValueIn> seqConsumer = (c, in2) -> {
            while (in2.hasNextSequenceItem())
                c.add(in2.text());
        };

        public StringCollectionFieldAccess(Field field, Boolean isLeaf, Supplier<Collection> collectionSupplier, Class<?> type) {
            super(field, isLeaf);
            this.collectionSupplier = collectionSupplier == null ? newInstance() : collectionSupplier;
            this.type = type;
        }

        private Supplier<Collection> newInstance() {
            return () -> {
                try {
                    return (Collection) type.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            };
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            Collection<String> c = (Collection<String>) field.get(o);
            if (c == null) {
                write.nu11();
                return;
            }
            write.sequence(c, (coll, out) -> {
                if (coll instanceof RandomAccess) {
                    List<String> list = (List<String>) coll;
                    for (int i = 0, len = list.size(); i < len; i++)
                        out.text(list.get(i));

                } else {
                    for (String element : coll)
                        out.text(element);
                }
            });
        }

        @Override
        void read(Object o, WireIn in, boolean overwrite) throws IllegalAccessException {
            ValueIn read = in.read(key);
            Collection coll = (Collection) field.get(o);
            if (coll == null) {
                coll = collectionSupplier.get();
                field.set(o, coll);
            } else {
                coll.clear();
            }
            if (!read.sequence(coll, seqConsumer)) {
                field.set(o, null);
            }
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) {
            throw new UnsupportedOperationException();
        }
    }

    static class MapFieldAccess extends FieldAccess {
        final Supplier<Map> collectionSupplier;
        private final Class<?> type;
        private final Class keyType;
        private final Class valueType;

        MapFieldAccess(Field field) {
            super(field);
            type = field.getType();
            if (type == Map.class)
                collectionSupplier = LinkedHashMap::new;
            else if (type == SortedMap.class || type == NavigableMap.class)
                collectionSupplier = TreeMap::new;
            else
                collectionSupplier = newInstance();
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) genericType;
                Type[] actualTypeArguments = pType.getActualTypeArguments();
                keyType = extractClass(actualTypeArguments[0]);
                valueType = extractClass(actualTypeArguments[1]);

            } else {
                keyType = Object.class;
                valueType = Object.class;
            }
        }

        private Supplier<Map> newInstance() {
            try {
                return (Supplier<Map>) type.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            Map map = (Map) field.get(o);
            write.marshallable(map);
        }

        @Override
        void read(Object o, WireIn in, boolean overwrite) throws IllegalAccessException {
            ValueIn read = in.read(key);
            Map map = (Map) field.get(o);
            if (map == null) {
                map = collectionSupplier.get();
                field.set(o, map);
            } else {
                map.clear();
            }
            if (read.marshallableAsMap(keyType, valueType, map) == null)
                field.set(o, null);
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) {
            throw new UnsupportedOperationException();
        }
    }

    static class BooleanFieldAccess extends FieldAccess {
        BooleanFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            write.bool(UNSAFE.getBoolean(o, offset));
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            UNSAFE.putBoolean(o, offset, read.bool());
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) throws IllegalAccessException {
            bytes.writeBoolean(UNSAFE.getBoolean(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) throws IllegalAccessException {
            return UNSAFE.getBoolean(o, offset) == UNSAFE.getBoolean(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) throws IllegalAccessException {
            UNSAFE.putBoolean(to, offset, UNSAFE.getBoolean(from, offset));
        }
    }

    static class ByteFieldAccess extends FieldAccess {
        ByteFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            write.int8(UNSAFE.getByte(o, offset));
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            UNSAFE.putByte(o, offset, read.int8());
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) throws IllegalAccessException {
            bytes.writeByte(UNSAFE.getByte(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) throws IllegalAccessException {
            return UNSAFE.getByte(o, offset) == UNSAFE.getByte(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) throws IllegalAccessException {
            UNSAFE.putByte(to, offset, UNSAFE.getByte(from, offset));
        }
    }

    static class ShortFieldAccess extends FieldAccess {
        ShortFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            write.int16(UNSAFE.getShort(o, offset));
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            UNSAFE.putShort(o, offset, read.int16());
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) throws IllegalAccessException {
            bytes.writeShort(UNSAFE.getShort(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) throws IllegalAccessException {
            return UNSAFE.getShort(o, offset) == UNSAFE.getShort(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) throws IllegalAccessException {
            UNSAFE.putShort(to, offset, UNSAFE.getShort(from, offset));
        }
    }

    static class CharFieldAccess extends FieldAccess {
        CharFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            StringBuilder sb = new StringBuilder();
            sb.append(UNSAFE.getChar(o, offset));
            write.text(sb);
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            UNSAFE.putChar(o, offset, read.text().charAt(0));
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) throws IllegalAccessException {
            bytes.writeUnsignedShort(UNSAFE.getChar(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) throws IllegalAccessException {
            return UNSAFE.getChar(o, offset) == UNSAFE.getChar(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) throws IllegalAccessException {
            UNSAFE.putChar(to, offset, UNSAFE.getChar(from, offset));
        }
    }

    static class IntegerFieldAccess extends FieldAccess {
        IntegerFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            if (previous == null)
                write.int32(UNSAFE.getInt(o, offset));
            else
                write.int32(UNSAFE.getInt(o, offset), UNSAFE.getInt(previous, offset));
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            int i = overwrite ? read.int32() : read.int32(UNSAFE.getInt(o, offset));
            UNSAFE.putInt(o, offset, i);
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) throws IllegalAccessException {
            bytes.writeInt(UNSAFE.getInt(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) throws IllegalAccessException {
            return UNSAFE.getInt(o, offset) == UNSAFE.getInt(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) throws IllegalAccessException {
            UNSAFE.putInt(to, offset, UNSAFE.getInt(from, offset));
        }
    }

    static class FloatFieldAccess extends FieldAccess {
        FloatFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            if (previous == null)
                write.float32(UNSAFE.getFloat(o, offset));
            else
                write.float32(UNSAFE.getFloat(o, offset), UNSAFE.getFloat(previous, offset));
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            final float v = overwrite ? read.float32() : read.float32(UNSAFE.getFloat(o, offset));
            UNSAFE.putFloat(o, offset, v);
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) throws IllegalAccessException {
            bytes.writeFloat(UNSAFE.getFloat(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) throws IllegalAccessException {
            return Maths.same(UNSAFE.getFloat(o, offset), UNSAFE.getFloat(o2, offset));
        }

        @Override
        protected void copy(Object from, Object to) throws IllegalAccessException {
            UNSAFE.putFloat(to, offset, UNSAFE.getFloat(from, offset));
        }
    }

    static class LongFieldAccess extends FieldAccess {
        LongFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            if (previous == null)
                write.int64(UNSAFE.getLong(o, offset));
            else
                write.int64(UNSAFE.getLong(o, offset), UNSAFE.getLong(previous, offset));
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            long i = overwrite ? read.int64() : read.int64(UNSAFE.getLong(o, offset));
            UNSAFE.putLong(o, offset, i);
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) throws IllegalAccessException {
            bytes.writeLong(UNSAFE.getLong(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) throws IllegalAccessException {
            return UNSAFE.getLong(o, offset) == UNSAFE.getLong(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) throws IllegalAccessException {
            UNSAFE.putLong(to, offset, UNSAFE.getLong(from, offset));
        }
    }

    static class DoubleFieldAccess extends FieldAccess {
        DoubleFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            if (previous == null)
                write.float64(UNSAFE.getDouble(o, offset));
            else
                write.float64(UNSAFE.getDouble(o, offset), UNSAFE.getDouble(previous, offset));
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            final double v = overwrite ? read.float64() : read.float64(UNSAFE.getDouble(o, offset));
            UNSAFE.putDouble(o, offset, v);
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) throws IllegalAccessException {
            bytes.writeDouble(UNSAFE.getDouble(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) throws IllegalAccessException {
            return Maths.same(UNSAFE.getDouble(o, offset), UNSAFE.getDouble(o2, offset));
        }

        @Override
        protected void copy(Object from, Object to) throws IllegalAccessException {
            UNSAFE.putDouble(to, offset, UNSAFE.getDouble(from, offset));
        }
    }
}
