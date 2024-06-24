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

    // Provide different wire format factories (JSON, Text, and YAML) for parameterized tests
    static Stream<Function<Bytes<byte[]>, Wire>> provideWire() {
        return Stream.of(
                JSONWire::new,
                TextWire::new,
                YamlWire::new
        );
    }

    // Add an alias for AImpl before each test
    @BeforeEach
    void beforeEach() {
        ClassAliasPool.CLASS_ALIASES.addAlias(AImpl.class, "AImpl");
    }

    // Parameterized test to verify typed fields are not null
    @ParameterizedTest
    @MethodSource("provideWire")
    void typedFieldsShouldBeNonNull(Function<Bytes<byte[]>, Wire> wireConstruction) {
        final Bytes<byte[]> bytes = Bytes.from("" +
                "!net.openhft.chronicle.wire.AbstractUntypedFieldShouldBeNull$Holder {\n" +
                "  \"a\": !AImpl {\n" +
                "  }," +
                "  \"b\": \"Impl\",\n" +
                "}");
        final Wire textWire = wireConstruction.apply(bytes);

        final Holder holder = textWire.getValueIn().object(Holder.class);

        System.out.println("holder.a = " + holder.a);

        // Assertion to check if the typed field is not null
        assertNotNull(holder.a);
    }

    // Parameterized test to verify untyped fields are null
    @ParameterizedTest
    @MethodSource("provideWire")
    void untypedFieldsShouldBeNull(Function<Bytes<byte[]>, Wire> wireConstruction) {
        final Bytes<byte[]> bytes = Bytes.from("!net.openhft.chronicle.wire.AbstractUntypedFieldShouldBeNull$Holder {\n" +
                "  \"a\": {\n" +
                "  }," +
                "  \"b\": \"Abstract\",\n" +
                "}");
        final Wire textWire = wireConstruction.apply(bytes);

        final Holder holder = textWire.getValueIn().object(Holder.class);

        // Assertion to check if the untyped field is null
        assertNull(holder.a);
    }

    // Parameterized test to ensure that missing aliases result in warnings
    @ParameterizedTest
    @MethodSource("provideWire")
    void missingAliasesShouldLogWarnings(Function<Bytes<byte[]>, Wire> wireConstruction) {
        final Bytes<byte[]> bytes = Bytes.from("!net.openhft.chronicle.wire.AbstractUntypedFieldShouldBeNull$Holder {\n" +
                "  a: !MissingAlias {\n" +
                "  }\n" +
                "}");
        final Wire textWire = wireConstruction.apply(bytes);

        // Expect certain exception messages to be logged
        expectException("Ignoring exception and setting field 'a' to null");
        expectException("Cannot find a class for MissingAlias are you missing an alias?");
        final ValueIn valueIn = textWire.getValueIn();

        // Assertion to check if the field with missing alias is null
        assertNull(valueIn.object(Holder.class).a);
    }

    // Abstract base class for testing
    static abstract class A {
    }

    // Implementation of the abstract base class
    private static final class AImpl extends A {
    }

    // Holder class to hold instances of type A
    private static final class Holder {
        A a;
        String b;
    }

}
