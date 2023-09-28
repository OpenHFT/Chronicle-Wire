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
import net.openhft.chronicle.bytes.HexDumpBytesDescription;
import net.openhft.chronicle.core.*;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.openhft.chronicle.core.UnsafeMemory.*;

@SuppressWarnings({"restriction", "rawtypes", "unchecked"})
public class WireMarshaller<T> {
    static final StringBuilderPool SBP = new StringBuilderPool();
    private static final Class[] UNEXPECTED_FIELDS_PARAMETER_TYPES = {Object.class, ValueIn.class};
    private static final FieldAccess[] NO_FIELDS = {};
    private static final StringBuilderPool RSBP = new StringBuilderPool();
    private static final StringBuilderPool WSBP = new StringBuilderPool();
    @NotNull
    final FieldAccess[] fields;
    final TreeMap<CharSequence, FieldAccess> fieldMap = new TreeMap<>(WireMarshaller::compare);
    private final boolean isLeaf;
    @Nullable
    private final T defaultValue;

    protected WireMarshaller(@NotNull Class<T> tClass, @NotNull FieldAccess[] fields, boolean isLeaf) {
        this(fields, isLeaf, defaultValueForType(tClass));
    }

    public static final ClassLocal<WireMarshaller> WIRE_MARSHALLER_CL = ClassLocal.withInitial
            (tClass ->
                    Throwable.class.isAssignableFrom(tClass)
                            ? WireMarshaller.ofThrowable(tClass)
                            : WireMarshaller.of(tClass)
            );

    private WireMarshaller(@NotNull FieldAccess[] fields, boolean isLeaf, @Nullable T defaultValue) {
        this.fields = fields;
        this.isLeaf = isLeaf;
        this.defaultValue = defaultValue;
        for (FieldAccess field : fields) {
            fieldMap.put(field.key.name(), field);
        }
    }

    @NotNull
    public static <T> WireMarshaller<T> of(@NotNull Class<T> tClass) {
        if (tClass.isInterface() || (tClass.isEnum() && !DynamicEnum.class.isAssignableFrom(tClass)))
            return new WireMarshaller<>(tClass, NO_FIELDS, true);

        @NotNull Map<String, Field> map = new LinkedHashMap<>();
        getAllField(tClass, map);
        final FieldAccess[] fields = map.values().stream()
                // for Java 15+ strip "hidden" fields that can't be accessed in Java 15+ this way.
                .filter(field -> !(Jvm.isJava15Plus() && field.getName().matches("^.*\\$\\d+$")))
                .map(FieldAccess::create)
                .toArray(FieldAccess[]::new);
        Map<String, Long> fieldCount = Stream.of(fields).collect(Collectors.groupingBy(f -> f.field.getName(), Collectors.counting()));
        fieldCount.forEach((n, c) -> {
            if (c > 1) Jvm.warn().on(tClass, "Has " + c + " fields called '" + n + "'");
        });
        List<FieldAccess> collect = Stream.of(fields)
                .filter(WireMarshaller::leafable)
                .collect(Collectors.toList());
        boolean isLeaf = collect.isEmpty();
        return overridesUnexpectedFields(tClass)
                ? new WireMarshallerForUnexpectedFields<>(tClass, fields, isLeaf)
                : new WireMarshaller<>(tClass, fields, isLeaf);
    }

    protected static boolean leafable(FieldAccess c) {
        Class<?> type = c.field.getType();
        if (isCollection(type))
            return !Boolean.TRUE.equals(c.isLeaf);
        if (DynamicEnum.class.isAssignableFrom(type))
            return false;
        return WriteMarshallable.class.isAssignableFrom(type);
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
        if (clazz != Object.class && clazz != AbstractCommonMarshallable.class)
            getAllField(clazz.getSuperclass(), map);
        for (@NotNull Field field : clazz.getDeclaredFields()) {
            if ((field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) != 0)
                continue;
            if ("ordinal".equals(field.getName()) && Enum.class.isAssignableFrom(clazz))
                continue;
            String name = field.getName();
            if (name.startsWith("this$0")) {
                if (ValidatableUtil.validateEnabled())
                    Jvm.warn().on(WireMarshaller.class, "Found " + name + ", in " + clazz + " which will be ignored!");
                continue;
            }
            Jvm.setAccessible(field);
            map.put(name, field);
        }
    }

    private static <T> T defaultValueForType(@NotNull Class<T> tClass) {
//        tClass = ObjectUtils.implementationToUse(tClass);
        if (ObjectUtils.isConcreteClass(tClass)
                && !tClass.getName().startsWith("java")
                && !tClass.isEnum()
                && !tClass.isArray()) {
            T t = ObjectUtils.newInstance(tClass);
            IOTools.unmonitor(t);
            return t;
        }
        if (DynamicEnum.class.isAssignableFrom(tClass)) {
            try {
                T t = OS.memory().allocateInstance(tClass);
                Jvm.getField(Enum.class, "name").set(t, "[unset]");
                Jvm.getField(Enum.class, "ordinal").set(t, -1);
                IOTools.unmonitor(t);
                return t;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
        return null;
    }

    private static int compare(CharSequence cs0, CharSequence cs1) {
        for (int i = 0, len = Math.min(cs0.length(), cs1.length()); i < len; i++) {
            int cmp = Character.compare(cs0.charAt(i), cs1.charAt(i));
            if (cmp != 0)
                return cmp;
        }
        return Integer.compare(cs0.length(), cs1.length());
    }

    private static Type[] computeActualTypeArguments(Class iface, Field field) {
        Type[] actual = consumeActualTypeArguments(new HashMap<>(), iface, field.getGenericType());

        if (actual == null)
            return iface.getTypeParameters();

        return actual;
    }

    private static Type[] consumeActualTypeArguments(Map<String, Type> prevTypeParameters, Class iface, Type type) {
        Class cls = null;
        Map<String, Type> typeParameters = new HashMap<>();
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] typeArguments = pType.getActualTypeArguments();

            cls = ((Class) ((ParameterizedType) type).getRawType());
            TypeVariable<?>[] typeParamDecls = cls.getTypeParameters();

            for (int i = 0; i < Math.min(typeParamDecls.length, typeArguments.length); i++) {
                Type value;
                if (typeArguments[i] instanceof TypeVariable) {
                    value = prevTypeParameters.get(((TypeVariable<?>) typeArguments[i]).getName());

                    if (value == null) {
                        // Fail-safe.
                        Type[] bounds = ((TypeVariable<?>) typeArguments[i]).getBounds();
                        value = bounds.length == 0 ? Object.class : bounds[0];
                    }
                } else {
                    value = typeArguments[i];
                }

                typeParameters.put(typeParamDecls[i].getName(), value);
            }
        } else if (type instanceof Class) {
            cls = (Class) type;
        }

        if (iface.equals(cls)) {
            TypeVariable[] parameters = iface.getTypeParameters();
            Type[] result = new Type[parameters.length];

            for (int i = 0; i < result.length; i++) {
                Type parameter = typeParameters.get(parameters[i].getName());

                result[i] = parameter != null ? parameter : parameters[i];
            }

            return result;
        }

        if (cls != null) {
            for (Type ifaceType : cls.getGenericInterfaces()) {
                Type[] result = consumeActualTypeArguments(typeParameters, iface, ifaceType);

                if (result != null)
                    return result;
            }

            return consumeActualTypeArguments(typeParameters, iface, cls.getGenericSuperclass());
        }

        return null;
    }

