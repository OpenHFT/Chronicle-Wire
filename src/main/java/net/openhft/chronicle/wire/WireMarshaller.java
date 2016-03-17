package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.ClassLocal;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

/**
 * Created by peter on 16/03/16.
 */
public class WireMarshaller<T> {
    static final ClassLocal<WireMarshaller> WIRE_MARSHALLER_CL = ClassLocal.withInitial(WireMarshaller::new);
    private final FieldAccess[] fields;
    private final boolean isLeaf;

    public WireMarshaller(Class<T> tClass) {
        Map<String, Field> map = new LinkedHashMap<>();
        getAllField(tClass, map);
        fields = map.values().stream()
                .map(FieldAccess::create).toArray(FieldAccess[]::new);
        isLeaf = map.values().stream()
                .map(Field::getType).noneMatch(
                        c -> WireMarshaller.class.isAssignableFrom(c) ||
                                isCollection(c));
//        System.out.println(tClass + " leaf= " + isLeaf);
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
            field.setAccessible(true);
            map.put(field.getName(), field);
        }
    }

    public void writeMarshallable(T t, WireOut out) {
        for (FieldAccess field : fields) {
            field.write(t, out);
        }
    }

    public void readMarshallable(T t, WireIn in) {
        for (FieldAccess field : fields) {
            field.read(t, in);
        }
    }

    static class FieldAccess {
        final Field field;
        final WireKey key;
        Boolean isLeaf;

        public FieldAccess(Field field) {
            this(field, null);
        }

        public FieldAccess(Field field, Boolean isLeaf) {
            this.field = field;
            key = field::getName;
            this.isLeaf = isLeaf;
//            System.out.println(field + " isLeaf=" + isLeaf);
        }

        public static Object create(Field field) {
            Class<?> type = field.getType();
            if (type.isArray())
                return new ArrayFieldAccess(field);
            if (Collection.class.isAssignableFrom(type))
                return new CollectionFieldAccess(field);
            if (Map.class.isAssignableFrom(type))
                return new MapFieldAccess(field);

            switch (type.getName()) {
                case "boolean":
                    return new BooleanFieldAccess(field);
                case "byte":
                    return new ByteFieldAccess(field);
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
                default:
                    Boolean isLeaf = null;
                    if (WireMarshaller.class.isAssignableFrom(type))
                        isLeaf = WIRE_MARSHALLER_CL.get(type).isLeaf;
                    else if (isCollection(type))
                        isLeaf = false;
                    return new FieldAccess(field, isLeaf);
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
                    ", key=" + key.name() +
                    ", isLeaf=" + isLeaf +
                    '}';
        }

        void write(Object o, WireOut out) {
            try {
                ValueOut write = out.write(key);
                getValue(o, write);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        protected void getValue(Object o, ValueOut write) throws IllegalAccessException {
            if (isLeaf != null)
                write.leaf(isLeaf);
            write.object(field.get(o));
        }

        void read(Object o, WireIn in) {
            try {
                ValueIn read = in.read(key);
                setValue(o, read);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        protected void setValue(Object o, ValueIn read) throws IllegalAccessException {
            field.set(o, read.object(field.getType()));
        }

    }

    static class ArrayFieldAccess extends FieldAccess {
        private final Class componentType;

        public ArrayFieldAccess(Field field) {
            super(field);
            componentType = field.getType().getComponentType();
        }

        @Override
        protected void getValue(Object o, ValueOut write) throws IllegalAccessException {
            write.sequence(o, (array, out) -> {
                for (int i = 0, len = Array.getLength(array); i < len; i++)
                    out.object(componentType, Array.get(array, i));
            });
        }

        @Override
        protected void setValue(Object o, ValueIn read) throws IllegalAccessException {
            read.sequence(o, (array, out) -> {
                for (int i = 0, len = Array.getLength(array); i < len; i++)
                    Array.set(array, i, out.object(componentType));
            });
        }
    }

    static class CollectionFieldAccess extends FieldAccess {
        final Supplier<Collection> collectionSupplier;
        private final Class componentType;
        private final Class<?> type;

        public CollectionFieldAccess(Field field) {
            super(field);
            type = field.getType();
            if (type == List.class || type == Collection.class)
                collectionSupplier = ArrayList::new;
            else if (type == SortedSet.class || type == NavigableSet.class)
                collectionSupplier = TreeSet::new;
            else if (type == Set.class)
                collectionSupplier = LinkedHashSet::new;
            else
                collectionSupplier = newInstance();
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) genericType;
                Type type0 = pType.getActualTypeArguments()[0];
                componentType = extractClass(type0);
                isLeaf = WIRE_MARSHALLER_CL.get(componentType).isLeaf;
            } else {
                componentType = Object.class;
            }
        }

        private Supplier<Collection> newInstance() {
            try {
                return (Supplier<Collection>) type.newInstance();
            } catch (InstantiationException e) {
                throw new AssertionError(e);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void getValue(Object o, ValueOut write) throws IllegalAccessException {
            Collection c = (Collection) field.get(o);
            write.sequence(c, (coll, out) -> {
                if (coll instanceof RandomAccess) {
                    List list = (List) coll;
                    for (int i = 0, len = list.size(); i < len; i++) {
                        if (Boolean.TRUE.equals(isLeaf)) out.leaf();
                        out.object(componentType, list.get(i));
                    }
                } else {
                    for (Object element : coll) {
                        if (Boolean.TRUE.equals(isLeaf)) out.leaf();
                        out.object(componentType, element);
                    }
                }
            });
        }

        void read(Object o, WireIn in) {
            try {
                ValueIn read = in.read(key);
                Collection coll = (Collection) field.get(o);
                if (coll == null) {
                    coll = collectionSupplier.get();
                    field.set(o, coll);
                } else {
                    coll.clear();
                }
                read.sequence(coll, (c, in2) -> {
                    while (in2.hasNextSequenceItem())
                        c.add(in2.object(componentType));
                });
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static class MapFieldAccess extends FieldAccess {
        final Supplier<Map> collectionSupplier;
        private final Class<?> type;
        private final Class keyType;
        private final Class valueType;

        public MapFieldAccess(Field field) {
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
            } catch (InstantiationException e) {
                throw new AssertionError(e);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void getValue(Object o, ValueOut write) throws IllegalAccessException {
            Map map = (Map) field.get(o);
            write.map(map);
        }

        void read(Object o, WireIn in) {
            try {
                ValueIn read = in.read(key);
                Map map = (Map) field.get(o);
                if (map == null) {
                    map = collectionSupplier.get();
                    field.set(o, map);
                } else {
                    map.clear();
                }
                read.map(keyType, valueType, map);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static class BooleanFieldAccess extends FieldAccess {
        public BooleanFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write) throws IllegalAccessException {
            write.bool(field.getBoolean(o));
        }

        @Override
        protected void setValue(Object o, ValueIn read) throws IllegalAccessException {
            field.setBoolean(o, read.bool());
        }
    }

    static class ByteFieldAccess extends FieldAccess {
        public ByteFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write) throws IllegalAccessException {
            write.int8(field.getByte(o));
        }

        @Override
        protected void setValue(Object o, ValueIn read) throws IllegalAccessException {
            field.setByte(o, read.int8());
        }
    }

    static class ShortFieldAccess extends FieldAccess {
        public ShortFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write) throws IllegalAccessException {
            write.int16(field.getShort(o));
        }

        @Override
        protected void setValue(Object o, ValueIn read) throws IllegalAccessException {
            field.setShort(o, read.int16());
        }
    }

    static class IntegerFieldAccess extends FieldAccess {
        public IntegerFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write) throws IllegalAccessException {
            write.int32(field.getInt(o));
        }

        @Override
        protected void setValue(Object o, ValueIn read) throws IllegalAccessException {
            field.setInt(o, read.int32());
        }
    }

    static class FloatFieldAccess extends FieldAccess {
        public FloatFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write) throws IllegalAccessException {
            write.float32(field.getFloat(o));
        }

        @Override
        protected void setValue(Object o, ValueIn read) throws IllegalAccessException {
            field.setFloat(o, read.float32());
        }
    }

    static class LongFieldAccess extends FieldAccess {
        public LongFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write) throws IllegalAccessException {
            write.int64(field.getLong(o));
        }

        @Override
        protected void setValue(Object o, ValueIn read) throws IllegalAccessException {
            field.setLong(o, read.int64());
        }
    }

    static class DoubleFieldAccess extends FieldAccess {
        public DoubleFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write) throws IllegalAccessException {
            write.float64(field.getDouble(o));
        }

        @Override
        protected void setValue(Object o, ValueIn read) throws IllegalAccessException {
            field.setDouble(o, read.float64());
        }
    }
}
