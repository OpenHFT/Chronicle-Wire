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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.util.ClassLocal;
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

/**
 * The WireMarshaller class is responsible for marshalling and unmarshalling of objects of type T.
 * This class provides an efficient mechanism for serialization/deserialization using wire protocols.
 * It utilizes field accessors to read and write values directly to and from the fields of the object.
 *
 * @param <T> The type of the object to be marshalled/unmarshalled.
 */
@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
public class WireMarshaller<T> {
    private static final Class[] UNEXPECTED_FIELDS_PARAMETER_TYPES = {Object.class, ValueIn.class};
    private static final FieldAccess[] NO_FIELDS = {};
    private static Method isRecord;
    @NotNull
    final FieldAccess[] fields;

    // Map for quick field look-up based on their names.
    final TreeMap<CharSequence, FieldAccess> fieldMap = new TreeMap<>(WireMarshaller::compare);

    // Flag to determine if this marshaller is for a leaf class.
    private final boolean isLeaf;

    // Default value for the type T.
    @Nullable
    private final T defaultValue;

    static {
        if (Jvm.isJava14Plus()) {
            try {
                isRecord = Jvm.getMethod(Class.class, "isRecord");
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Constructs a new instance of the WireMarshaller with the specified parameters.
     *
     * @param tClass  The class of the object to be marshalled.
     * @param fields  An array of field accessors that provide access to the fields of the object.
     * @param isLeaf  Indicates if the marshaller is for a leaf class.
     */
    protected WireMarshaller(@NotNull Class<T> tClass, @NotNull FieldAccess[] fields, boolean isLeaf) {
        this(fields, isLeaf, defaultValueForType(tClass));
    }

    // A class-local storage for caching WireMarshallers for different types.
    // Depending on the type of class, it either creates a marshaller for exceptions or a generic one.
    public static final ClassLocal<WireMarshaller> WIRE_MARSHALLER_CL = ClassLocal.withInitial
            (tClass ->
                    Throwable.class.isAssignableFrom(tClass)
                            ? WireMarshaller.ofThrowable(tClass)
                            : WireMarshaller.of(tClass)
            );

    WireMarshaller(@NotNull FieldAccess[] fields, boolean isLeaf, @Nullable T defaultValue) {
        this.fields = fields;
        this.isLeaf = isLeaf;
        this.defaultValue = defaultValue;
        for (FieldAccess field : fields) {
            fieldMap.put(field.key.name(), field);
        }
    }

    /**
     * Factory method to create an instance of the WireMarshaller for a specific class type.
     * Determines the appropriate marshaller type (basic or one that handles unexpected fields)
     * based on the characteristics of the provided class.
     *
     * @param tClass The class type for which the marshaller is to be created.
     * @return A new instance of WireMarshaller for the provided class type.
     */
    @NotNull
    public static <T> WireMarshaller<T> of(@NotNull Class<T> tClass) {
        if (tClass.isInterface() || (tClass.isEnum() && !DynamicEnum.class.isAssignableFrom(tClass)))
            return new WireMarshaller<>(tClass, NO_FIELDS, true);

        T defaultObject = defaultValueForType(tClass);

        @NotNull Map<String, Field> map = new LinkedHashMap<>();
        getAllField(tClass, map);
        final FieldAccess[] fields = map.values().stream()
                // for Java 15+ strip "hidden" fields that can't be accessed in Java 15+ this way.
                .filter(field -> !(Jvm.isJava15Plus() && field.getName().matches("^.*\\$\\d+$")))
                .map(field -> FieldAccess.create(field, defaultObject))
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
                ? new WireMarshallerForUnexpectedFields<>(fields, isLeaf, defaultObject)
                : new WireMarshaller<>(fields, isLeaf, defaultObject);
    }

    /**
     * Checks if the provided field accessor corresponds to a "leaf" entity.
     * An entity is considered a leaf if it doesn't need to be further broken down in the serialization process.
     *
     * @param c The field accessor to be checked.
     * @return {@code true} if the field accessor is leafable, {@code false} otherwise.
     */
    protected static boolean leafable(FieldAccess c) {
        Class<?> type = c.field.getType();
        if (isCollection(type))
            return !Boolean.TRUE.equals(c.isLeaf);
        if (DynamicEnum.class.isAssignableFrom(type))
            return false;
        return WriteMarshallable.class.isAssignableFrom(type);
    }

    /**
     * Determines if the provided class overrides the "unexpectedField" method from the ReadMarshallable interface.
     *
     * @param tClass The class type to be checked.
     * @return {@code true} if the class overrides the "unexpectedField" method, {@code false} otherwise.
     */
    private static <T> boolean overridesUnexpectedFields(Class<T> tClass) {
        try {
            Method method = tClass.getMethod("unexpectedField", UNEXPECTED_FIELDS_PARAMETER_TYPES);
            return method.getDeclaringClass() != ReadMarshallable.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Factory method to create an instance of the WireMarshaller for a Throwable class type.
     * The method identifies fields that should be marshalled and prepares the marshaller accordingly.
     *
     * @param tClass The Throwable class type for which the marshaller is to be created.
     * @return A new instance of WireMarshaller for the provided Throwable class type.
     */
    @NotNull
    private static <T> WireMarshaller<T> ofThrowable(@NotNull Class<T> tClass) {
        T defaultObject = defaultValueForType(tClass);

        @NotNull Map<String, Field> map = new LinkedHashMap<>();
        getAllField(tClass, map);
        final FieldAccess[] fields = map.values().stream()
                .map(field -> FieldAccess.create(field, defaultObject)).toArray(FieldAccess[]::new);
        boolean isLeaf = false;
        return new WireMarshaller<>(fields, isLeaf, defaultObject);
    }

    /**
     * Determines if the provided class is a collection type, including arrays, standard Collections, or Maps.
     *
     * @param c The class to be checked.
     * @return {@code true} if the class is a collection type, {@code false} otherwise.
     */
    private static boolean isCollection(@NotNull Class<?> c) {
        return c.isArray() ||
                Collection.class.isAssignableFrom(c) ||
                Map.class.isAssignableFrom(c);
    }

    /**
     * Recursively fetches all non-static, non-transient fields from the provided class and its superclasses,
     * up to but not including Object or AbstractCommonMarshallable, and adds them to the provided map.
     * Fields that are flagged for exclusion (e.g., the "ordinal" field for Enum types) are skipped.
     *
     * @param clazz The class type from which fields are to be extracted.
     * @param map   The map to populate with field names and their corresponding Field objects.
     */
    public static void getAllField(@NotNull Class<?> clazz, @NotNull Map<String, Field> map) {
        if (clazz != Object.class && clazz != AbstractCommonMarshallable.class)
            getAllField(clazz.getSuperclass(), map);
        for (@NotNull Field field : clazz.getDeclaredFields()) {
            if ((field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) != 0)
                continue;
            String fieldName = field.getName();
            if (("ordinal".equals(fieldName) || "hash".equals(fieldName)) && Enum.class.isAssignableFrom(clazz))
                continue;
            String name = fieldName;
            if (name.startsWith("this$0")) {
                if (ValidatableUtil.validateEnabled())
                    Jvm.warn().on(WireMarshaller.class, "Found " + name + ", in " + clazz + " which will be ignored!");
                continue;
            }
            Jvm.setAccessible(field);
            map.put(name, field);
        }
    }

    static <T> T defaultValueForType(@NotNull Class<T> tClass) {
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

    /**
     * Compares two CharSequences lexicographically. This is a character-by-character comparison
     * that returns the difference of the first unmatched characters or the difference in their lengths
     * if one sequence is a prefix of the other.
     *
     * @param cs0 The first CharSequence to be compared.
     * @param cs1 The second CharSequence to be compared.
     * @return A positive integer if {@code cs0} comes after {@code cs1},
     *         a negative integer if {@code cs0} comes before {@code cs1},
     *         or zero if the sequences are equal.
     */
    private static int compare(CharSequence cs0, CharSequence cs1) {
        for (int i = 0, len = Math.min(cs0.length(), cs1.length()); i < len; i++) {
            int cmp = Character.compare(cs0.charAt(i), cs1.charAt(i));
            if (cmp != 0)
                return cmp;
        }
        return Integer.compare(cs0.length(), cs1.length());
    }

    /**
     * Computes the actual type arguments for a given field of an interface. This method determines
     * the generic type arguments that the field uses based on the interface's type parameters.
     *
     * @param iface The interface containing the field.
     * @param field The field whose type arguments need to be determined.
     * @return An array of actual type arguments or the interface's type parameters if no actual arguments can be deduced.
     */
    private static Type[] computeActualTypeArguments(Class<?> iface, Field field) {
        Type[] actual = consumeActualTypeArguments(new HashMap<>(), iface, field.getGenericType());

        if (actual == null)
            return iface.getTypeParameters();

        return actual;
    }

    /**
     * Determines the actual type arguments used by a class or interface for a given interface type.
     * This method recursively inspects the type hierarchy to match type arguments against the
     * interface's type parameters. It uses a previously built map of type parameter names to their
     * actual types to deduce the correct arguments for the given interface.
     *
     * @param prevTypeParameters A map containing previously discovered type parameter names
     *                           mapped to their actual types.
     * @param iface              The interface for which we want to determine the type arguments.
     * @param type               The type to inspect. This could be an actual class, interface, or
     *                           a parameterized type that uses generic arguments.
     *
     * @return An array of actual type arguments used by the provided type for the specified interface,
     *         or null if the type doesn't directly or indirectly implement or extend the given interface.
     */
    private static Type[] consumeActualTypeArguments(Map<String, Type> prevTypeParameters, Class<?> iface, Type type) {
        Class<?> cls = null;
        Map<String, Type> typeParameters = new HashMap<>();

        // If the type is a ParameterizedType, retrieve its actual type arguments and
        // map them against the declared type parameters.
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] typeArguments = pType.getActualTypeArguments();

            cls = ((Class) ((ParameterizedType) type).getRawType());
            TypeVariable<?>[] typeParamDecls = cls.getTypeParameters();

            for (int i = 0; i < Math.min(typeParamDecls.length, typeArguments.length); i++) {
                Type value;

                // If the actual type argument is another type variable, try to get its actual type
                // from the previously discovered type parameters.
                if (typeArguments[i] instanceof TypeVariable) {
                    value = prevTypeParameters.get(((TypeVariable<?>) typeArguments[i]).getName());

                    // Use a bound type or Object.class if the actual type isn't found.
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
            // If the type is a Class (not a ParameterizedType), directly use it.
            cls = (Class) type;
        }

        // If the provided type (or its raw type in case of a ParameterizedType) matches the target
        // interface, map the discovered type arguments to the interface's type parameters and return them.
        if (iface.equals(cls)) {
            TypeVariable[] parameters = iface.getTypeParameters();
            Type[] result = new Type[parameters.length];

            for (int i = 0; i < result.length; i++) {
                Type parameter = typeParameters.get(parameters[i].getName());

                result[i] = parameter != null ? parameter : parameters[i];
            }

            return result;
        }

        // Recursively inspect the generic interfaces and superclass of the type to discover
        // the actual type arguments for the given interface.
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

    /**
     * Excludes specified fields from the current marshaller and returns a new instance of the marshaller
     * with the remaining fields.
     *
     * @param fieldNames Names of the fields to be excluded.
     * @return A new instance of the {@link WireMarshaller} with the specified fields excluded.
     */
    public WireMarshaller<T> excludeFields(String... fieldNames) {
        Set<String> fieldSet = new HashSet<>(Arrays.asList(fieldNames));
        return new WireMarshaller<>(Stream.of(fields)
                .filter(f -> !fieldSet.contains(f.field.getName()))
                .toArray(FieldAccess[]::new),
                isLeaf, defaultValue);
    }

    /**
     * Writes the marshallable representation of the given object to the provided {@link WireOut} destination.
     * This will traverse the fields and use their respective {@link FieldAccess} to write each field.
     * The method also adjusts the hex dump indentation for better readability in the output.
     *
     * @param t   The object to write.
     * @param out The destination {@link WireOut} where the object representation will be written.
     * @throws InvalidMarshallableException If the object fails validation checks before serialization.
     */
    public void writeMarshallable(T t, @NotNull WireOut out) throws InvalidMarshallableException {
        ValidatableUtil.validate(t);
        HexDumpBytesDescription<?> bytes = out.bytesComment();
        bytes.adjustHexDumpIndentation(+1);
        try {
            for (@NotNull FieldAccess field : fields)
                field.write(t, out);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        bytes.adjustHexDumpIndentation(-1);
    }

    /**
     * Writes the marshallable representation of the given object to the provided {@link Bytes} destination.
     * Unlike the previous method, this doesn't adjust the hex dump indentation. It's a more direct
     * serialization of the fields to bytes.
     *
     * @param t     The object to write.
     * @param bytes The destination {@link Bytes} where the object representation will be written.
     */
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
     * @param t    Object whose field values are to be written.
     * @param out  Output destination where the field values are written to.
     * @param copy Flag indicating whether to copy values from the source object to the previous object.
     * @throws InvalidMarshallableException If there's an error during marshalling.
     */
    public void writeMarshallable(T t, @NotNull WireOut out, boolean copy) throws InvalidMarshallableException {
        // Validate the object before writing
        ValidatableUtil.validate(t);
        try {
            // Iterate through all fields and write their values to the output
            for (@NotNull FieldAccess field : fields) {
                field.write(t, out, defaultValue, copy);
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
     * @param overwrite Flag indicating whether to overwrite the existing value in the target object.
     * @throws InvalidMarshallableException If there is an error during marshalling.
     */
    public void readMarshallable(T t, @NotNull WireIn in, boolean overwrite) throws InvalidMarshallableException {
        // Choose the reading method based on the hint
        if (in.hintReadInputOrder())
            readMarshallableInputOrder(t, in, overwrite);
        else
            readMarshallableDTOOrder(t, in, overwrite);

        // Validate the object after reading
        ValidatableUtil.validate(t);
    }

    /**
     * Reads and populates the DTO based on the provided order.
     *
     * @param t         Target object to populate with read values.
     * @param in        Input source from which values are read.
     * @param overwrite Flag indicating whether to overwrite the existing value in the target object.
     * @throws InvalidMarshallableException If there is an error during marshalling.
     */
    public void readMarshallableDTOOrder(T t, @NotNull WireIn in, boolean overwrite) throws InvalidMarshallableException {
        try {
            for (@NotNull FieldAccess field : fields) {
                ValueIn vin = in.read(field.key);
                field.readValue(t, defaultValue, vin, overwrite);
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
     * @param overwrite Flag indicating whether to overwrite the existing value in the target object.
     * @throws InvalidMarshallableException If there is an error during marshalling.
     */
    public void readMarshallableInputOrder(T t, @NotNull WireIn in, boolean overwrite) throws InvalidMarshallableException {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            StringBuilder sb = stlSb.get();

            // Iterating over all fields to read their values
            for (int i = 0; i < fields.length; i++) {
                boolean more = in.hasMore();
                FieldAccess field = fields[i];

                ValueIn vin = more ? in.read(sb) : null;

                // Check if fields are present and in order
                if (more && matchesFieldName(sb, field)) {
                    field.readValue(t, defaultValue, in.getValueIn(), overwrite);

                } else {
                    // If not, copy default values
                    for (; i < fields.length; i++) {
                        FieldAccess field2 = fields[i];
                        field2.setDefaultValue(defaultValue, t);
                    }

                    if (vin == null || sb.length() <= 0)
                        return;

                    // Read the next set of values if there are any left
                    do {
                        FieldAccess fieldAccess = fieldMap.get(sb);
                        if (fieldAccess == null)
                            vin.skipValue();
                        else
                            fieldAccess.readValue(t, defaultValue, vin, overwrite);

                        vin = in.read(sb);
                    } while (in.hasMore());
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Checks if the given field name (represented by a StringBuilder) matches the field name of the provided {@link FieldAccess}.
     * If the StringBuilder has a length of 0, it's assumed to match any field name.
     *
     * @param sb    The StringBuilder containing the field name to be checked.
     * @param field The {@link FieldAccess} whose field name needs to be matched against.
     * @return True if the field name matches or if the StringBuilder is empty; False otherwise.
     */
    public boolean matchesFieldName(StringBuilder sb, FieldAccess field) {
        return sb.length() == 0 || StringUtils.equalsCaseIgnore(field.field.getName(), sb);
    }

    /**
     * Writes the key representation of the given object to the provided {@link Bytes} destination.
     * As per the assumption, only the first field (key) of the object is written.
     *
     * @param t     The object whose key needs to be written.
     * @param bytes The destination {@link Bytes} where the object's key representation will be written.
     */
    public void writeKey(T t, Bytes<?> bytes) {
        // assume one key for now.
        try {
            fields[0].getAsBytes(t, bytes);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Compares two objects field by field to determine their equality.
     * Uses each field's {@link FieldAccess} to perform the equality check.
     *
     * @param o1 The first object to compare.
     * @param o2 The second object to compare.
     * @return True if all fields of both objects are equal; False if at least one field differs.
     */
    public boolean isEqual(Object o1, Object o2) {
        for (@NotNull FieldAccess field : fields) {
            if (!field.isEqual(o1, o2))
                return false;
        }
        return true;
    }

    /**
     * Fetches the value of the specified field from the provided object.
     *
     * @param o    The object from which the field value needs to be fetched.
     * @param name The name of the field whose value is to be fetched.
     * @return The value of the specified field from the object.
     * @throws NoSuchFieldException If no field with the specified name is found in the object.
     */
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

    /**
     * Retrieves the value of a specified field from the provided object and converts it to a long.
     * If the field's type is not inherently long or int, it attempts a conversion using the ObjectUtils class.
     *
     * @param o    The object from which the field value is to be retrieved.
     * @param name The name of the field whose value needs to be fetched.
     * @return The long value of the specified field.
     * @throws NoSuchFieldException If no field with the specified name is found in the object.
     */
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

    /**
     * Sets the value of a specified field in the provided object.
     * If the type of the value does not directly match the field's type, it attempts a conversion using the ObjectUtils class.
     *
     * @param o     The object in which the field's value needs to be set.
     * @param name  The name of the field whose value needs to be set.
     * @param value The value to set to the field.
     * @throws NoSuchFieldException If no field with the specified name is found in the object.
     */
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

    /**
     * Sets a long value to a specified field in the provided object.
     * If the field's type is not inherently long or int, it attempts a conversion using the ObjectUtils class.
     *
     * @param o     The object in which the field's value needs to be set.
     * @param name  The name of the field whose value needs to be set.
     * @param value The long value to set to the field.
     * @throws NoSuchFieldException If no field with the specified name is found in the object.
     */
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

    /**
     * Returns the default value of type T.
     *
     * @return The default value of type T.
     */
    @Nullable
    public T defaultValue() {
        return defaultValue;
    }

    /**
     * Resets the fields of the given object 'o' to the default value.
     *
     * @param o The object whose fields are to be reset to the default value.
     */
    public void reset(T o) {
        try {
            for (FieldAccess field : fields)
                field.setDefaultValue(defaultValue, o);
        } catch (IllegalAccessException e) {
            // should never happen as the types should match.
            throw new AssertionError(e);
        }
    }

    /**
     * Checks if the current WireMarshaller is a leaf.
     *
     * @return true if the WireMarshaller is a leaf, false otherwise.
     */
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
        static LongConverter getInstance(Class<?> clazz) {
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
                try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                    StringBuilder sb = stlSb.get();
                    longConverter.append(sb, aLong);
                    if (!write.isBinary() && sb.length() == 0)
                        write.text("");
                    else if (longConverter.allSafeChars() || noUnsafeChars(sb))
                        write.rawText(sb);
                    else
                        write.text(sb);
                }
            }
        }

        private boolean noUnsafeChars(StringBuilder sb) {
            int index = sb.length() - 1;
            if (sb.charAt(0) == ' ' || sb.charAt(index) == ' ')
                return false;
            for (int i = 0; i < sb.length(); i++) {
                if (":'\"#,".indexOf(sb.charAt(i)) >= 0)
                    return false;
            }
            return true;
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
                try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                    StringBuilder sb = stlSb.get();
                    read.text(sb);
                    i = longConverter.parse(sb);
                    if (!rangeCheck(i))
                        throw new IORuntimeException("value '" + sb + "' is out of range for a " + field.getType());
                }
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
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                StringBuilder sb = stlSb.get();
                bytes.readUtf8(sb);
                long i = longConverter.parse(sb);
                bytes.writeLong(i);
            }
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
        public static Object create(@NotNull Field field, @Nullable Object defaultObject) {
            Class<?> type = field.getType();
            try {
                if (type.isArray()) {
                    if (type.getComponentType() == byte.class)
                        return new ByteArrayFieldAccess(field);
                    return new ArrayFieldAccess(field);
                }
                if (EnumSet.class.isAssignableFrom(type)) {
                    final Class<?> componentType = extractClass(computeActualTypeArguments(EnumSet.class, field)[0]);
                    if (componentType == Object.class || Modifier.isAbstract(componentType.getModifiers()))
                        throw new RuntimeException("Could not get enum constant directory");

                    boolean isLeaf = !Throwable.class.isAssignableFrom(componentType)
                            && WIRE_MARSHALLER_CL.get(componentType).isLeaf;
                    try {
                        Object[] values = (Object[]) Jvm.getMethod(componentType, "values").invoke(componentType);
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
                        return new ByteFieldAccess(field);
                    }
                    case "char": {
                        LongConverter longConverter = acquireLongConverter(field);
                        if (longConverter != null)
                            return new CharLongConverterFieldAccess(field, longConverter);
                        return new CharFieldAccess(field);
                    }
                    case "short": {
                        LongConverter longConverter = acquireLongConverter(field);
                        if (longConverter != null)
                            return new ShortLongConverterFieldAccess(field, longConverter);
                        return new ShortFieldAccess(field);
                    }
                    case "int": {
                        LongConverter longConverter = acquireLongConverter(field);
                        if (longConverter != null)
                            return new IntLongConverterFieldAccess(field, longConverter);
                        return new IntegerFieldAccess(field);
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
                        return new StringBuilderFieldAccess(field, defaultObject);
                    case "net.openhft.chronicle.bytes.Bytes":
                        return new BytesFieldAccess(field);
                    default:
                        if (isRecord != null && (boolean) isRecord.invoke(type))
                            throw new UnsupportedOperationException("Record classes are not supported");
                        @Nullable Boolean isLeaf = null;
                        if (IntValue.class.isAssignableFrom(type))
                            return new IntValueAccess(field);
                        if (LongValue.class.isAssignableFrom(type))
                            return new LongValueAccess(field);
                        if (WireMarshaller.class.isAssignableFrom(type))
                            isLeaf = WIRE_MARSHALLER_CL.get(type).isLeaf;
                        else if (isCollection(type))
                            isLeaf = false;

                        Object defaultValue = defaultObject == null ? null : field.get(defaultObject);
                        if (defaultValue != null && defaultValue instanceof Resettable && !(defaultValue instanceof DynamicEnum))
                            return new ResettableFieldAccess(field, isLeaf, defaultValue);

                        return new ObjectFieldAccess(field, isLeaf);
                }
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw Jvm.rethrow(ex);
            }
        }

        /**
         * Acquires a LongConverter instance associated with a given field, if available.
         * <p>
         * This method checks if the provided field has a LongConversion annotation. If present,
         * it retrieves the corresponding LongConverter using the LongConverterFieldAccess helper class.
                 *
         * @param field The field for which the LongConverter needs to be obtained
         * @return The associated LongConverter instance, or null if not present
         */
        @Nullable
        private static LongConverter acquireLongConverter(@NotNull Field field) {
            LongConversion longConversion = Jvm.findAnnotation(field, LongConversion.class);
            LongConverter longConverter = null;
            if (longConversion != null)
                longConverter = LongConverterFieldAccess.getInstance(longConversion.value());
            return longConverter;
        }

        /**
         * Extracts the raw Class type from a given Type object.
         * <p>
         * This method aims to handle various Type representations, like Class or ParameterizedType,
         * and return the underlying Class representation.
                 *
         * @param type0 The type from which the class should be extracted
         * @return The extracted Class representation
         */
        @NotNull
        static Class<?> extractClass(Type type0) {
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

        /**
         * Writes the value of a given object's field to a provided WireOut instance.
         * <p>
         * This method serializes the value of the specified object's field and writes it to
         * the WireOut. If the field has a Comment annotation, a special processing is done
         * using the CommentAnnotationNotifier.
                 *
         * @param o    The object containing the field to be written
         * @param out  The WireOut instance to write the field value to
         * @throws IllegalAccessException If the field cannot be accessed
         * @throws InvalidMarshallableException If the object cannot be marshalled
         */
        void write(Object o, @NotNull WireOut out) throws IllegalAccessException, InvalidMarshallableException {

            ValueOut valueOut = out.write(field.getName());

            if (valueOut instanceof CommentAnnotationNotifier && this.commentAnnotation != null) {
                getValueCommentAnnotated(o, out, valueOut);
                return;
            }

            getValue(o, valueOut, null);

        }

        /**
         * Retrieves the value of the provided object's field and writes it to the provided WireOut instance,
         * appending a comment based on the associated Comment annotation.
         * <p>
         * If the field has a Comment annotation, its value is formatted using the field's value and appended as a comment.
         * The CommentAnnotationNotifier is used to indicate that the written value is preceded by a comment.
                 *
         * @param o         The object containing the field whose value needs to be retrieved
         * @param out       The WireOut instance to which the value and the comment are written
         * @param valueOut  The ValueOut instance representing the field's value
         * @throws IllegalAccessException If the field cannot be accessed
         * @throws InvalidMarshallableException If the object cannot be marshalled
         */
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
                    try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                        StringBuilder sb = stlSb.get();
                        read.text(sb);
                        Jvm.warn().on(getClass(), "Failed to read '" + this.field.getName() + "' with '" + sb + "' taking default", e);
                    }
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
         * Abstract method to reset the value of a field in an object to default value.
         * The default value is the one present in objects of that class after no-argument constructor.
         * Where possible, existing data structures should be preserved without reallocation to avoid garbage.
         *
         * @param defaultObject A reference unmodified instance of this class.
         * @param o             Object to reset the value in.
         */
        protected void setDefaultValue(Object defaultObject, Object o) throws IllegalAccessException {
            copy(defaultObject, o);
        }

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

    /**
     * This is a specialized FieldAccess implementation for fields of type IntValue.
     * It provides implementations for reading and writing the IntValue from and to various sources.
     */
    static class IntValueAccess extends FieldAccess {

        /**
         * Constructor for the IntValueAccess class.
         *
         * @param field The field this FieldAccess is responsible for.
         */
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

    /**
     * This is a specialized FieldAccess implementation for fields of type LongValue.
     * It provides implementations for reading and writing the LongValue from and to various sources.
     */
    static class LongValueAccess extends FieldAccess {

        /**
         * Constructor for the LongValueAccess class.
         *
         * @param field The field this FieldAccess is responsible for.
         */
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

    /**
     * This is a specialized FieldAccess implementation for generic object fields.
     * It provides methods for reading and writing object fields from and to various sources,
     * taking into account special cases where the field may be marshaled differently based on annotations.
     */
    static class ObjectFieldAccess extends FieldAccess {
        private final Class<?> type; // Type of the object field
        private final AsMarshallable asMarshallable; // Annotation indicating if the field should be treated as marshallable

        /**
         * Constructor for the ObjectFieldAccess class.
         *
         * @param field  The field this FieldAccess is responsible for.
         * @param isLeaf A flag indicating whether the field is a leaf node.
         */
        ObjectFieldAccess(@NotNull Field field, Boolean isLeaf) {
            super(field, isLeaf);
            // Get annotations and field type
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

    static class ResettableFieldAccess extends ObjectFieldAccess {
        private final Object defaultValue;

        ResettableFieldAccess(@NotNull Field field, Boolean isLeaf, Object defaultValue) {
            super(field, isLeaf);
            this.defaultValue = defaultValue;
        }

        @Override
        protected void setDefaultValue(Object defaultObject, Object o) throws IllegalAccessException {
            Object existingValue = unsafeGetObject(o, offset);

            if (existingValue == defaultValue)
                return;

            if (existingValue != null && existingValue.getClass() == defaultValue.getClass()) {
                ((Resettable) existingValue).reset();

                return;
            }

            super.setDefaultValue(defaultObject, o);
        }
    }

    static class StringFieldAccess extends FieldAccess {
        StringFieldAccess(@NotNull Field field) {
            super(field, false);  // Strings are not leaf nodes, hence 'false'
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
            bytes.writeUtf8(unsafeGetObject(o, offset));
        }
    }

    /**
     * This is a specialized FieldAccess implementation for StringBuilder fields.
     * It provides methods to efficiently read and write StringBuilder fields from
     * and to various sources, using unsafe operations for performance optimization.
     */
    static class StringBuilderFieldAccess extends FieldAccess {
        private StringBuilder defaultValue;

        public StringBuilderFieldAccess(@NotNull Field field, @Nullable Object defaultObject) throws IllegalAccessException {
            super(field, true);
            this.defaultValue = defaultObject == null ? null : (StringBuilder) field.get(defaultObject);
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, Object previous) {
            @NotNull CharSequence cs = unsafeGetObject(o, offset);
            write.text(cs);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            StringBuilder sb = unsafeGetObject(o, offset);
            if (sb == null) {
                sb = new StringBuilder();
                unsafePutObject(o, offset, sb);
            }
            if (read.textTo(sb) == null)
                unsafePutObject(o, offset, null);
        }

        @Override
        protected void setDefaultValue(Object defaultObject, Object o) throws IllegalAccessException {
            if (defaultValue == null) {
                super.setDefaultValue(defaultObject, o);
                return;
            }

            StringBuilder sb = unsafeGetObject(o, offset);
            if (sb == defaultValue)
                return;
            if (sb == null) {
                sb = new StringBuilder();
                unsafePutObject(o, offset, sb);
            }
            sb.setLength(0);
            sb.append(defaultValue);
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

    /**
     * This is a specialized FieldAccess implementation for Bytes fields.
     * It provides methods to efficiently read and write Bytes fields from
     * and to various sources, using unsafe operations for performance optimization.
     */
    static class BytesFieldAccess extends FieldAccess {

        /**
         * Constructor for the BytesFieldAccess class.
         *
         * @param field The Bytes field this FieldAccess is responsible for.
         */
        BytesFieldAccess(@NotNull Field field) {
            super(field, false);  // Bytes is not treated as a leaf node
        }

        @Override
        protected void getValue(@NotNull Object o, @NotNull ValueOut write, Object previous)
                throws IllegalAccessException {
            Bytes<?> bytesField = (Bytes) field.get(o);
            write.bytes(bytesField);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            @NotNull Bytes<?> bytes = unsafeGetObject(o, offset);
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

        /**
         * Helper method to decode a Base64-encoded text value into a Bytes instance.
         *
         * @param read  The ValueIn instance containing the Base64-encoded text value.
         * @param bytes The Bytes instance where the decoded value will be stored.
         */
        private void decodeBytes(@NotNull ValueIn read, Bytes<?> bytes) {
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                @NotNull StringBuilder sb0 = stlSb.get();
                read.text(sb0);
                String s = WireInternal.INTERNER.intern(sb0);
                byte[] decode = Base64.getDecoder().decode(s);
                bytes.clear();
                bytes.write(decode);
            }
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) throws IllegalAccessException {
            Bytes<?> bytesField = (Bytes) field.get(o);
            bytes.write(bytesField);
        }

        @Override
        protected void copy(Object from, Object to) {
            Bytes<?> fromBytes = unsafeGetObject(from, offset);
            Bytes<?> toBytes = unsafeGetObject(to, offset);
            if (fromBytes == toBytes)
                return;

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

    /**
     * The ArrayFieldAccess class extends FieldAccess to provide specialized access
     * and manipulation methods for fields that are arrays.
     * <p>
     * It calculates the component type of the array and retrieves its equivalent object type
     * for ease of use. The class is designed to handle arrays in a generic manner and
     * use the provided methods of the superclass for actual field manipulation.
     */
    static class ArrayFieldAccess extends FieldAccess {
        private final Class<?> componentType;
        private final Class<?> objectType;

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

    /**
     * The ByteArrayFieldAccess class extends FieldAccess to provide specialized access
     * and manipulation methods for fields that are byte arrays.
     * <p>
     * The class is optimized for reading and writing byte arrays to and from a wire format
     * while preserving encapsulation and supporting null values.
     */
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

    /**
     * The EnumSetFieldAccess class extends FieldAccess to provide specialized access and manipulation methods
     * for fields that are of type {@link EnumSet}.
     * <p>
     * This class allows efficient reading and writing of EnumSet fields to and from a wire format while
     * preserving encapsulation and supporting null values.
     */
    static class EnumSetFieldAccess extends FieldAccess {

        // An array of enum values
        private final Object[] values;

        // The sequence getter function used for iterating and retrieving enum values from EnumSet
        private final BiConsumer<Object, ValueOut> sequenceGetter;

        // The type of the enum component
        private final Class<?>componentType;

        // A supplier for creating an empty EnumSet of the component type
        private final Supplier<EnumSet> enumSetSupplier;
        private final BiConsumer<EnumSet, ValueIn> addAll;

        /**
         * Constructor for the EnumSetFieldAccess class.
         * Initializes the field to be accessed, which should be of type EnumSet, and prepares the
         * necessary utilities for manipulating it.
         *
         * @param field The field to be accessed, expected to be an EnumSet.
         * @param isLeaf A flag to determine whether the data structure is at its deepest level.
         * @param values An array of possible enum values.
         * @param componentType The type of enum component within the EnumSet.
         */
        EnumSetFieldAccess(@NotNull final Field field, final Boolean isLeaf, final Object[] values, final Class<?> componentType) {
            super(field, isLeaf);
            this.values = values;
            this.componentType = componentType;
            this.enumSetSupplier = () -> EnumSet.noneOf((Class) this.componentType);
            this.sequenceGetter = (o, out) -> sequenceGetter(o,
                    out, this.values, this.field, this.componentType);
            this.addAll = this::addAll;
        }

        /**
         * Retrieves the values from the provided object's EnumSet field and writes them
         * using the provided ValueOut writer. Only values that are part of the provided
         * enum values will be written.
         *
         * @param o The object from which the EnumSet field value is retrieved.
         * @param out The writer to output the enum values.
         * @param values An array of possible enum values.
         * @param field The EnumSet field from which values are retrieved.
         * @param componentType The type of enum component within the EnumSet.
         * @throws InvalidMarshallableException If the marshalling process is invalid.
         */
        private static void sequenceGetter(Object o,
                                           ValueOut out,
                                           Object[] values,
                                           Field field,
                                           Class<?>componentType)
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

        /**
         * Adds all the enum items from the provided ValueIn reader into the specified EnumSet.
         * If the EnumSet already contains values, it clears them before adding the new items.
         * The method reads each sequence item from the reader and adds it to the EnumSet as an enum value.
         *
         * @param c The EnumSet to which enum items are to be added.
         * @param in2 The ValueIn reader from which enum values are read.
         */
        private void addAll(EnumSet c, ValueIn in2) {
            if (!c.isEmpty())
                c.clear();
            while (in2.hasNextSequenceItem()) {
                c.add(in2.asEnum((Class) componentType));
            }
        }
    }

    /**
     * The CollectionFieldAccess class extends FieldAccess to provide specialized access and manipulation methods
     * for fields that are of type {@link Collection}.
     * <p>
     * It is optimized to handle both random-access and non-random-access collections efficiently. Additionally,
     * the class provides flexibility by allowing users to supply custom collection creation logic if needed.
     */
    static class CollectionFieldAccess extends FieldAccess {

        // Supplier to provide a new instance of a Collection
        @NotNull
        final Supplier<Collection> collectionSupplier;

        // The component type of the Collection
        private final Class<?>componentType;
        private final Class<?> type;
        private final BiConsumer<Object, ValueOut> sequenceGetter;

        /**
         * Constructs a CollectionFieldAccess instance for a given field, with optional leaf indication,
         * custom collection supplier, component type, and collection type.
         *
         * @param field The field this accessor will manage.
         * @param isLeaf A flag indicating whether the field should be treated as a leaf in the object graph.
         * @param collectionSupplier The supplier for creating new instances of the collection. If null, a default will be used.
         * @param componentType The type of the elements in the collection.
         * @param type The type of the collection itself.
         */
        public CollectionFieldAccess(@NotNull Field field, Boolean isLeaf, @Nullable Supplier<Collection> collectionSupplier, Class<?>componentType, Class<?>type) {
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

        /**
         * Determines the appropriate type of FieldAccess based on the provided field's type and characteristics.
         * <p>
         * This method analyses the type and generic parameters of the field, decides on a suitable Collection
         * supplier, and then returns an instance of either StringCollectionFieldAccess or CollectionFieldAccess.
         *
         * @param field The field for which a FieldAccess instance is required.
         * @return An instance of the appropriate FieldAccess subtype for the given field.
         */
        @NotNull
        static FieldAccess of(@NotNull Field field) {
            @Nullable final Supplier<Collection> collectionSupplier;
            @NotNull final Class<?> componentType;
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

        /**
         * Provides a supplier to create a new instance of the collection type associated with this field access.
         * <p>
         * This method utilizes the ObjectUtils utility class to instantiate a new collection object
         * based on the type.
         *
         * @return A supplier that can create a new instance of the collection type.
         */
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
            if (!fromColl.isEmpty())
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

    /**
     * The StringCollectionFieldAccess class extends FieldAccess to provide specialized access and manipulation methods
     * for fields that are of type {@link Collection} where the elements of the collection are {@link String} instances.
     * <p>
     * This extension particularly manages the parsing of strings from the given input and offers utility functions
     * for instantiation of the correct {@link Collection} type.
     */
    static class StringCollectionFieldAccess extends FieldAccess {

        // Supplier that provides new instances of the underlying collection type
        @NotNull
        final Supplier<Collection> collectionSupplier;

        // The type of the collection that this field access manages
        private final Class<?> type;

        // Consumer that processes each sequence item and populates the collection with strings
        @NotNull
        private final BiConsumer<Collection, ValueIn> seqConsumer = (c, in2) -> {
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

        /**
         * Constructs an instance of StringCollectionFieldAccess with the specified parameters.
         *
         * @param field               The field to be managed by this field access instance.
         * @param isLeaf              A flag indicating if the field is a leaf node.
         * @param collectionSupplier  Supplier to provide instances of the desired collection type.
         * @param type                The type of the collection that this field access manages.
         */
        public StringCollectionFieldAccess(@NotNull Field field, Boolean isLeaf, @Nullable Supplier<Collection> collectionSupplier, Class<?> type) {
            super(field, isLeaf);
            this.collectionSupplier = collectionSupplier == null ? newInstance() : collectionSupplier;
            this.type = type;
        }

        /**
         * Provides a supplier to create a new instance of the collection type associated with this field access.
         * <p>
         * This method utilizes the ObjectUtils utility class to instantiate a new collection object
         * based on the type.
         *
         * @return A supplier that can create a new instance of the collection type.
         */
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
            if (!fromColl.isEmpty())
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

    /**
     * The MapFieldAccess class extends FieldAccess to provide specialized access and manipulation methods
     * for fields that are of type {@link Map}.
     * <p>
     * This extension is designed to manage the parsing and instantiation of the correct {@link Map} type
     * and provides functionality to work with the key-value pairs within the map.
     */
    static class MapFieldAccess extends FieldAccess {

        // Supplier that provides new instances of the underlying map type
        @NotNull
        final Supplier<Map> collectionSupplier;

        // The type of the map that this field access manages
        private final Class<?> type;

        // The type of the keys within the map
        @NotNull
        private final Class<?> keyType;
        @NotNull
        private final Class<?> valueType;

        /**
         * Constructs an instance of MapFieldAccess for the specified field.
         * <p>
         * This constructor initializes the map type, key type, value type, and an appropriate
         * supplier for instantiating new map instances based on the field's type and generic parameters.
         *
         * @param field The map field to be managed by this field access instance.
         */
        MapFieldAccess(@NotNull Field field) {
            super(field);
            type = field.getType();
            if (type == Map.class)
                collectionSupplier = LinkedHashMap::new;
            else if (type == SortedMap.class || type == NavigableMap.class)
                collectionSupplier = TreeMap::new;
            else
                collectionSupplier = newInstance();

            // Extract generic type arguments for key and value types
            Type[] actualTypeArguments = computeActualTypeArguments(Map.class, field);
            keyType = extractClass(actualTypeArguments[0]);
            valueType = extractClass(actualTypeArguments[1]);
        }

        /**
         * Provides a supplier to create a new instance of the map type associated with this field access.
         * <p>
         * This method utilizes the ObjectUtils utility class to instantiate a new map object
         * based on the type.
         *
         * @return A supplier that can create a new instance of the map type.
         */
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
            if (!fromMap.isEmpty())
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

    /**
     * The BooleanFieldAccess class extends FieldAccess to provide specialized access and manipulation methods
     * for fields that are of type boolean or {@link Boolean}.
     * <p>
     * This extension uses unsafe operations to get and set the boolean value efficiently without invoking reflection
     * on each operation.
     */
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

    /**
     * The ByteFieldAccess class extends FieldAccess to provide specialized access and manipulation methods
     * for fields that are of type byte or {@link Byte}.
     * <p>
     * This extension leverages unsafe operations to get and set the byte value directly without the need for
     * reflective access each time, ensuring better performance.
     */
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

    /**
     * The ShortFieldAccess class extends FieldAccess to provide specialized access and manipulation methods
     * for fields that are of type short or {@link Short}.
     * <p>
     * This extension leverages unsafe operations to get and set the short value directly without the need for
     * reflective access each time, ensuring optimal performance.
     */
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

    /**
     * The CharFieldAccess class extends FieldAccess to provide specialized access and manipulation methods
     * for fields that are of type char or {@link Character}.
     * <p>
     * This extension leverages unsafe operations to get and set the character value directly without the need for
     * reflective access each time, ensuring optimal performance.
     */
    static class CharFieldAccess extends FieldAccess {

        /**
         * Constructs an instance of CharFieldAccess for the specified field.
         */
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
                try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                    StringBuilder sb = stlSb.get();
                    sb.append(c);
                    write.text(sb);
                }
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

    /**
     * The IntegerFieldAccess class extends FieldAccess to provide specialized access and manipulation methods
     * for fields that are of type int or {@link Integer}.
     * <p>
     * This extension leverages unsafe operations to get and set the integer value directly without the need for
     * reflective access each time, ensuring optimal performance.
     */
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

    /**
     * The ByteIntConversionFieldAccess class extends IntConversionFieldAccess to provide specialized access
     * and conversion between fields that are of type byte and their representation as int.
     * <p>
     * This extension leverages unsafe operations to get and set the byte value directly from or to an object
     * while converting to or from an int respectively. This helps in preserving the value of the byte as an
     * unsigned integer representation.
     */
    static class ByteIntConversionFieldAccess extends IntConversionFieldAccess {
        public ByteIntConversionFieldAccess(@NotNull Field field, @NotNull LongConversion conversion) {
            super(field, conversion);
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

    /**
     * The ShortIntConversionFieldAccess class extends IntConversionFieldAccess to provide specialized access
     * and conversion between fields that are of type short and their representation as int.
     * <p>
     * This extension leverages unsafe operations to get and set the short value directly from or to an object
     * while converting to or from an int respectively. This helps in preserving the value of the short as an
     * unsigned integer representation.
     */
    static class ShortIntConversionFieldAccess extends IntConversionFieldAccess {
        public ShortIntConversionFieldAccess(@NotNull Field field, @NotNull LongConversion conversion) {
            super(field, conversion);
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
         * Constructs a new instance
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
         * Constructs a new instance
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
         * Constructs a new instance
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
         * Constructs a new instance
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

    static class IntConversionFieldAccess extends FieldAccess {
        @NotNull
        private final LongConverter converter;

        IntConversionFieldAccess(@NotNull Field field, @NotNull LongConversion conversion) {
            super(field);
            this.converter = (LongConverter) ObjectUtils.newInstance(conversion.value());
        }

        @Override
        protected void getValue(Object o, @NotNull ValueOut write, @Nullable Object previous) {
            int anInt = getInt(o);
            if (write.isBinary()) {
                write.int32(anInt);
            } else {
                try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                    StringBuilder sb = stlSb.get();
                    converter.append(sb, anInt);
                    if (!write.isBinary() && sb.length() == 0)
                        write.text("");
                    else
                        write.rawText(sb);
                }
            }
        }

        /**
         * A helper method to retrieve the integer from the object using the provided offset.
         *
         * @param o The object containing the field
         * @return  The retrieved integer value
         */
        protected int getInt(Object o) {
            return unsafeGetInt(o, offset);
        }

        @Override
        protected void setValue(Object o, @NotNull ValueIn read, boolean overwrite) {
            long i;
            if (read.isBinary()) {
                i = read.int64();

            } else {
                try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                    StringBuilder sb = stlSb.get();
                    read.text(sb);
                    i = converter.parse(sb);
                }
            }
            unsafePutLong(o, offset, i);
        }

        /**
         * A helper method to set the integer value on the object using the provided offset.
         *
         * @param o The object to set the value on
         * @param i The integer value to set
         */
        protected void putInt(Object o, int i) {
            unsafePutInt(o, offset, i);
        }

        @Override
        public void getAsBytes(Object o, @NotNull Bytes<?> bytes) {
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                StringBuilder sb = stlSb.get();
                bytes.readUtf8(sb);
                long i = converter.parse(sb);
                bytes.writeLong(i);
            }
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

    /**
     * The FloatFieldAccess class extends the FieldAccess class, providing specialized access
     * to float fields of an object. This class supports reading and writing float values,
     * considering a potential "previous" value for optimized serialization or other comparative tasks.
     */
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

    /**
     * The LongFieldAccess class extends FieldAccess to provide specialized access
     * to long fields of an object. It supports reading and writing long values,
     * considering a potential "previous" value for optimized serialization or other
     * comparative tasks.
     */
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

    /**
     * The DoubleFieldAccess class extends FieldAccess to provide specialized access
     * to double fields of an object. It supports reading and writing double values,
     * considering a potential "previous" value for optimized serialization or other
     * comparative tasks.
     */
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
