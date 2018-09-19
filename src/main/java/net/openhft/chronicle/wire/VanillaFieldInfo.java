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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.openhft.chronicle.wire.WireMarshaller.WIRE_MARSHALLER_CL;

/*
 * Created by Peter Lawrey on 18/10/16.
 */
public class VanillaFieldInfo extends AbstractFieldInfo implements FieldInfo {

    private final Class parent;
    private transient Field field;

    public VanillaFieldInfo(String name, Class type, BracketType bracketType, @NotNull Field field) {
        super(type, bracketType, name);
        parent = field.getDeclaringClass();
        this.field = field;
    }

    @NotNull
    public static Wires.FieldInfoPair lookupClass(@NotNull Class aClass) {
        final SerializationStrategy ss = Wires.CLASS_STRATEGY.get(aClass);
        if (ss.bracketType() != BracketType.MAP) {
            return Wires.FieldInfoPair.EMPTY;
        }

        @NotNull List<FieldInfo> fields = new ArrayList<>();
        final WireMarshaller marshaller = WIRE_MARSHALLER_CL.get(aClass);
        for (@NotNull WireMarshaller.FieldAccess fa : marshaller.fields) {
            final String name = fa.field.getName();
            final Class<?> type = fa.field.getType();
            final SerializationStrategy ss2 = Wires.CLASS_STRATEGY.get(type);
            final BracketType bracketType = ss2.bracketType();
            fields.add(new VanillaFieldInfo(name, type, bracketType, fa.field));
        }
        return new Wires.FieldInfoPair(
                Collections.unmodifiableList(fields),
                fields.stream().collect(Collectors.toMap(FieldInfo::name, f -> f)));
    }

    @Nullable
    @Override
    public Object get(Object value) {
        try {
            return getField().get(value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return null;
        }
    }

    @Override
    public long getLong(Object value) {
        try {
            return getField().getLong(value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return Long.MIN_VALUE;
        }
    }

    @Override
    public int getInt(Object value) {
        try {
            return getField().getInt(value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public char getChar(Object value) {
        try {
            return getField().getChar(value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return Character.MAX_VALUE;
        }
    }

    @Override
    public double getDouble(Object value) {
        try {
            return getField().getDouble(value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return Double.NaN;
        }
    }

    @Override
    public void set(Object object, Object value) throws IllegalArgumentException {
        Object value2 = ObjectUtils.convertTo(type, value);
        try {
            getField().set(object, value2);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void set(Object object, int value) throws IllegalArgumentException {
        try {
            getField().setInt(object, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void set(Object object, char value) throws IllegalArgumentException {
        try {
            getField().setChar(object, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void set(Object object, long value) throws IllegalArgumentException {
        try {
            getField().setLong(object, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void set(Object object, double value) throws IllegalArgumentException {
        try {
            getField().setDouble(object, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Field getField() throws NoSuchFieldException {
        if (field == null) {
            field = parent.getDeclaredField(name);
            Jvm.setAccessible(field);
        }
        return field;
    }

    @NotNull
    @Override
    public Class genericType(int index) {
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type type = genericType.getActualTypeArguments()[index];
        return (Class) type;
    }
}
