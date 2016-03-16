package net.openhft.chronicle.wire;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by peter on 16/03/16.
 */
public class WireMarshaller<T> {
    private final FieldAccess[] fields;

    public WireMarshaller(Class<T> tClass) {
        Map<String, Field> map = new LinkedHashMap<>();
        getAllField(tClass, map);
        fields = map.values().stream().map(FieldAccess::create).toArray(FieldAccess[]::new);
    }

    public static void getAllField(Class clazz, Map<String, Field> map) {
        if (clazz != Object.class)
            getAllField(clazz.getSuperclass(), map);
        for (Field field : clazz.getDeclaredFields()) {
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

        public FieldAccess(Field field) {
            this.field = field;
            key = field::getName;
        }

        public static Object create(Field field) {
            switch (field.getType().getName()) {
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
                    return new FieldAccess(field);
            }
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
