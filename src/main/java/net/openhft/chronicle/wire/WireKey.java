/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter.lawrey on 1/10/15.
 */
@FunctionalInterface
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
