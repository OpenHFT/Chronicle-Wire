/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.util.ReadResolvable;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertSame;

/**
 * @author greg allen
 */
@SuppressWarnings("rawtypes")
@RunWith(Parameterized.class)
public class EnumWireTest extends WireTestCommon {
    private final Function<Bytes<?>, Wire> createWire;

    public EnumWireTest(Function<Bytes<?>, Wire> createWire) {
        this.createWire = createWire;
    }

    @Parameterized.Parameters
    public static Iterable<Function<Bytes<?>, Wire>> wires() {
        return Arrays.asList(YamlWire::new, TextWire::new, BinaryWire::new, RawWire::new);
    }

    private static Wire serialise(@NotNull Function<Bytes<?>, Wire> createWire, @NotNull Marshallable person) {
        Wire wire = createWire.apply(Bytes.elasticByteBuffer());
        person.writeMarshallable(wire);
        return wire;
    }

    @Test
    public void testEnumImplementingMarshallable() {
        assertSame(Marsh.MARSH, roundTrip(Person1::new).field);
    }

    @Test
    public void testEnumNotImplementingMarshallable() {
        assertSame(NoMarsh.NO_MARSH, roundTrip(Person2::new).field);
    }

    @Test
    public void testEnumImplementingMarshallableAndReadResolve() {
        assertSame(MarshAndResolve.MARSH_AND_RESOLVE, roundTrip(Person3::new).field);
    }

    private <T extends Marshallable> T roundTrip(@NotNull Supplier<T> supplier) {
        Wire wire = serialise(createWire, supplier.get());
       // System.out.println(wire.bytes());
        try {
            T deserialized = supplier.get();
            deserialized.readMarshallable(wire);
            return deserialized;
        } finally {
            wire.bytes().releaseLast();
        }
    }

    enum Marsh implements Marshallable {
        MARSH
    }

    enum NoMarsh {
        NO_MARSH
    }

    static class MarshAndResolve implements Marshallable, ReadResolvable<MarshAndResolve> {
        static final MarshAndResolve MARSH_AND_RESOLVE = new MarshAndResolve();

        @Override
        @NotNull
        public MarshAndResolve readResolve() {
            return MARSH_AND_RESOLVE;
        }
    }

    static class Person1 extends SelfDescribingMarshallable {
        @NotNull
        private Marsh field = Marsh.MARSH;
    }

    static class Person2 extends SelfDescribingMarshallable {
        @NotNull
        private NoMarsh field = NoMarsh.NO_MARSH;
    }

    static class Person3 extends SelfDescribingMarshallable {
        @NotNull
        private MarshAndResolve field = MarshAndResolve.MARSH_AND_RESOLVE;
    }
}