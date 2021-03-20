/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

public class WireMarshallerForUnexpectedFields<T> extends WireMarshaller<T> {
    final CharSequenceObjectMap<FieldAccess> fieldMap;

    public WireMarshallerForUnexpectedFields(@NotNull Class<T> tClass, @NotNull FieldAccess[] fields, boolean isLeaf) {
        super(tClass, fields, isLeaf);
        fieldMap = new CharSequenceObjectMap<>(fields.length * 3);
        for (FieldAccess field : fields) {
            fieldMap.put(field.key.name().toString(), field);
            fieldMap.put(field.key.name().toString().toLowerCase(), field);
        }
    }

    public void readMarshallable(T t, @NotNull WireIn in, T defaults, boolean overwrite) {
        try {
            ReadMarshallable rm = t instanceof ReadMarshallable ? (ReadMarshallable) t : null;
            StringBuilder sb = Wires.acquireStringBuilder();
            int next = 0;
            if (overwrite) {
                for (FieldAccess field : fields) {
                    field.copy(defaults, t);
                }
            }
            while (in.hasMore()) {
                long pos = in.bytes().readPosition();
                ValueIn vin = in.read(sb);
                FieldAccess field;
                if (next >= 0 && sb.length() == 0 && !(vin instanceof DefaultValueIn)) {
                    field = fields[next++];
                } else {
                    next = -1;
                    field = fieldMap.get(sb);
                    if (field == null) {
                        for (int i = 0; i < sb.length(); i++)
                            sb.setCharAt(i, Character.toLowerCase(sb.charAt(i)));
                        field = fieldMap.get(sb);
                    }
                }
                if (field == null) {
                    if (rm == null)
                        vin.skipValue();
                    else
                        rm.unexpectedField(sb, vin);
                } else {
                    field.readValue(t, defaults, vin, overwrite);
                }
                if (pos >= in.bytes().readPosition()) {
                    Jvm.warn().on(getClass(), "Failed to parse " + in.bytes());
                    return;
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