    public WireMarshaller<T> excludeFields(String... fieldNames) {
        Set<String> fieldSet = new HashSet<>(Arrays.asList(fieldNames));
        return new WireMarshaller(Stream.of(fields)
                .filter(f -> !fieldSet.contains(f.field.getName()))
                .toArray(FieldAccess[]::new),
                isLeaf, defaultValue);
    }

    public void writeMarshallable(T t, @NotNull WireOut out) throws InvalidMarshallableException {
        ValidatableUtil.validate(t);
        HexDumpBytesDescription bytes = out.bytesComment();
        bytes.adjustHexDumpIndentation(+1);
        try {
            for (@NotNull FieldAccess field : fields)
                field.write(t, out);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        bytes.adjustHexDumpIndentation(-1);
    }

    public void writeMarshallable(T t, Bytes<?> bytes) {
        for (@NotNull FieldAccess field : fields) {
            try {
                field.getAsBytes(t, bytes);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    /**
     * Writes the values of the fields from the provided object (DTO) to the output. Before writing,
     * the object is validated. The method also supports optional copying of the values
     * from the source object to a previous instance.
     *
     * @param t        Object whose field values are to be written.
     * @param out      Output destination where the field values are written to.
     * @param previous Previous object to compare for optional copying.
     * @param copy     Flag indicating whether to copy values from the source object to the previous object.
     * @throws InvalidMarshallableException If there's an error during marshalling.
     */
    public void writeMarshallable(T t, @NotNull WireOut out, T previous, boolean copy) throws InvalidMarshallableException {
        // Validate the object before writing
        ValidatableUtil.validate(t);
        try {
            // Iterate through all fields and write their values to the output
            for (@NotNull FieldAccess field : fields) {
                field.write(t, out, previous, copy);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Reads and populates the DTO based on the provided input. The input order can be hinted.
     * After reading, the object is validated.
     *
     * @param t         Object to populate with read values.
     * @param in        Input source from which values are read.
     * @param defaults  Default values to use if a value isn't provided in the input.
     * @param overwrite Flag indicating whether to overwrite the existing value in the target object.
     * @throws InvalidMarshallableException If there is an error during marshalling.
     */
    public void readMarshallable(T t, @NotNull WireIn in, T defaults, boolean overwrite) throws InvalidMarshallableException {
        // Choose the reading method based on the hint
        if (in.hintReadInputOrder())
            readMarshallableInputOrder(t, in, defaults, overwrite);
        else
            readMarshallableDTOOrder(t, in, defaults, overwrite);

        // Validate the object after reading
        ValidatableUtil.validate(t);
    }

    /**
     * Reads and populates the DTO based on the provided order.
     *
     * @param t         Target object to populate with read values.
     * @param in        Input source from which values are read.
     * @param defaults  Default values to use if a value isn't provided in the input.
     * @param overwrite Flag indicating whether to overwrite the existing value in the target object.
     * @throws InvalidMarshallableException If there is an error during marshalling.
     */
    public void readMarshallableDTOOrder(T t, @NotNull WireIn in, T defaults, boolean overwrite) throws InvalidMarshallableException {
        try {
            for (@NotNull FieldAccess field : fields) {
                ValueIn vin = in.read(field.key);
                field.readValue(t, defaults, vin, overwrite);
            }
            ValidatableUtil.validate(t);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Reads and populates the DTO based on the input's order.
     *
     * @param t         Target object to populate with read values.
     * @param in        Input source from which values are read.
     * @param defaults  Default values to use if a value isn't provided in the input.
     * @param overwrite Flag indicating whether to overwrite the existing value in the target object.
     * @throws InvalidMarshallableException If there is an error during marshalling.
     */
    public void readMarshallableInputOrder(T t, @NotNull WireIn in, T defaults, boolean overwrite) throws InvalidMarshallableException {
        try {
            StringBuilder sb = SBP.acquireStringBuilder();

            // Iterating over all fields to read their values
            for (int i = 0; i < fields.length; i++) {
                boolean more = in.hasMore();
                FieldAccess field = fields[i];

                ValueIn vin = more ? in.read(sb) : null;

                // Check if fields are present and in order
                if (more && matchesFieldName(sb, field)) {
                    field.readValue(t, defaults, in.getValueIn(), overwrite);

                } else {
                    // If not, copy default values
                    for (; i < fields.length; i++) {
                        FieldAccess field2 = fields[i];
                        field2.copy(defaults, t);
                    }

                    if (vin == null || sb.length() <= 0)
                        return;

                    // Read the next set of values if there are any left
                    do {
                        FieldAccess fieldAccess = fieldMap.get(sb);
                        if (fieldAccess == null)
                            vin.skipValue();
                        else
                            fieldAccess.readValue(t, defaults, vin, overwrite);

                        vin = in.read(sb);
                    } while (in.hasMore());
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public boolean matchesFieldName(StringBuilder sb, FieldAccess field) {
        return sb.length() == 0 || StringUtils.equalsCaseIgnore(field.field.getName(), sb);
    }

    public void writeKey(T t, Bytes<?> bytes) {
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
            FieldAccess field = fieldMap.get(name);
            if (field == null)
                throw new NoSuchFieldException(name);

            return field.field.get(o);

        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public long getLongField(@NotNull Object o, String name) throws NoSuchFieldException {
        try {
            FieldAccess field = fieldMap.get(name);
            if (field == null)
                throw new NoSuchFieldException(name);

            Field field2 = field.field;
            return field2.getType() == long.class
                    ? field2.getLong(o)
                    : field2.getType() == int.class
                    ? field2.getInt(o)
                    : ObjectUtils.convertTo(Long.class, field2.get(o));

        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void setField(Object o, String name, Object value) throws NoSuchFieldException {
        try {
            FieldAccess field = fieldMap.get(name);
            if (field == null)
                throw new NoSuchFieldException(name);
            @NotNull final Field field2 = field.field;
            value = ObjectUtils.convertTo(field2.getType(), value);
            field2.set(o, value);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void setLongField(Object o, String name, long value) throws NoSuchFieldException {
        try {
            FieldAccess field = fieldMap.get(name);
            if (field == null)
                throw new NoSuchFieldException(name);
            @NotNull final Field field2 = field.field;
            if (field2.getType() == long.class)
                field2.setLong(o, value);
            else if (field2.getType() == int.class)
                field2.setInt(o, (int) value);
            else
                field2.set(o, ObjectUtils.convertTo(field2.getType(), value));
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

    /**
     * Provides a field accessor that's specialized for handling fields which require
     * conversion between integer values and string representations using a LongConverter.
     */
    static class LongConverterFieldAccess extends FieldAccess {

        // The LongConverter instance used for conversion operations.
        @NotNull
        private final LongConverter longConverter;

        /**
         * Constructor to initialize field access with a specific LongConverter.
         *
         * @param field         The field being accessed.
         * @param longConverter The converter to use for this field.
         */
        LongConverterFieldAccess(@NotNull Field field, @NotNull LongConverter longConverter) {
            super(field);
            this.longConverter = longConverter;
        }

        /**
         * Fetches the LongConverter instance associated with a given class.
         * Tries to retrieve a static "INSTANCE" field or creates a new instance if not found.
         *
         * @param clazz The class which presumably has a LongConverter.
         * @return The LongConverter instance.
         */
        static LongConverter getInstance(Class clazz) {
            try {
                Field converterField = clazz.getDeclaredField("INSTANCE");
                return (LongConverter) converterField.get(null);
            } catch (NoSuchFieldException nsfe) {
                return (LongConverter) ObjectUtils.newInstance(clazz);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Reads the long value from an object and writes it using the provided ValueOut writer.
         *
         * @param o        The source object.
         * @param write    The writer for output.
         * @param previous The previous value (currently not used).
         */
        @Override
        protected void getValue(Object o, @NotNull ValueOut write, @Nullable Object previous) {
            long aLong = getLong(o);
            if (write.isBinary()) {
                write.int64(aLong);
            } else {
                StringBuilder sb = WSBP.acquireStringBuilder();
                longConverter.append(sb, aLong);
                if (!write.isBinary() && sb.length() == 0)
                    write.text("");
                else
                    write.rawText(sb);
            }
        }

        /**
         * Retrieves the long value from an object.
         *
         * @param o The object from which to retrieve the value.
         * @return The long value of the field.
         */
        protected long getLong(Object o) {
            return unsafeGetLong(o, offset);
        }

        /**
         * Sets the value of the object's field based on the provided ValueIn reader.
         *
         * @param o         The target object.
         * @param read      The reader for input.
         * @param overwrite Whether to overwrite existing values (currently not used).
         */
        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            long i;
            if (read.isBinary()) {
                i = read.int64();
            } else {
                StringBuilder sb = RSBP.acquireStringBuilder();
                read.text(sb);
                i = longConverter.parse(sb);
                if (!rangeCheck(i))
                    throw new IORuntimeException("value '" + sb + "' is out of range for a " + field.getType());
            }
            setLong(o, i);
        }

        /**
         * Checks if the provided long value is within acceptable ranges.
         *
         * @param i The long value to check.
         * @return True if the value is within range; otherwise, false.
         */
        protected boolean rangeCheck(long i) {
            return true;
        }

        /**
         * Sets a long value to the field of an object.
         *
         * @param o The target object.
         * @param i The value to set.
         */
        protected void setLong(Object o, long i) {
            unsafePutLong(o, offset, i);
        }

        /**
         * Reads a string value from the object and writes its long representation to the provided bytes.
         *
         * @param o     The source object.
         * @param bytes The bytes to write the long representation to.
         */
        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            StringBuilder sb = WSBP.acquireStringBuilder();
            bytes.readUtf8(sb);
            long i = longConverter.parse(sb);
            bytes.writeLong(i);
        }

        /**
         * Checks if two objects have the same long value for the accessed field.
         *
         * @param o  First object.
         * @param o2 Second object.
         * @return True if values are the same; otherwise, false.
         */
        @Override
        protected boolean sameValue(Object o, Object o2) {
            return getLong(o) == getLong(o2);
        }

        /**
         * Copies the long value of the accessed field from one object to another.
         *
         * @param from Source object.
         * @param to   Destination object.
         */
        @Override
        protected void copy(Object from, Object to) {
            setLong(to, getLong(from));
        }
    }

    /**
     * Abstract class to manage access to fields of objects.
     * This class provides utility methods to read and write fields from/to objects.
     */
    abstract static class FieldAccess {
        @NotNull
        final Field field;
        final long offset;
        @NotNull
        final WireKey key;

        Comment commentAnnotation;
        Boolean isLeaf;

        /**
         * Constructor initializing field with given value.
         *
         * @param field Field to be accessed.
         */
        FieldAccess(@NotNull Field field) {
            this(field, null);
        }

        /**
         * Constructor initializing field and isLeaf with given values.
         *
         * @param field  Field to be accessed.
         * @param isLeaf Flag to indicate whether the field is a leaf node.
         */
        FieldAccess(@NotNull Field field, Boolean isLeaf) {
            this.field = field;

            offset = unsafeObjectFieldOffset(field);
            key = field::getName;
            this.isLeaf = isLeaf;
            try {
                commentAnnotation = Jvm.findAnnotation(field, Comment.class);
            } catch (NullPointerException ignore) {

            }
        }

        // ... (code continues)

        /**
         * Create a specific FieldAccess object based on the field type.
         *
         * @param field Field for which FieldAccess object is created.
         * @return FieldAccess object specific to the field type.
         */
        @Nullable
        public static Object create(@NotNull Field field) {
            Class<?> type = field.getType();
            if (type.isArray()) {
                if (type.getComponentType() == byte.class)
                    return new ByteArrayFieldAccess(field);
                return new ArrayFieldAccess(field);
            }
            if (EnumSet.class.isAssignableFrom(type)) {
                final Class componentType = extractClass(computeActualTypeArguments(EnumSet.class, field)[0]);
                if (componentType == Object.class || Modifier.isAbstract(componentType.getModifiers()))
                    throw new RuntimeException("Could not get enum constant directory");

                boolean isLeaf = !Throwable.class.isAssignableFrom(componentType)
                        && WIRE_MARSHALLER_CL.get(componentType).isLeaf;
                try {
                    Object[] values = (Object[]) Jvm.getMethod(componentType, "values").invoke(componentType, null);
                    return new EnumSetFieldAccess(field, isLeaf, values, componentType);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw Jvm.rethrow(e);
                }
            }
            if (Collection.class.isAssignableFrom(type))
                return CollectionFieldAccess.of(field);
            if (Map.class.isAssignableFrom(type))
                return new MapFieldAccess(field);

            switch (type.getName()) {
                case "boolean":
                    return new BooleanFieldAccess(field);
                case "byte": {
                    LongConverter longConverter = acquireLongConverter(field);
                    if (longConverter != null)
                        return new ByteLongConverterFieldAccess(field, longConverter);
                    IntConversion intConversion = Jvm.findAnnotation(field, IntConversion.class);
                    return intConversion == null
                            ? new ByteFieldAccess(field)
                            : new ByteIntConversionFieldAccess(field, intConversion);
                }
                case "char": {
                    LongConverter longConverter = acquireLongConverter(field);
                    if (longConverter != null)
                        return new CharLongConverterFieldAccess(field, longConverter);
                    CharConversion charConversion = Jvm.findAnnotation(field, CharConversion.class);
                    return charConversion == null
                            ? new CharFieldAccess(field)
                            : new CharConversionFieldAccess(field, charConversion);
                }
                case "short": {
                    LongConverter longConverter = acquireLongConverter(field);
                    if (longConverter != null)
                        return new ShortLongConverterFieldAccess(field, longConverter);
                    IntConversion intConversion = Jvm.findAnnotation(field, IntConversion.class);
                    return intConversion == null
                            ? new ShortFieldAccess(field)
                            : new ShortIntConversionFieldAccess(field, intConversion);
                }
                case "int": {
                    LongConverter longConverter = acquireLongConverter(field);
                    if (longConverter != null)
                        return new IntLongConverterFieldAccess(field, longConverter);
                    IntConversion intConversion = Jvm.findAnnotation(field, IntConversion.class);
                    return intConversion == null
                            ? new IntegerFieldAccess(field)
                            : new IntConversionFieldAccess(field, intConversion);
                }
                case "float":
                    return new FloatFieldAccess(field);
                case "long": {
                    LongConverter longConverter = acquireLongConverter(field);

                    return longConverter == null
                            ? new LongFieldAccess(field)
                            : new LongConverterFieldAccess(field, longConverter);
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
                    if (IntValue.class.isAssignableFrom(type))
                        return new IntValueAccess(field);
                    if (LongValue.class.isAssignableFrom(type))
                        return new LongValueAccess(field);
                    if (WireMarshaller.class.isAssignableFrom(type))
                        isLeaf = WIRE_MARSHALLER_CL.get(type).isLeaf;
                    else if (isCollection(type))
                        isLeaf = false;
                    return new ObjectFieldAccess(field, isLeaf);
            }
        }

        @Nullable
        private static LongConverter acquireLongConverter(@NotNull Field field) {
            LongConversion longConversion = Jvm.findAnnotation(field, LongConversion.class);
            LongConverter longConverter = null;
            if (longConversion != null)
                longConverter = LongConverterFieldAccess.getInstance(longConversion.value());
            return longConverter;
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

        void write(Object o, @NotNull WireOut out) throws IllegalAccessException, InvalidMarshallableException {

            ValueOut valueOut = out.write(field.getName());

            if (valueOut instanceof CommentAnnotationNotifier && this.commentAnnotation != null) {
                getValueCommentAnnotated(o, out, valueOut);
                return;
            }

            getValue(o, valueOut, null);

        }

        private void getValueCommentAnnotated(Object o, @NotNull WireOut out, ValueOut valueOut) throws IllegalAccessException, InvalidMarshallableException {
            CommentAnnotationNotifier notifier = (CommentAnnotationNotifier) valueOut;
            notifier.hasPrecedingComment(true);
            try {
                getValue(o, valueOut, null);
                out.writeComment(String.format(this.commentAnnotation.value(), field.get(o)));
            } finally {
                notifier.hasPrecedingComment(false);
            }
        }

        /**
         * Writes the value of the field from the provided object to the output. If the value is the same
         * as the previous value, it skips the writing. If the copy flag is set, it also copies the value
         * from the source object to the previous object.
         *
         * @param o        Object from which the field value is fetched.
         * @param out      Output destination where the value is written to.
         * @param previous Previous object to compare for sameness and optionally copy to.
         * @param copy     Flag indicating whether to copy the value from source to the previous object.
         * @throws IllegalAccessException       If there's an access violation when fetching the field value.
         * @throws InvalidMarshallableException If there's an error during marshalling.
         */
        void write(Object o, @NotNull WireOut out, Object previous, boolean copy) throws IllegalAccessException, InvalidMarshallableException {
            // Check if the current and previous values are the same
            if (sameValue(o, previous))
                return;

            // Write the field's value to the output
            ValueOut write = out.write(field.getName());
            getValue(o, write, previous);

            // Copy value from source object to previous object, if required
            if (copy)
                copy(o, previous);
        }

        /**
         * Check if the values of a field in two objects are the same.
         *
         * @param o  First object.
         * @param o2 Second object.
         * @return true if values are the same, false otherwise.
         * @throws IllegalAccessException If unable to access the field.
         */
        protected boolean sameValue(Object o, Object o2) throws IllegalAccessException {
            final Object v1 = field.get(o);
            final Object v2 = field.get(o2);
            if (v1 instanceof CharSequence && v2 instanceof CharSequence)
                return StringUtils.isEqual((CharSequence) v1, (CharSequence) v2);
            return Objects.equals(v1, v2);
        }

        /**
         * Copies the value of a field from one object to another.
         *
         * @param from Source object.
         * @param to   Destination object.
         * @throws IllegalAccessException If unable to access the field.
         */
        protected void copy(Object from, Object to) throws IllegalAccessException {
            ObjectUtils.requireNonNull(from);
            ObjectUtils.requireNonNull(to);

            unsafePutObject(to, offset, unsafeGetObject(from, offset));
        }

        /**
         * Abstract method to get the value of a field from an object.
         *
         * @param o        Object from which to get the value.
         * @param write    Output destination.
         * @param previous Previous object for comparison.
         * @throws IllegalAccessException       If unable to access the field.
         * @throws InvalidMarshallableException If marshalling fails.
         */
        protected abstract void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException, InvalidMarshallableException;

        /**
         * Reads the value of a field from an input and sets it in an object.
         *
         * @param o         Object to set the value in.
         * @param defaults  Default values.
         * @param read      Input source.
         * @param overwrite Whether to overwrite existing value.
         * @throws IllegalAccessException       If unable to access the field.
         * @throws InvalidMarshallableException If marshalling fails.
         */
        protected void readValue(Object o, Object defaults, ValueIn read, boolean overwrite) throws IllegalAccessException, InvalidMarshallableException {
            if (!read.isPresent()) {
                if (overwrite && defaults != null)
                    copy(Objects.requireNonNull(defaults), o);
            } else {
                long pos = read.wireIn().bytes().readPosition();
                try {
                    setValue(o, read, overwrite);
                } catch (UnexpectedFieldHandlingException | ClassCastException | ClassNotFoundRuntimeException e) {
                    Jvm.rethrow(e);
                } catch (Exception e) {
                    read.wireIn().bytes().readPosition(pos);
                    StringBuilder sb = RSBP.acquireStringBuilder();
                    read.text(sb);
                    Jvm.warn().on(getClass(), "Failed to read '" + this.field.getName() + "' with '" + sb + "' taking default", e);
                    copy(defaults, o);
                }
            }
        }

        /**
         * Abstract method to set the value of a field in an object.
         *
         * @param o         Object to set the value in.
         * @param read      Input source.
         * @param overwrite Whether to overwrite existing value.
         * @throws IllegalAccessException If unable to access the field.
         */
        protected abstract void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException;

        /**
         * Abstract method to convert the value of a field in an object to bytes.
         *
         * @param o     Object containing the field.
         * @param bytes Destination to write the bytes to.
         * @throws IllegalAccessException If unable to access the field.
         */
        public abstract void getAsBytes(Object o, Bytes<?> bytes) throws IllegalAccessException;

        /**
         * Checks whether the values of a field in two objects are equal.
         *
         * @param o1 First object.
         * @param o2 Second object.
         * @return true if the values are equal, false otherwise.
         */
        public boolean isEqual(Object o1, Object o2) {
            try {
                return sameValue(o1, o2);
            } catch (IllegalAccessException e) {
                return false;
            }
        }
    }

    static class IntValueAccess extends FieldAccess {
        IntValueAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            IntValue f = (IntValue) field.get(o);
            int value = f == null ? 0 : f.getValue();
            write.int32forBinding(value);
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            IntValue f = (IntValue) field.get(o);
            if (f == null) {
                f = read.wireIn().newIntReference();
                field.set(o, f);
            }
            read.int32(f);
        }

        @Override
        public void getAsBytes(Object o, Bytes<?> bytes) {
            throw new UnsupportedOperationException();
        }
    }

    static class LongValueAccess extends FieldAccess {
        LongValueAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, ValueOut write, Object previous) throws IllegalAccessException {
            LongValue f = (LongValue) field.get(o);
            long value = f == null ? 0 : f.getValue();
            write.int64forBinding(value);
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) throws IllegalAccessException {
            LongValue f = (LongValue) field.get(o);
            if (f == null) {
                f = read.wireIn().newLongReference();
                field.set(o, f);
            }
            read.int64(f);
        }

        @Override
        public void getAsBytes(Object o, Bytes<?> bytes) {
            throw new UnsupportedOperationException();
        }
    }

    static class ObjectFieldAccess extends FieldAccess {
        private final Class type;
        private final AsMarshallable asMarshallable;

        ObjectFieldAccess(@NotNull Field field, Boolean isLeaf) {
            super(field, isLeaf);
            asMarshallable = Jvm.findAnnotation(field, AsMarshallable.class);
            type = field.getType();
        }

        @Override
        protected void getValue(@NotNull Object o, @NotNull ValueOut write, Object previous)
                throws IllegalAccessException, InvalidMarshallableException {
            Boolean wasLeaf = null;
            if (isLeaf != null)
                wasLeaf = write.swapLeaf(isLeaf);
            assert o != null;
            Object v = field.get(o);
            if (asMarshallable == null || !(v instanceof WriteMarshallable))
                write.object(type, v);
            else
                write.typedMarshallable((WriteMarshallable) v);
            if (wasLeaf != null)
                write.swapLeaf(wasLeaf);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) throws IllegalAccessException {
            long pos = read.wireIn().bytes().readPosition();
            try {
                @Nullable Object using = ObjectUtils.isImmutable(type) == ObjectUtils.Immutability.NO ? field.get(o) : null;

                Object object = null;
                try {
                    object = read.object(using, type, false);
                } catch (Exception e) {
                    // "Unhandled" Abstract classes that are not types should be null (Enums are abstract classes in Java but should not be null here)
                    if (using == null &&
                            Modifier.isAbstract(type.getModifiers()) &&
                            !Modifier.isInterface(type.getModifiers()) &&
                            !type.isEnum() &&
                            !read.isTyped()) {
                        // retain the null value of object
                        Jvm.warn().on(getClass(), "Ignoring exception and setting field '" + field.getName() + "' to null", e);
                    } else {
                        Jvm.rethrow(e);
                    }
                }

                if (object instanceof SingleThreadedChecked)
                    ((SingleThreadedChecked) object).singleThreadedCheckReset();
                field.set(o, object);

            } catch (UnexpectedFieldHandlingException | ClassCastException | ClassNotFoundRuntimeException e) {
                Jvm.rethrow(e);
            } catch (Exception e) {
                read.wireIn().bytes().readPosition(pos);
                Jvm.warn().on(getClass(), "Unable to parse field: " + field.getName() + ", as a marshallable as it is " + read.objectBestEffort(), e);
                if (overwrite)
                    field.set(o, ObjectUtils.defaultValue(field.getType()));
            }
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) throws IllegalAccessException {
            bytes.writeUtf8(String.valueOf(field.get(o)));
        }
    }

    static class StringFieldAccess extends FieldAccess {
        StringFieldAccess(@NotNull Field field) {
            super(field, false);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) {
            write.text(UnsafeMemory.<String>unsafeGetObject(o, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            unsafePutObject(o, offset, read.text());
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            bytes.writeUtf8((String) unsafeGetObject(o, offset));
        }

    }

    static class StringBuilderFieldAccess extends FieldAccess {

        public StringBuilderFieldAccess(@NotNull Field field) {
            super(field, true);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) {
            @NotNull CharSequence cs = unsafeGetObject(o, offset);
            write.text(cs);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            @NotNull StringBuilder sb = unsafeGetObject(o, offset);
            if (sb == null) {
                sb = new StringBuilder();
                unsafePutObject(o, offset, sb);
            }
            if (read.textTo(sb) == null)
                unsafePutObject(o, offset, null);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            bytes.writeUtf8((CharSequence) unsafeGetObject(o, offset));
        }

        @Override
        protected boolean sameValue(Object o1, Object o2) throws IllegalAccessException {
            return StringUtils.isEqual((StringBuilder) field.get(o1), (StringBuilder) field.get(o2));
        }

        @Override
        protected void copy(Object from, Object to) {
            final StringBuilder fromSequence = unsafeGetObject(from, offset);
            StringBuilder toSequence = unsafeGetObject(to, offset);

            if (fromSequence == null) {
                unsafePutObject(to, offset, null);
                return;
            } else if (toSequence == null) {
                toSequence = new StringBuilder();
                unsafePutObject(to, offset, toSequence);
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
            Bytes<?> bytesField = (Bytes) field.get(o);
            write.bytes(bytesField);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            @NotNull Bytes<?> bytes = (Bytes) unsafeGetObject(o, offset);
            if (bytes == null)
                unsafePutObject(o, offset, bytes = Bytes.allocateElasticOnHeap(128));
            WireIn wireIn = read.wireIn();
            if (wireIn instanceof TextWire) {
                wireIn.consumePadding();
                if (wireIn.bytes().startsWith(TextWire.BINARY)) {
                    decodeBytes(read, bytes);
                    return;
                }
            }
            if (read.textTo(bytes) == null)
                unsafePutObject(o, offset, null);
            else
                bytes.singleThreadedCheckReset();
        }

        private void decodeBytes(@NotNull ValueIn read, Bytes<?> bytes) {
            @NotNull StringBuilder sb0 = RSBP.acquireStringBuilder();
            read.text(sb0);
            String s = WireInternal.INTERNER.intern(sb0);
            byte[] decode = Base64.getDecoder().decode(s);
            bytes.clear();
            bytes.write(decode);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) throws IllegalAccessException {
            Bytes<?> bytesField = (Bytes) field.get(o);
            bytes.write(bytesField);
        }

        @Override
        protected void copy(Object from, Object to) {
            Bytes<?> fromBytes = (Bytes) unsafeGetObject(from, offset);
            Bytes<?> toBytes = (Bytes) unsafeGetObject(to, offset);
            if (fromBytes == null) {
                unsafePutObject(to, offset, null);
                return;

            } else if (toBytes == null) {
                toBytes = Bytes.elasticByteBuffer();
                unsafePutObject(to, offset, toBytes);
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
            objectType = ObjectUtils.implementationToUse(
                    ObjectUtils.primToWrapper(componentType));
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
        public void getAsBytes(Object o, Bytes<?> bytes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEqual(Object o1, Object o2) {
            try {
                Object a1 = field.get(o1);
                Object a2 = field.get(o2);
                if (a1 == null) return a2 == null;
                if (a2 == null) return false;
                Class<?> aClass1 = a1.getClass();
                Class<?> aClass2 = a2.getClass();
                if (aClass1 != aClass2)
                    if (!aClass1.isAssignableFrom(aClass2) && !aClass2.isAssignableFrom(aClass1))
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

    static class ByteArrayFieldAccess extends FieldAccess {

        ByteArrayFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) throws IllegalAccessException {
            Object arr = field.get(o);
            boolean leaf = write.swapLeaf(true);
            if (arr == null)
                write.nu11();
            else
                write.bytes((byte[]) arr);
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
            byte[] arr2 = read.bytes((byte[]) arr);
            if (arr2 != arr)
                field.set(o, arr2);
        }

        @Override
        public void getAsBytes(Object o, Bytes<?> bytes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEqual(Object o1, Object o2) {
            try {
                Object a1 = field.get(o1);
                Object a2 = field.get(o2);
                if (a1 == null) return a2 == null;
                if (a2 == null) return false;
                Class<?> aClass1 = a1.getClass();
                Class<?> aClass2 = a2.getClass();
                if (aClass1 != aClass2)
                    if (!aClass1.isAssignableFrom(aClass2) && !aClass2.isAssignableFrom(aClass1))
                        return false;
                return Arrays.equals((byte[]) a1, (byte[]) a2);
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
                                           Class componentType)
                throws InvalidMarshallableException {
            final EnumSet coll;
            try {
                coll = (EnumSet) field.get(o);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }

            for (Object v : values) {
                if (coll.contains(v)) {
                    out.object(componentType, v);
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

        @Override
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
        public void getAsBytes(final Object o, final Bytes<?> bytes) {
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

            componentType = extractClass(computeActualTypeArguments(Collection.class, field)[0]);
            if (componentType != Object.class) {
                isLeaf = !Throwable.class.isAssignableFrom(componentType)
                        && WIRE_MARSHALLER_CL.get(componentType).isLeaf;
            }

            return componentType == String.class
                    ? new StringCollectionFieldAccess(field, true, collectionSupplier, type)
                    : new CollectionFieldAccess(field, isLeaf, collectionSupplier, componentType, type);
        }

        private Supplier<Collection> newInstance() {
            return () -> (Collection) ObjectUtils.newInstance(type);
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

        @Override
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
        public void getAsBytes(Object o, Bytes<?> bytes) {
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
            return () -> (Collection) ObjectUtils.newInstance(type);
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
            } else if (!coll.isEmpty()) {
                coll.clear();
            }
            boolean sequenced = read.sequence(coll, seqConsumer);
            if (overwrite && !sequenced) {
                field.set(o, null);
            }
        }

        @Override
        protected void setValue(Object o, ValueIn read, boolean overwrite) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getAsBytes(Object o, Bytes<?> bytes) {
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

            Type[] actualTypeArguments = computeActualTypeArguments(Map.class, field);
            keyType = extractClass(actualTypeArguments[0]);
            valueType = extractClass(actualTypeArguments[1]);
        }

        @NotNull
        private Supplier<Map> newInstance() {
            return () -> (Map) ObjectUtils.newInstance(type);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) throws IllegalAccessException, InvalidMarshallableException {
            @NotNull Map map = (Map) field.get(o);
            write.marshallable(map, keyType, valueType, Boolean.TRUE.equals(isLeaf));
        }

        @Override
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
        protected void readValue(Object o, Object defaults, ValueIn read, boolean overwrite) throws IllegalAccessException, InvalidMarshallableException {
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
        public void getAsBytes(Object o, Bytes<?> bytes) {
            throw new UnsupportedOperationException();
        }
    }

    static class BooleanFieldAccess extends FieldAccess {
        BooleanFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) {
            write.bool(unsafeGetBoolean(o, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            unsafePutBoolean(o, offset, read.bool());
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            bytes.writeBoolean(unsafeGetBoolean(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return unsafeGetBoolean(o, offset) == unsafeGetBoolean(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            unsafePutBoolean(to, offset, unsafeGetBoolean(from, offset));
        }
    }

    static class ByteFieldAccess extends FieldAccess {
        ByteFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) {
            write.int8(unsafeGetByte(o, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            unsafePutByte(o, offset, read.int8());
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            bytes.writeByte(unsafeGetByte(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return unsafeGetByte(o, offset) == unsafeGetByte(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            unsafePutByte(to, offset, unsafeGetByte(from, offset));
        }
    }

    static class ShortFieldAccess extends FieldAccess {
        ShortFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) {
            write.int16(unsafeGetShort(o, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            unsafePutShort(o, offset, read.int16());
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            bytes.writeShort(unsafeGetShort(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return unsafeGetShort(o, offset) == unsafeGetShort(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            unsafePutShort(to, offset, unsafeGetShort(from, offset));
        }
    }

    static class CharFieldAccess extends FieldAccess {

        public static final String INVALID_CHAR_STR = String.valueOf((char) 0xFFFF);

        CharFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) {
            char c = unsafeGetChar(o, offset);
            if (c == (char) 0xFFFF) {
                write.nu11();
            } else {
                StringBuilder sb = WSBP.acquireStringBuilder();
                sb.append(c);
                write.text(sb);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            String text = read.text();
            if (text == null || text.length() < 1) {
                if (overwrite)
                    text = INVALID_CHAR_STR;
                else
                    return;
            }
            unsafePutChar(o, offset, text.charAt(0));
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            bytes.writeUnsignedShort(unsafeGetChar(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return unsafeGetChar(o, offset) == unsafeGetChar(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            unsafePutChar(to, offset, unsafeGetChar(from, offset));
        }
    }

    static class IntegerFieldAccess extends FieldAccess {
        IntegerFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, @Nullable Object previous) {
            if (previous == null)
                write.int32(unsafeGetInt(o, offset));
            else
                write.int32(unsafeGetInt(o, offset), unsafeGetInt(previous, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            int i = overwrite ? read.int32() : read.int32(unsafeGetInt(o, offset));
            unsafePutInt(o, offset, i);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            bytes.writeInt(unsafeGetInt(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return unsafeGetInt(o, offset) == unsafeGetInt(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            unsafePutInt(to, offset, unsafeGetInt(from, offset));
        }
    }

    static class ByteIntConversionFieldAccess extends IntConversionFieldAccess {
        public ByteIntConversionFieldAccess(@NotNull Field field, @NotNull IntConversion intConversion) {
            super(field, intConversion);
        }

        @Override
        protected int getInt(Object o) {
            return unsafeGetByte(o, offset) & 0xFF;
        }

        @Override
        protected void putInt(Object o, int i) {
            unsafePutByte(o, offset, (byte) i);
        }
    }

    static class ShortIntConversionFieldAccess extends IntConversionFieldAccess {
        public ShortIntConversionFieldAccess(@NotNull Field field, @NotNull IntConversion intConversion) {
            super(field, intConversion);
        }

        @Override
        protected int getInt(Object o) {
            return unsafeGetShort(o, offset) & 0xFFFF;
        }

        @Override
        protected void putInt(Object o, int i) {
            unsafePutShort(o, offset, (short) i);
        }
    }

    /**
     * A field access that provides a way to interact with a byte field as if it's a long,
     * by using a {@link LongConverter} for any necessary transformations.
     */
    static class ByteLongConverterFieldAccess extends LongConverterFieldAccess {

        /**
         * Constructs a new instance of {@link ByteLongConverterFieldAccess}.
         *
         * @param field         The byte field to be accessed.
         * @param longConverter The converter to be used for the transformations.
         */
        public ByteLongConverterFieldAccess(@NotNull Field field, LongConverter longConverter) {
            super(field, longConverter);
        }

        /**
         * Checks if the given long value fits into an unsigned byte range.
         *
         * @param i The long value to check.
         * @return True if the value is within the byte range, otherwise false.
         */
        @Override
        protected boolean rangeCheck(long i) {
            return (i & 0xFF) == i;
        }

        /**
         * Retrieves the unsigned byte value from the given object and returns it as a long.
         *
         * @param o The object containing the field.
         * @return The long representation of the byte value.
         */
        @Override
        protected long getLong(Object o) {
            return unsafeGetByte(o, offset) & 0xFFL;
        }

        /**
         * Sets the value of the byte field in the given object using a long value.
         *
         * @param o The object containing the field.
         * @param i The long value to set.
         */
        @Override
        protected void setLong(Object o, long i) {
            unsafePutByte(o, offset, (byte) i);
        }
    }

    /**
     * A field access that provides a way to interact with a short field as if it's a long,
     * by using a {@link LongConverter} for any necessary transformations.
     */
    static class ShortLongConverterFieldAccess extends LongConverterFieldAccess {

        /**
         * Constructs a new instance of {@link ShortLongConverterFieldAccess}.
         *
         * @param field         The short field to be accessed.
         * @param longConverter The converter to be used for the transformations.
         */
        public ShortLongConverterFieldAccess(@NotNull Field field, LongConverter longConverter) {
            super(field, longConverter);
        }

        /**
         * Checks if the given long value fits into a short range.
         *
         * @param i The long value to check.
         * @return True if the value is within the short range, otherwise false.
         */
        @Override
        protected boolean rangeCheck(long i) {
            return (i & 0xFFFFL) == i;
        }

        /**
         * Retrieves the short value from the given object and returns it as a long.
         *
         * @param o The object containing the field.
         * @return The long representation of the short value.
         */
        @Override
        protected long getLong(Object o) {
            return unsafeGetShort(o, offset) & 0xFFFFL;
        }

        /**
         * Sets the value of the short field in the given object using a long value.
         *
         * @param o The object containing the field.
         * @param i The long value to set.
         */
        @Override
        protected void setLong(Object o, long i) {
            unsafePutShort(o, offset, (short) i);
        }
    }

    /**
     * A field access that provides a way to interact with a char field as if it's a long,
     * leveraging a {@link LongConverter} for any necessary transformations.
     */
    static class CharLongConverterFieldAccess extends LongConverterFieldAccess {

        /**
         * Constructs a new instance of {@link CharLongConverterFieldAccess}.
         *
         * @param field         The char field to be accessed.
         * @param longConverter The converter used for transformations.
         */
        public CharLongConverterFieldAccess(@NotNull Field field, LongConverter longConverter) {
            super(field, longConverter);
        }

        /**
         * Checks if the given long value can be represented as a char.
         *
         * @param i The long value to check.
         * @return True if the value can be represented as a char, otherwise false.
         */
        @Override
        protected boolean rangeCheck(long i) {
            return (char) i == i;
        }

        /**
         * Retrieves the char value from the given object and returns it as a long.
         *
         * @param o The object containing the field.
         * @return The long representation of the char value.
         */
        @Override
        protected long getLong(Object o) {
            return unsafeGetChar(o, offset);
        }

        /**
         * Sets the value of the char field in the given object using a long value.
         *
         * @param o The object containing the field.
         * @param i The long value to set.
         */
        @Override
        protected void setLong(Object o, long i) {
            unsafePutChar(o, offset, (char) i);
        }
    }

    /**
     * A field access that provides a way to interact with an int field as if it's a long,
     * leveraging a {@link LongConverter} for any necessary transformations.
     */
    static class IntLongConverterFieldAccess extends LongConverterFieldAccess {

        /**
         * Constructs a new instance of {@link IntLongConverterFieldAccess}.
         *
         * @param field         The int field to be accessed.
         * @param longConverter The converter used for transformations.
         */
        public IntLongConverterFieldAccess(@NotNull Field field, @NotNull LongConverter longConverter) {
            super(field, longConverter);
        }

        /**
         * Checks if the given long value can be represented as an int.
         *
         * @param i The long value to check.
         * @return True if the value can be represented as an int, otherwise false.
         */
        @Override
        protected boolean rangeCheck(long i) {
            return (i & 0xFFFF_FFFFL) == i;
        }

        /**
         * Retrieves the int value from the given object and returns it as a long.
         *
         * @param o The object containing the field.
         * @return The long representation of the int value.
         */
        @Override
        protected long getLong(Object o) {
            return unsafeGetInt(o, offset) & 0xFFFF_FFFFL;
        }

        /**
         * Sets the value of the int field in the given object using a long value.
         *
         * @param o The object containing the field.
         * @param i The long value to set.
         */
        @Override
        protected void setLong(Object o, long i) {
            unsafePutInt(o, offset, (int) i);
        }
    }

    static class CharConversionFieldAccess extends CharFieldAccess {

        @NotNull
        private final CharConverter intConverter;

        CharConversionFieldAccess(@NotNull Field field, @NotNull CharConversion charConversion) {
            super(field);
            this.intConverter = ObjectUtils.newInstance(charConversion.value());
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, @Nullable Object previous) {
            StringBuilder sb = WSBP.acquireStringBuilder();
            intConverter.append(sb, getChar(o));
            if (!write.isBinary() && sb.length() == 0)
                write.text("");
            else
                write.rawText(sb);
        }

        protected char getChar(Object o) {
            return unsafeGetChar(o, offset);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            StringBuilder sb = RSBP.acquireStringBuilder();
            read.text(sb);
            char i = intConverter.parse(sb);
            putChar(o, i);
        }

        protected void putChar(Object o, char i) {
            unsafePutChar(o, offset, i);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            StringBuilder sb = WSBP.acquireStringBuilder();
            bytes.readUtf8(sb);
            int i = intConverter.parse(sb);
            bytes.writeInt(i);
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return getChar(o) == getChar(o2);
        }

        @Override
        protected void copy(Object from, Object to) {
            putChar(to, getChar(from));
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
            int anInt = getInt(o);
            if (write.isBinary()) {
                write.int32(anInt);
            } else {
                StringBuilder sb = WSBP.acquireStringBuilder();
                intConverter.append(sb, anInt);
                if (!write.isBinary() && sb.length() == 0)
                    write.text("");
                else
                    write.rawText(sb);
            }
        }

        protected int getInt(Object o) {
            return unsafeGetInt(o, offset);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            int i;
            if (read.isBinary()) {
                i = read.int32();

            } else {
                StringBuilder sb = RSBP.acquireStringBuilder();
                read.text(sb);
                i = intConverter.parse(sb);
            }
            putInt(o, i);
        }

        protected void putInt(Object o, int i) {
            unsafePutInt(o, offset, i);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            StringBuilder sb = WSBP.acquireStringBuilder();
            bytes.readUtf8(sb);
            int i = intConverter.parse(sb);
            bytes.writeInt(i);
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return getInt(o) == getInt(o2);
        }

        @Override
        protected void copy(Object from, Object to) {
            putInt(to, getInt(from));
        }
    }

    static class FloatFieldAccess extends FieldAccess {
        FloatFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, @Nullable Object previous) {
            if (previous == null)
                write.float32(unsafeGetFloat(o, offset));
            else
                write.float32(unsafeGetFloat(o, offset), unsafeGetFloat(previous, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            final float v = overwrite ? read.float32() : read.float32(unsafeGetFloat(o, offset));
            unsafePutFloat(o, offset, v);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            bytes.writeFloat(unsafeGetFloat(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return Maths.same(unsafeGetFloat(o, offset), unsafeGetFloat(o2, offset));
        }

        @Override
        protected void copy(Object from, Object to) {
            unsafePutFloat(to, offset, unsafeGetFloat(from, offset));
        }
    }

    static class LongFieldAccess extends FieldAccess {
        LongFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, @Nullable Object previous) {
            if (previous == null)
                write.int64(unsafeGetLong(o, offset));
            else
                write.int64(unsafeGetLong(o, offset), unsafeGetLong(previous, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            long i = overwrite ? read.int64() : read.int64(unsafeGetLong(o, offset));
            unsafePutLong(o, offset, i);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            bytes.writeLong(unsafeGetLong(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return unsafeGetLong(o, offset) == unsafeGetLong(o2, offset);
        }

        @Override
        protected void copy(Object from, Object to) {
            unsafePutLong(to, offset, unsafeGetLong(from, offset));
        }
    }

    static class DoubleFieldAccess extends FieldAccess {
        DoubleFieldAccess(@NotNull Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, @Nullable Object previous) {
            if (previous == null)
                write.float64(unsafeGetDouble(o, offset));
            else
                write.float64(unsafeGetDouble(o, offset), unsafeGetDouble(previous, offset));
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            final double v = overwrite ? read.float64() : read.float64(unsafeGetDouble(o, offset));
            unsafePutDouble(o, offset, v);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            bytes.writeDouble(unsafeGetDouble(o, offset));
        }

        @Override
        protected boolean sameValue(Object o, Object o2) {
            return Maths.same(unsafeGetDouble(o, offset), unsafeGetDouble(o2, offset));
        }

        @Override
        protected void copy(Object from, Object to) {
            unsafePutDouble(to, offset, unsafeGetDouble(from, offset));
        }
    }


}
