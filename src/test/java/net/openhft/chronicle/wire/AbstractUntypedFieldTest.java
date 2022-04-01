package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbstractUntypedFieldTest extends WireTestCommon {

    @BeforeEach
    void beforeEach() {
        ClassAliasPool.CLASS_ALIASES.addAlias(AImpl.class, "AImpl");
    }

    @ParameterizedTest
    @MethodSource("provideWire")
    void typedFieldsShouldBeNonNull(Function<Bytes<byte[]>, Wire> wireConstruction) {
        final Bytes<byte[]> bytes = Bytes.from("!net.openhft.chronicle.wire.AbstractUntypedFieldShouldBeNull$Holder {\n" +
                "  a: !AImpl {\n" +
                "  }\n" +
                "}");
        final Wire textWire = wireConstruction.apply(bytes);

        final Holder holder = textWire.getValueIn().object(Holder.class);

        System.out.println("holder.a = " + holder.a);

        assertNotNull(holder.a);
    }

    @ParameterizedTest
    @MethodSource("provideWire")
    void untypedFieldsShouldBeNull(Function<Bytes<byte[]>, Wire> wireConstruction) {
        final Bytes<byte[]> bytes = Bytes.from("!net.openhft.chronicle.wire.AbstractUntypedFieldShouldBeNull$Holder {\n" +
                "  a: {\n" +
                "  }\n" +
                "}");
        final Wire textWire = wireConstruction.apply(bytes);

        final Holder holder = textWire.getValueIn().object(Holder.class);

        assertNull(holder.a);
    }

    @ParameterizedTest
    @MethodSource("provideWire")
    void missingAliasesShouldLogWarnings(Function<Bytes<byte[]>, Wire> wireConstruction) {
        final Bytes<byte[]> bytes = Bytes.from("!net.openhft.chronicle.wire.AbstractUntypedFieldShouldBeNull$Holder {\n" +
                "  a: !MissingAlias {\n" +
                "  }\n" +
                "}");
        final Wire textWire = wireConstruction.apply(bytes);

        expectException("Ignoring exception and setting field 'a' to null");
        expectException("Cannot find a class for MissingAlias are you missing an alias?");
        assertNull(textWire.getValueIn().object(Holder.class).a);
    }

    static abstract class A {
    }

    private static final class AImpl extends A {
    }

    private static final class Holder {
        A a;
    }

    static Stream<Function<Bytes<byte[]>, Wire>> provideWire() {
        return Stream.of(
                JSONWire::new,
                TextWire::new
        );
    }


}
