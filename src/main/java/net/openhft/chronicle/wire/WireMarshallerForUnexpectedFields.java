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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.scoped.ScopedResource;
import org.jetbrains.annotations.NotNull;

/**
 * Extends {@link WireMarshaller} to provide custom handling for unexpected fields during
 * deserialisation. Used when the target class overrides
 * {@link ReadMarshallable#unexpectedField(CharSequence, ValueIn)} to gain control of unknown
 * data.
 */
public class WireMarshallerForUnexpectedFields<T> extends WireMarshaller<T> {
    /**
     * A {@link CharSequenceObjectMap} for efficient lookup of {@link FieldAccess} objects by
     * field name, supporting both original and lower-cased names for flexibility in matching
     * fields from the input wire.
     */
    final CharSequenceObjectMap<FieldAccess> fieldMap;

    /**
     * Constructs a marshaller that can delegate to
     * {@link ReadMarshallable#unexpectedField(CharSequence, ValueIn)} if unknown fields are
     * encountered. Initialises the internal field map for quick lookups.
     */
    public WireMarshallerForUnexpectedFields(@NotNull FieldAccess[] fields, boolean isLeaf, T defaultValue) {
        super(fields, isLeaf, defaultValue);
        fieldMap = new CharSequenceObjectMap<>(fields.length * 3);
        for (FieldAccess field : fields) {
            fieldMap.put(field.key.name().toString(), field);
            fieldMap.put(field.key.name().toString().toLowerCase(), field);
        }
    }

    /**
     * Overrides the default deserialisation logic to handle unexpected fields. When a field name
     * read from {@code WireIn} is not found in the known {@link #fields} (even after
     * case-insensitive matching), it calls
     * {@link ReadMarshallable#unexpectedField(CharSequence, ValueIn)} on object {@code t} if
     * possible. Known fields are processed as usual. The check
     * {@code sb.length() == 0 && vin.isPresent()} optimises for DTO-order field reading by using
     * the next field directly. Ensures progress is made during parsing to avoid infinite loops.
     */
    @Override
    public void readMarshallable(T t, @NotNull WireIn in, boolean overwrite) throws InvalidMarshallableException {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            ReadMarshallable rm = t instanceof ReadMarshallable ? (ReadMarshallable) t : null;
            StringBuilder sb = stlSb.get();
            int next = 0;
            if (overwrite) {
                for (FieldAccess field : fields) {
                    field.copy(defaultValue(), t);
                }
            }
            while (in.hasMore()) {
                long pos = in.bytes().readPosition();
                ValueIn vin = in.read(sb);
                FieldAccess field;
                if (next >= 0 && sb.length() == 0 && vin.isPresent()) {
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
                    if (rm == null) {
                        vin.skipValue();
                    } else {
                        // implicitly ignore fields starting with -
                        if (sb.length() > 0 && sb.charAt(0) == '-') {
                            vin.skipValue();
                        } else {
                            try {
                                rm.unexpectedField(sb, vin);
                            } catch (Exception e) {
                                throw new UnexpectedFieldHandlingException(e);
                            }
                        }
                    }
                } else {
                    field.readValue(t, defaultValue(), vin, overwrite);
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
