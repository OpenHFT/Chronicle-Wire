package net.openhft.chronicle.wire;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter on 1/10/15.
 */
public interface WireKey {
    CharSequence name();

    default int code() {
        return name().toString().hashCode();
    }

    default Type type() {
        Object o = defaultValue();
        return o == null ? Void.class : o.getClass();
    }

    default Object defaultValue() {
        return null;
    }

    static boolean checkKeys(WireKey[] keys) {
        Map<Integer, WireKey> codes = new HashMap<>();
        for (WireKey key : keys) {
            WireKey pkey = codes.put(key.code(), key);
            if (pkey != null)
                throw new AssertionError(pkey + " and " + key + " have the same code " + key.code());
        }
        return true;
    }
}
