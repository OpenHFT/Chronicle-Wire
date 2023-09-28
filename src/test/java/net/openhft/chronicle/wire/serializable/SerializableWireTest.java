/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire.serializable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(value = Parameterized.class)
public class SerializableWireTest extends WireTestCommon {
    private final WireType wireType;
    private final Serializable m;
    private final boolean ime;

    public SerializableWireTest(WireType wireType, Serializable m, boolean ime) {
        this.wireType = wireType;
        this.m = m;
        this.ime = ime;
    }

    @NotNull
    @Parameterized.Parameters(name = "wt: {0}, object: {1}, IME: {2}") // toString() implicility called here
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();
        // TODO FIX
        @NotNull WireType[] wireTypes = {WireType.TEXT/*, WireType.YAML_ONLY, WireType.BINARY*/};
        @NotNull Serializable[] objects = {
                new Nested(),
                new ScalarValues(),
                new Nested(new ScalarValues(), Collections.emptyList(), Collections.emptySet(), Collections.emptyMap()),
                new Nested(new ScalarValues(1), null, Collections.emptySet(), Collections.emptyMap()),
                new Nested(new ScalarValues(1), Collections.emptyList(), Collections.emptySet(), Collections.emptyMap()),
                new ScalarValues(1),
                new ScalarValues(10)
        };
        for (WireType wt : wireTypes) {
            for (Serializable object : objects) {
                @NotNull Object[] test = {wt, object, list.size() < 4};
                list.add(test);
            }
        }
        return list;
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void writeMarshallable() {
        if (ime) // TODO Fix to be expected
            ignoreException(ek -> ek.throwable instanceof InvalidMarshallableException, "IME");
        Bytes<?> bytes = Bytes.elasticByteBuffer();
        try {
            Wire wire = wireType.apply(bytes);

            wire.getValueOut().object(m);
            // System.out.println(wire);

            @Nullable Object m2 = wire.getValueIn().object();
            assertEquals(m, m2);
            if (ime)
                fail();
        } catch (InvalidMarshallableException e) {
            if (!ime)
                throw e;
        } finally {
            bytes.releaseLast();
        }
    }
}
