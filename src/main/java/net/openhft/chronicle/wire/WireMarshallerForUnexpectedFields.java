/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.scoped.ScopedResource;
import org.jetbrains.annotations.NotNull;

/**
 * This class extends the WireMarshaller and provides the ability to handle unexpected fields.
 * It maps fields by their name (both in their original form and lower-cased) for easy access.
 * It overrides the method to read marshallable objects and provides specialized logic to
 * handle unexpected fields that might be present in the data source.
 */
public class WireMarshallerForUnexpectedFields<T> extends WireMarshaller<T> {
    /**
     * A {@link CharSequenceObjectMap} for efficient lookup of {@link FieldAccess} objects by
     * field name, supporting both original and lower-cased names for flexibility in matching
     * fields from the input wire.
     */
    @SuppressWarnings("java:S2387")
    final CharSequenceObjectMap<FieldAccess> fieldMap;

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
     * {@link ReadMarshallable#unexpectedField(Object, ValueIn)} on object {@code t} if
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
