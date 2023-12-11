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

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class HashWireTest extends WireTestCommon {

    @Test
    public void testHash64() {
        long h = HashWire.hash64(wire ->
                wire.write(() -> "entrySet").sequence(s -> {
                    s.marshallable(m -> m
                            .write(() -> "key").text("key-1")
                            .write(() -> "value").text("value-1"));
                    s.marshallable(m -> m
                            .write(() -> "key").text("key-2")
                            .write(() -> "value").text("value-2"));
                }));
        assertNotEquals(0, h);
    }

    @Test
    public void testHashWithMap() {
        assertEquals(428977857, new Field("hi").hashCode());
    }

    enum Required {
        A
    }

    enum EnumValue {
        A
    }

    static class Field extends SelfDescribingMarshallable implements Cloneable {
        private final String name;
        private final Map<String, Required> required = new HashMap<>();
        private final List<EnumValue> values = new ArrayList<>();
        private boolean used = false;

        public Field(String name) {
            this.name = name;
        }
    }
}
