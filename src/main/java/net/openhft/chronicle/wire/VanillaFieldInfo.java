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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.openhft.chronicle.wire.WireMarshaller.WIRE_MARSHALLER_CL;

/**
 * Created by peter on 18/10/16.
 */
public class VanillaFieldInfo extends AbstractMarshallable implements FieldInfo {

    private final String name;
    private final Class type;
    private final BracketType bracketType;
    private final Class parent;
    private transient Field field;

    public VanillaFieldInfo(String name, Class type, BracketType bracketType, @NotNull Field field) {
        this.name = name;
        this.type = type;
        this.bracketType = bracketType;
        parent = field.getDeclaringClass();
        this.field = field;
    }

    @NotNull
    public static Wires.FieldInfoPair lookupClass(Class aClass) {
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

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class type() {
        return type;
    }

    @Override
    public BracketType bracketType() {
        return bracketType;
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
    public void set(Object object, Object value) {
        Object value2 = ObjectUtils.convertTo(type, value);
        try {
            getField().set(object, value2);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public Field getField() throws NoSuchFieldException {
        if (field == null) {
            field = parent.getDeclaredField(name);
            field.setAccessible(true);
        }
        return field;
    }
}
