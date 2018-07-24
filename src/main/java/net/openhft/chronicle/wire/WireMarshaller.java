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
import net.openhft.chronicle.bytes.BytesComment;
import net.openhft.chronicle.core.ClassLocal;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static net.openhft.chronicle.core.UnsafeMemory.UNSAFE;
import static net.openhft.chronicle.wire.Wires.acquireStringBuilder;

/*
 * Created by Peter Lawrey on 16/03/16.
 */
public class WireMarshaller<T> {
    public static final Class[] UNEXPECTED_FIELDS_PARAMETER_TYPES = {Object.class, ValueIn.class};
    private static final FieldAccess[] NO_FIELDS = {};
    public static final ClassLocal<WireMarshaller> WIRE_MARSHALLER_CL = ClassLocal.withInitial
            (tClass ->
                    Throwable.class.isAssignableFrom(tClass)
                            ? WireMarshaller.ofThrowable(tClass)
                            : WireMarshaller.of(tClass)
            );
    @NotNull
    final FieldAccess[] fields;
    private final boolean isLeaf;
    @Nullable
    private final T defaultValue;

    public WireMarshaller(@NotNull Class<T> tClass, @NotNull FieldAccess[] fields, boolean isLeaf) {
        this.fields = fields;
        this.isLeaf = isLeaf;
        defaultValue = defaultValueForType(tClass);
    }

    @NotNull
    public static <T> WireMarshaller<T> of(@NotNull Class<T> tClass) {
        if (tClass.isInterface() || tClass.isEnum())
            return new WireMarshaller<>(tClass, NO_FIELDS, true);

        @NotNull Map<String, Field> map = new LinkedHashMap<>();
        getAllField(tClass, map);
        final FieldAccess[] fields = map.values().stream()
                .map(FieldAccess::create)
                .toArray(FieldAccess[]::new);
        boolean isLeaf = Stream.of(fields).noneMatch(
                c -> (isCollection(c.field.getType()) && !Boolean.TRUE.equals(c.isLeaf))
                        || WriteMarshallable.class.isAssignableFrom(c.field.getType()));
        return overridesUnexpectedFields(tClass)
                ? new WireMarshallerForUnexpectedFields<>(tClass, fields, isLeaf)
                : new WireMarshaller<>(tClass, fields, isLeaf);
    }

