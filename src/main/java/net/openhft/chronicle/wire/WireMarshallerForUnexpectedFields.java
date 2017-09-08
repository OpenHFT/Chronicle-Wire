package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

public class WireMarshallerForUnexpectedFields<T> extends WireMarshaller<T> {
    final CharSequenceObjectMap<FieldAccess> fieldMap;

    public WireMarshallerForUnexpectedFields(@NotNull Class<T> tClass, @NotNull FieldAccess[] fields, boolean isLeaf) {
        super(tClass, fields, isLeaf);
        fieldMap = new CharSequenceObjectMap<>(fields.length * 3 / 2);
        for (FieldAccess field : fields) {
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
