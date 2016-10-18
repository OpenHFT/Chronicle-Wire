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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.openhft.chronicle.wire.WireMarshaller.WIRE_MARSHALLER_CL;

/**
 * Created by peter on 18/10/16.
 */
public class VanillaFieldInfo extends AbstractMarshallable implements FieldInfo {

    private final String name;
    private final Class type;
    private final BracketType bracketType;

    public VanillaFieldInfo(String name, Class type, BracketType bracketType) {
        this.name = name;
        this.type = type;
        this.bracketType = bracketType;
    }

    public static List<FieldInfo> lookupClass(Class aClass) {
        final SerializationStrategy ss = Wires.CLASS_STRATEGY.get(aClass);
        if (ss.bracketType() != BracketType.MAP)
            return Collections.emptyList();

        List<FieldInfo> fields = new ArrayList<>();
        final WireMarshaller marshaller = WIRE_MARSHALLER_CL.get(aClass);
        for (WireMarshaller.FieldAccess field : marshaller.fields) {
            final String name = field.field.getName();
            final Class<?> type = field.field.getType();
            final SerializationStrategy ss2 = Wires.CLASS_STRATEGY.get(type);
            final BracketType bracketType = ss2.bracketType();
            fields.add(new VanillaFieldInfo(name, type, bracketType));
        }
        return Collections.unmodifiableList(fields);
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
}