    private static <T> boolean overridesUnexpectedFields(Class<T> tClass) {
        try {
            Method method = tClass.getMethod("unexpectedField", UNEXPECTED_FIELDS_PARAMETER_TYPES);
            return method.getDeclaringClass() != ReadMarshallable.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @NotNull
    private static <T> WireMarshaller<T> ofThrowable(@NotNull Class<T> tClass) {
        @NotNull Map<String, Field> map = new LinkedHashMap<>();
        getAllField(tClass, map);
        final FieldAccess[] fields = map.values().stream()
                .map(FieldAccess::create).toArray(FieldAccess[]::new);
        boolean isLeaf = false;
        return new WireMarshaller<>(tClass, fields, isLeaf);
    }

    private static boolean isCollection(@NotNull Class<?> c) {
        return c.isArray() ||
                Collection.class.isAssignableFrom(c) ||
                Map.class.isAssignableFrom(c);
    }

    public static void getAllField(@NotNull Class clazz, @NotNull Map<String, Field> map) {
        if (clazz != Object.class && clazz != AbstractMarshallable.class)
            getAllField(clazz.getSuperclass(), map);
        for (@NotNull Field field : clazz.getDeclaredFields()) {
            if ((field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) != 0)
                continue;
            String name = field.getName();
            if (name.equals("this$0")) {
                Jvm.warn().on(WireMarshaller.class, "Found this$0, in " + clazz + " which will be ignored!");
                continue;
            }
            field.setAccessible(true);
            map.put(name, field);
        }
    }

    private static <T> T defaultValueForType(final @NotNull Class<T> tClass) {
        return ObjectUtils.isConcreteClass(tClass)
                && !tClass.getName().startsWith("java")
                && !tClass.isEnum()
                && !tClass.isArray()
                ? ObjectUtils.newInstance(tClass) :
                null;
    }

    public void writeMarshallable(T t, @NotNull WireOut out) {
        BytesComment bytes = out.bytesComment();
        bytes.indent(+1);
        try {
            for (@NotNull FieldAccess field : fields) {
                bytes.comment(field.field.getName());
                field.write(t, out);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        bytes.indent(-1);
    }

    public void writeMarshallable(T t, Bytes bytes) {
        for (@NotNull FieldAccess field : fields) {
            try {
                field.getAsBytes(t, bytes);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    public void writeMarshallable(T t, @NotNull WireOut out, T previous, boolean copy) {
        try {
            for (@NotNull FieldAccess field : fields) {
                field.write(t, out, previous, copy);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void readMarshallable(T t, @NotNull WireIn in, T defaults, boolean overwrite) {
        try {
            for (@NotNull FieldAccess field : fields) {
                ValueIn vin = in.read(field.key);
                field.readValue(t, defaults, vin, overwrite);
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
        for (@NotNull FieldAccess field : fields) {
            if (!field.isEqual(o1, o2))
                return false;
        }
        return true;
    }

    public Object getField(Object o, String name) throws NoSuchFieldException {
        try {
            // TODO use a more optimal data structure
            for (@NotNull FieldAccess field : fields) {
                if (field.field.getName().equals(name)) {
                    return field.field.get(o);
                }
            }
            throw new NoSuchFieldException(name);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void setField(Object o, String name, Object value) throws NoSuchFieldException {
        try {
            // TODO use a more optimal data structure
            for (@NotNull FieldAccess field : fields) {
                @NotNull final Field field2 = field.field;
                if (field2.getName().equals(name)) {
                    value = ObjectUtils.convertTo(field2.getType(), value);
                    field2.set(o, value);
                    return;
                }
            }
            throw new NoSuchFieldException(name);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    @Nullable
    public T defaultValue() {
        return defaultValue;
    }

    public void reset(T o) {
        try {
            for (FieldAccess field : fields) {
                field.copy(defaultValue, o);
            }

        } catch (IllegalAccessException e) {
            // should never happen as the types should match.
            throw new AssertionError(e);
        }
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    static abstract class FieldAccess {
        @NotNull
        final Field field;
        final long offset;
        @NotNull
        final WireKey key;
        Boolean isLeaf;

        FieldAccess(@NotNull Field field) {
            this(field, null);
        }

        FieldAccess(@NotNull Field field, Boolean isLeaf) {
            this.field = field;
            offset = UNSAFE.objectFieldOffset(field);
            key = field::getName;
            this.isLeaf = isLeaf;
        }

        @Nullable
        public static Object create(@NotNull Field field) {
            Class<?> type = field.getType();
            if (type.isArray())
                return new ArrayFieldAccess(field);
            if (EnumSet.class.isAssignableFrom(type)) {
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    @NotNull ParameterizedType pType = (ParameterizedType) genericType;
                    Type type0 = pType.getActualTypeArguments()[0];
                    final Class componentType = extractClass(type0);
                    boolean isLeaf = !Throwable.class.isAssignableFrom(componentType)
                            && WIRE_MARSHALLER_CL.get(componentType).isLeaf;
                    try {
                        final Method method = Class.class.getDeclaredMethod("enumConstantDirectory");
                        Jvm.setAccessible(method);
                        final Map<String, ? extends Enum> values = (Map<String, ? extends Enum>) method.invoke(componentType);
                        return new EnumSetFieldAccess(field, isLeaf, values.values().toArray(), componentType);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
                throw new RuntimeException("Could not get enum constant directory");
            }
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
                case "int": {
                    IntConversion intConversion = field.getAnnotation(IntConversion.class);
                    return intConversion == null
                            ? new IntegerFieldAccess(field)
                            : new IntConversionFieldAccess(field, intConversion);
                }
                case "float":
                    return new FloatFieldAccess(field);
                case "long": {
                    LongConversion longConversion = field.getAnnotation(LongConversion.class);
                    return longConversion == null
                            ? new LongFieldAccess(field)
                            : new LongConversionFieldAccess(field, longConversion);
                }
                case "double":
                    return new DoubleFieldAccess(field);
                case "java.lang.String":
                    return new StringFieldAccess(field);
                case "java.lang.StringBuilder":
                    return new StringBuilderFieldAccess(field);
                case "net.openhft.chronicle.bytes.Bytes":
                    return new BytesFieldAccess(field);
                default:
                    @Nullable Boolean isLeaf = null;
                    if (WireMarshaller.class.isAssignableFrom(type))
                        isLeaf = WIRE_MARSHALLER_CL.get(type).isLeaf;
                    else if (isCollection(type))
                        isLeaf = false;
                    return new ObjectFieldAccess(field, isLeaf);
            }
        }

        @NotNull
        static Class extractClass(Type type0) {
            if (type0 instanceof Class)
                return (Class) type0;
            else if (type0 instanceof ParameterizedType)
                return (Class) ((ParameterizedType) type0).getRawType();
            else
                return Object.class;
        }

        @NotNull
        @Override
        public String toString() {
            return "FieldAccess{" +
                    "field=" + field +
                    ", isLeaf=" + isLeaf +
                    '}';
        }

        void write(Object o, @NotNull WireOut out) throws IllegalAccessException {
            ValueOut write = out.write(field.getName());
            getValue(o, write, null);
        }

        void write(Object o, @NotNull WireOut out, Object previous, boolean copy) throws IllegalAccessException {
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

        protected void readValue(Object o, Object defaults, ValueIn read, boolean overwrite) throws IllegalAccessException {
            if (read instanceof DefaultValueIn) {
                if (overwrite) copy(defaults, o);
            } else {
                setValue(o, read, overwrite);
            }
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

    static class ObjectFieldAccess extends FieldAccess {
        private final Class type;

        ObjectFieldAccess(@NotNull Field field, Boolean isLeaf) {
            super(field, isLeaf);
            type = field.getType();
        }

        @Override
        protected void getValue(@NotNull Object o, @NotNull ValueOut write, Object previous)
                throws IllegalAccessException {
            Boolean wasLeaf = null;
            if (isLeaf != null)
                wasLeaf = write.swapLeaf(isLeaf);
            assert o != null;
            write.object(type, field.get(o));
            if (wasLeaf != null)
                write.swapLeaf(wasLeaf);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) throws IllegalAccessException {
            long pos = read.wireIn().bytes().readPosition();
            try {
                @Nullable Object using = ObjectUtils.isImmutable(type) == ObjectUtils.Immutability.NO ? field.get(o) : null;
                Object object = read.object(using, type);
                field.set(o, object);

            } catch (Exception e) {
                read.wireIn().bytes().readPosition(pos);
                Object object = read.object();
                Jvm.warn().on(getClass(), "Unable to parse field: " + field.getName() + ", as a marshallable as it is " + object);
                if (overwrite)
                    field.set(o, ObjectUtils.defaultValue(field.getType()));
            }
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) throws IllegalAccessException {
            bytes.writeUtf8(String.valueOf(field.get(o)));
        }
    }

    static class StringFieldAccess extends FieldAccess {
        StringFieldAccess(@NotNull Field field) {
            super(field, false);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) {
            write.text((String) UNSAFE.getObject(o, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            UNSAFE.putObject(o, offset, read.text());
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) {
            bytes.writeUtf8((String) UNSAFE.getObject(o, offset));
        }

        @Override
        protected void copy(Object from, Object to) throws IllegalAccessException {
            super.copy(from, to);
        }
    }

    static class StringBuilderFieldAccess extends FieldAccess {

        public StringBuilderFieldAccess(@NotNull Field field) {
            super(field, true);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) {
            @NotNull CharSequence cs = (CharSequence) UNSAFE.getObject(o, offset);
            write.text(cs);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            @NotNull StringBuilder sb = (StringBuilder) UNSAFE.getObject(o, offset);
            if (sb == null)
                UNSAFE.putObject(o, offset, sb = new StringBuilder());
            if (read.textTo(sb) == null)
                UNSAFE.putObject(o, offset, null);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) {
            bytes.writeUtf8((CharSequence) UNSAFE.getObject(o, offset));
        }

        @Override
        protected boolean sameValue(Object o1, Object o2) throws IllegalAccessException {
            return StringUtils.isEqual((StringBuilder) field.get(o1), (StringBuilder) field.get(o2));
        }

        @Override
        protected void copy(Object from, Object to) {
            final StringBuilder fromSequence = (StringBuilder) UNSAFE.getObject(from, offset);
            StringBuilder toSequence = (StringBuilder) UNSAFE.getObject(to, offset);

            if (fromSequence == null) {
                UNSAFE.putObject(to, offset, null);
                return;
            } else if (toSequence == null) {
                UNSAFE.putObject(to, offset, toSequence = new StringBuilder());
            }

            toSequence.setLength(0);
            toSequence.append(fromSequence);
        }
    }

    static class BytesFieldAccess extends FieldAccess {
        BytesFieldAccess(@NotNull Field field) {
            super(field, false);
        }

        @Override
        protected void getValue(@NotNull Object o, @NotNull ValueOut write, Object previous)
                throws IllegalAccessException {
            Bytes bytesField = (Bytes) field.get(o);
            write.bytes(bytesField);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            @NotNull Bytes bytes = (Bytes) UNSAFE.getObject(o, offset);
            if (bytes == null)
                UNSAFE.putObject(o, offset, bytes = Bytes.elasticHeapByteBuffer(128));
            WireIn wireIn = read.wireIn();
            if (wireIn instanceof TextWire) {
                wireIn.consumePadding();
                if (wireIn.bytes().startsWith(Bytes.from("!!binary"))) {
                    decodeBytes(read, bytes);
                    return;
                }
            }
            if (read.textTo(bytes) == null)
                UNSAFE.putObject(o, offset, null);
        }

        private void decodeBytes(@NotNull ValueIn read, Bytes bytes) {
            @NotNull StringBuilder sb0 = acquireStringBuilder();
            read.text(sb0);
            String s = WireInternal.INTERNER.intern(sb0);
            byte[] decode = Base64.getDecoder().decode(s);
            bytes.clear();
            bytes.write(decode);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) throws IllegalAccessException {
            Bytes bytesField = (Bytes) field.get(o);
            bytes.write(bytesField);
        }

        @Override
        protected void copy(Object from, Object to) {
            Bytes fromBytes = (Bytes) UNSAFE.getObject(from, offset);
            Bytes toBytes = (Bytes) UNSAFE.getObject(to, offset);
            if (fromBytes == null) {
                UNSAFE.putObject(to, offset, null);
                return;

            } else if (toBytes == null) {
                UNSAFE.putObject(to, offset, toBytes = Bytes.elasticByteBuffer());
            }
            toBytes.clear();
            toBytes.write(fromBytes);
        }
    }

    static class ArrayFieldAccess extends FieldAccess {
        private final Class componentType;
        private final Class objectType;

        ArrayFieldAccess(@NotNull Field field) {
            super(field);
            componentType = field.getType().getComponentType();
            objectType = ObjectUtils.primToWrapper(componentType);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) throws IllegalAccessException {
            Object arr = field.get(o);
            boolean leaf = write.swapLeaf(true);
            if (arr == null)
                write.nu11();
            else
                write.sequence(arr, (array, out) -> {
                    for (int i = 0, len = Array.getLength(array); i < len; i++)
                        out.object(objectType, Array.get(array, i));
                });
            write.swapLeaf(leaf);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) throws IllegalAccessException {
            final Object arr = field.get(o);
            if (read.isNull()) {
                if (arr != null)
                    field.set(o, null);
                return;
            }
            @NotNull List list = new ArrayList();
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

    static class EnumSetFieldAccess extends FieldAccess {
        private final Object[] values;
        private final BiConsumer<Object, ValueOut> sequenceGetter;
        private final Class componentType;
        private final Supplier<EnumSet> enumSetSupplier;
        private BiConsumer<EnumSet, ValueIn> addAll;

        EnumSetFieldAccess(@NotNull final Field field, final Boolean isLeaf, final Object[] values, final Class componentType) {
            super(field, isLeaf);
            this.values = values;
            this.componentType = componentType;
            this.enumSetSupplier = () -> EnumSet.noneOf(this.componentType);
            this.sequenceGetter = (o, out) -> sequenceGetter(o,
                    out, this.values, this.field, this.componentType);
            this.addAll = this::addAll;
        }

        private static void sequenceGetter(Object o,
                                           ValueOut out,
                                           Object[] values,
                                           Field field,
                                           Class componentType) {
            EnumSet coll;
            try {
                coll = (EnumSet) field.get(o);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }

            for (int i = values.length - 1; i != -1; i--) {
                if (coll.contains(values[i])) {
                    out.object(componentType, values[i]);
                }
            }
        }

        @Override
        protected void getValue(final Object o, final ValueOut write, final Object previous) throws IllegalAccessException {
            @NotNull Collection c = (Collection) field.get(o);
            if (c == null) {
                write.nu11();
                return;
            }
            write.sequence(o, sequenceGetter);
        }

        protected void readValue(Object o, Object defaults, ValueIn read, boolean overwrite) throws IllegalAccessException {
            EnumSet coll = (EnumSet) field.get(o);
            if (coll == null) {
                coll = enumSetSupplier.get();
                field.set(o, coll);
            }

            if (!read.sequence(coll, addAll)) {
                Collection defaultColl = (Collection) field.get(defaults);
                if (defaultColl == null) {
                    field.set(o, null);
                } else {
                    coll.clear();
                    if (!defaultColl.isEmpty())
                        coll.addAll(defaultColl);
                }
            }
        }

        @Override
        protected void copy(final Object from, final Object to) throws IllegalAccessException {
            EnumSet fromColl = (EnumSet) field.get(from);
            if (fromColl == null) {
                field.set(to, null);
                return;
            }
            EnumSet coll = (EnumSet) field.get(to);
            if (coll == null) {
                coll = enumSetSupplier.get();
                field.set(to, coll);
            }
            coll.clear();
            for (int i = this.values.length - 1; i != -1; i--) {
                if (fromColl.contains(this.values[i])) {
                    coll.add(this.values[i]);
                }
            }
        }

        @Override
        protected void setValue(final Object o, final ValueIn read, final boolean overwrite) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getAsBytes(final Object o, final Bytes bytes) {
            throw new UnsupportedOperationException();
        }

        private void addAll(EnumSet c, ValueIn in2) {
            if (!c.isEmpty())
                c.clear();
            while (in2.hasNextSequenceItem()) {
                c.add(in2.asEnum(componentType));
            }
        }
    }

    static class CollectionFieldAccess extends FieldAccess {
        @NotNull
        final Supplier<Collection> collectionSupplier;
        private final Class componentType;
        private final Class<?> type;
        private BiConsumer<Object, ValueOut> sequenceGetter;

        public CollectionFieldAccess(@NotNull Field field, Boolean isLeaf, @Nullable Supplier<Collection> collectionSupplier, Class componentType, Class<?> type) {
            super(field, isLeaf);
            this.collectionSupplier = collectionSupplier == null ? newInstance() : collectionSupplier;
            this.componentType = componentType;
            this.type = type;
            sequenceGetter = (o, out) -> {
                Collection coll;
                try {
                    coll = (Collection) field.get(o);
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
                if (coll instanceof RandomAccess) {
                    @NotNull List list = (List) coll;
                    //noinspection ForLoopReplaceableByForEach
                    for (int i = 0, len = list.size(); i < len; i++) {
                        out.object(componentType, list.get(i));
                    }
                } else if (coll == null) {
                    try {
                        field.set(coll, null);
                    } catch (IllegalAccessException e) {
                        throw new AssertionError(e);
                    }
                } else {
                    for (Object element : coll) {
                        out.object(componentType, element);
                    }
                }
            };
        }

        @NotNull
        static FieldAccess of(@NotNull Field field) {
            @Nullable final Supplier<Collection> collectionSupplier;
            @NotNull final Class componentType;
            final Class<?> type;
            @Nullable Boolean isLeaf = null;
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
                @NotNull ParameterizedType pType = (ParameterizedType) genericType;
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
                } catch (@NotNull InstantiationException | IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            };
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) throws IllegalAccessException {
            @NotNull Collection c = (Collection) field.get(o);
            if (c == null) {
                write.nu11();
                return;
            }
            write.sequence(o, sequenceGetter);
        }

        protected void copy(Object from, Object to) throws IllegalAccessException {
            Collection fromColl = (Collection) field.get(from);
            if (fromColl == null) {
                field.set(to, null);
                return;
            }
            Collection coll = (Collection) field.get(to);
            if (coll == null) {
                coll = collectionSupplier.get();
                field.set(to, coll);
            }
            coll.clear();
            coll.addAll(fromColl);
        }

        @Override
        protected void readValue(Object o, Object defaults, ValueIn read, boolean overwrite) throws IllegalAccessException {
            Collection coll = (Collection) field.get(o);
            if (coll == null) {
                coll = collectionSupplier.get();
                field.set(o, coll);
            }
            if (!read.sequence(coll, (c, in2) -> {
                if (!c.isEmpty())
                    c.clear();
                while (in2.hasNextSequenceItem())
                    c.add(in2.object(componentType));
            })) {
                Collection defaultColl = (Collection) field.get(defaults);
                if (defaultColl == null) {
                    field.set(o, null);
                } else {
                    coll.clear();
                    if (!defaultColl.isEmpty())
                        coll.addAll(defaultColl);
                }
            }
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) {
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
        @NotNull
        final Supplier<Collection> collectionSupplier;
        private final Class<?> type;
        @NotNull
        private BiConsumer<Collection, ValueIn> seqConsumer = (c, in2) -> {
            Bytes<?> bytes = in2.wireIn().bytes();
            while (in2.hasNextSequenceItem()) {
                long start = bytes.readPosition();
                c.add(in2.text());
                long end = bytes.readPosition();
                if (start == end) {
                    int ch = bytes.readUnsignedByte(start);
                    throw new IORuntimeException("Expected a ] but found " + ch + " '" + (char) ch + "'");
                }
            }
        };

        public StringCollectionFieldAccess(@NotNull Field field, Boolean isLeaf, @Nullable Supplier<Collection> collectionSupplier, Class<?> type) {
            super(field, isLeaf);
            this.collectionSupplier = collectionSupplier == null ? newInstance() : collectionSupplier;
            this.type = type;
        }

        private Supplier<Collection> newInstance() {
            return () -> {
                try {
                    return (Collection) type.newInstance();
                } catch (@NotNull InstantiationException | IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            };
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) throws IllegalAccessException {
            @NotNull Collection<String> c = (Collection<String>) field.get(o);
            if (c == null) {
                write.nu11();
                return;
            }
            write.sequence(c, (coll, out) -> {
                if (coll instanceof RandomAccess) {
                    @NotNull List<String> list = (List<String>) coll;
                    //noinspection ForLoopReplaceableByForEach
                    for (int i = 0, len = list.size(); i < len; i++)
                        out.text(list.get(i));

                } else {
                    for (String element : coll)
                        out.text(element);
                }
            });
        }

        @Override
        protected void readValue(Object o, Object defaults, ValueIn read, boolean overwrite) throws IllegalAccessException {
            Collection coll = (Collection) field.get(o);
            if (coll == null) {
                coll = collectionSupplier.get();
                field.set(o, coll);
            } else if (!coll.isEmpty()) {
                coll.clear();
            }
            boolean sequenced = read.sequence(coll, seqConsumer);
            if (overwrite & !sequenced) {
                field.set(o, null);
            }
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) {
            throw new UnsupportedOperationException();
        }
    }

    static class MapFieldAccess extends FieldAccess {
        @NotNull
        final Supplier<Map> collectionSupplier;
        private final Class<?> type;
        @NotNull
        private final Class keyType;
        @NotNull
        private final Class valueType;

        MapFieldAccess(@NotNull Field field) {
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
                @NotNull ParameterizedType pType = (ParameterizedType) genericType;
                Type[] actualTypeArguments = pType.getActualTypeArguments();
                keyType = extractClass(actualTypeArguments[0]);
                valueType = extractClass(actualTypeArguments[1]);

            } else {
                keyType = Object.class;
                valueType = Object.class;
            }
        }

        @NotNull
        private Supplier<Map> newInstance() {
            return () -> {
                try {
                    return (Map) type.newInstance();
                } catch (@NotNull InstantiationException | IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            };
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) throws IllegalAccessException {
            @NotNull Map map = (Map) field.get(o);
            write.marshallable(map, keyType, valueType, Boolean.TRUE.equals(isLeaf));
        }

        protected void copy(Object from, Object to) throws IllegalAccessException {
            Map fromMap = (Map) field.get(from);
            if (fromMap == null) {
                field.set(to, null);
                return;
            }

            Map map = (Map) field.get(to);
            if (map == null) {
                map = collectionSupplier.get();
                field.set(to, map);
            } else if (!map.isEmpty()) {
                map.clear();
            }
            map.clear();
            map.putAll(fromMap);
        }

        @Override
        protected void readValue(Object o, Object defaults, ValueIn read, boolean overwrite) throws IllegalAccessException {
            Map map = (Map) field.get(o);
            if (map == null) {
                map = collectionSupplier.get();
                field.set(o, map);
            } else if (!map.isEmpty()) {
                map.clear();
            }
            if (read.marshallableAsMap(keyType, valueType, map) == null)
                field.set(o, null);
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getAsBytes(Object o, Bytes bytes) {
            throw new UnsupportedOperationException();
        }
    }

    static class BooleanFieldAccess extends FieldAccess {
        BooleanFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) {
            write.bool(UNSAFE.getBoolean(o, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            UNSAFE.putBoolean(o, offset, read.bool());
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) {
            bytes.writeBoolean(UNSAFE.getBoolean(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return UNSAFE.getBoolean(o, offset) == UNSAFE.getBoolean(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            UNSAFE.putBoolean(to, offset, UNSAFE.getBoolean(from, offset));
        }
    }

    static class ByteFieldAccess extends FieldAccess {
        ByteFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) {
            write.int8(UNSAFE.getByte(o, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            UNSAFE.putByte(o, offset, read.int8());
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) {
            bytes.writeByte(UNSAFE.getByte(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return UNSAFE.getByte(o, offset) == UNSAFE.getByte(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            UNSAFE.putByte(to, offset, UNSAFE.getByte(from, offset));
        }
    }

    static class ShortFieldAccess extends FieldAccess {
        ShortFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) {
            write.int16(UNSAFE.getShort(o, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            UNSAFE.putShort(o, offset, read.int16());
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) {
            bytes.writeShort(UNSAFE.getShort(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return UNSAFE.getShort(o, offset) == UNSAFE.getShort(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            UNSAFE.putShort(to, offset, UNSAFE.getShort(from, offset));
        }
    }

    static class CharFieldAccess extends FieldAccess {
        CharFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) {
            @NotNull StringBuilder sb = acquireStringBuilder();
            sb.append(UNSAFE.getChar(o, offset));
            write.text(sb);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            String text = read.text();
            if (text == null || text.length() < 1) {
                if (overwrite)
                    text = "\0";
                else
                    return;
            }
            UNSAFE.putChar(o, offset, text.charAt(0));
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) {
            bytes.writeUnsignedShort(UNSAFE.getChar(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return UNSAFE.getChar(o, offset) == UNSAFE.getChar(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            UNSAFE.putChar(to, offset, UNSAFE.getChar(from, offset));
        }
    }

    static class IntegerFieldAccess extends FieldAccess {
        IntegerFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, @Nullable Object previous) {
            if (previous == null)
                write.int32(UNSAFE.getInt(o, offset));
            else
                write.int32(UNSAFE.getInt(o, offset), UNSAFE.getInt(previous, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            int i = overwrite ? read.int32() : read.int32(UNSAFE.getInt(o, offset));
            UNSAFE.putInt(o, offset, i);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) {
            bytes.writeInt(UNSAFE.getInt(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return UNSAFE.getInt(o, offset) == UNSAFE.getInt(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            UNSAFE.putInt(to, offset, UNSAFE.getInt(from, offset));
        }
    }

    static class IntConversionFieldAccess extends FieldAccess {
        @NotNull
        private final IntConverter intConverter;

        IntConversionFieldAccess(@NotNull Field field, @NotNull IntConversion intConversion) {
            super(field);
            this.intConverter = ObjectUtils.newInstance(intConversion.value());
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, @Nullable Object previous) {
            StringBuilder sb = acquireStringBuilder();
            intConverter.append(sb, UNSAFE.getInt(o, offset));
            write.text(sb);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            StringBuilder sb = acquireStringBuilder();
            read.text(sb);
            int i = intConverter.parse(sb);
            UNSAFE.putInt(o, offset, i);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) {
            StringBuilder sb = acquireStringBuilder();
            bytes.readUtf8(sb);
            int i = intConverter.parse(sb);
            bytes.writeInt(i);
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return UNSAFE.getInt(o, offset) == UNSAFE.getInt(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            UNSAFE.putInt(to, offset, UNSAFE.getInt(from, offset));
        }
    }

    static class FloatFieldAccess extends FieldAccess {
        FloatFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, @Nullable Object previous) {
            if (previous == null)
                write.float32(UNSAFE.getFloat(o, offset));
            else
                write.float32(UNSAFE.getFloat(o, offset), UNSAFE.getFloat(previous, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            final float v = overwrite ? read.float32() : read.float32(UNSAFE.getFloat(o, offset));
            UNSAFE.putFloat(o, offset, v);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) {
            bytes.writeFloat(UNSAFE.getFloat(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return Maths.same(UNSAFE.getFloat(o, offset), UNSAFE.getFloat(o2, offset));
        }

        @Override
        protected void copy(Object from, Object to) {
            UNSAFE.putFloat(to, offset, UNSAFE.getFloat(from, offset));
        }
    }

    static class LongFieldAccess extends FieldAccess {
        LongFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, @Nullable Object previous) {
            if (previous == null)
                write.int64(UNSAFE.getLong(o, offset));
            else
                write.int64(UNSAFE.getLong(o, offset), UNSAFE.getLong(previous, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            long i = overwrite ? read.int64() : read.int64(UNSAFE.getLong(o, offset));
            UNSAFE.putLong(o, offset, i);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) {
            bytes.writeLong(UNSAFE.getLong(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return UNSAFE.getLong(o, offset) == UNSAFE.getLong(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            UNSAFE.putLong(to, offset, UNSAFE.getLong(from, offset));
        }
    }

    static class LongConversionFieldAccess extends FieldAccess {
        @NotNull
        private final LongConverter longConverter;

        LongConversionFieldAccess(@NotNull Field field, @NotNull LongConversion longConversion) {
            super(field);
            this.longConverter = ObjectUtils.newInstance(longConversion.value());
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, @Nullable Object previous) {
            StringBuilder sb = acquireStringBuilder();
            longConverter.append(sb, UNSAFE.getLong(o, offset));
            write.text(sb);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            StringBuilder sb = acquireStringBuilder();
            read.text(sb);
            long i = longConverter.parse(sb);
            UNSAFE.putLong(o, offset, i);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) {
            StringBuilder sb = acquireStringBuilder();
            bytes.readUtf8(sb);
            long i = longConverter.parse(sb);
            bytes.writeLong(i);
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return UNSAFE.getLong(o, offset) == UNSAFE.getLong(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            UNSAFE.putLong(to, offset, UNSAFE.getLong(from, offset));
        }
    }

    static class DoubleFieldAccess extends FieldAccess {
        DoubleFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, @Nullable Object previous) {
            if (previous == null)
                write.float64(UNSAFE.getDouble(o, offset));
            else
                write.float64(UNSAFE.getDouble(o, offset), UNSAFE.getDouble(previous, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            final double v = overwrite ? read.float64() : read.float64(UNSAFE.getDouble(o, offset));
            UNSAFE.putDouble(o, offset, v);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes bytes) {
            bytes.writeDouble(UNSAFE.getDouble(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return Maths.same(UNSAFE.getDouble(o, offset), UNSAFE.getDouble(o2, offset));
        }

        @Override
        protected void copy(Object from, Object to) {
            UNSAFE.putDouble(to, offset, UNSAFE.getDouble(from, offset));
        }
    }
}
