package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertSame;

@RunWith(Parameterized.class)
public class EnumWireTest {
    private final Function<Bytes, Wire> createWire;

    public EnumWireTest(Function<Bytes, Wire> createWire) {
        this.createWire = createWire;
    }

    @Parameterized.Parameters
    public static Iterable<Function<Bytes, Wire>> wires() {
        return Arrays.asList(TextWire::new, BinaryWire::new, RawWire::new);
    }

    private static Wire serialise(Function<Bytes, Wire> createWire, Marshallable person) {
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

    private <T extends Marshallable> T roundTrip(Supplier<T> supplier) {
        Wire wire = serialise(createWire, supplier.get());
        System.out.println(wire.bytes());
        T deserialized = supplier.get();
        deserialized.readMarshallable(wire);
        return deserialized;
    }

    enum Marsh implements Marshallable {
        MARSH
    }

    enum NoMarsh {
        NO_MARSH;
    }

    enum MarshAndResolve implements Marshallable/*, ReadResolvable<MarshAndResolve>*/ {
        MARSH_AND_RESOLVE;

        // not support yet but harmless
        public MarshAndResolve readResolve() {
            return values()[ordinal()];
        }
    }

    static class Person1 extends AbstractMarshallable {
        private Marsh field = Marsh.MARSH;
    }

    static class Person2 extends AbstractMarshallable {
        private NoMarsh field = NoMarsh.NO_MARSH;
    }

    static class Person3 extends AbstractMarshallable {
        private MarshAndResolve field = MarshAndResolve.MARSH_AND_RESOLVE;
    }
}