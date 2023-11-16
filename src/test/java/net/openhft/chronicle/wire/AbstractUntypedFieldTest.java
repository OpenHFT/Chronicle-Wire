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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class AbstractUntypedFieldTest extends WireTestCommon {

    static Stream<Function<Bytes<byte[]>, Wire>> provideWire() {
        return Stream.of(
                JSONWire::new,
                TextWire::new,
                YamlWire::new
        );
    }

    @BeforeEach
    void beforeEach() {
        ClassAliasPool.CLASS_ALIASES.addAlias(AImpl.class, "AImpl");
    }

    @ParameterizedTest
    @MethodSource("provideWire")
    void typedFieldsShouldBeNonNull(Function<Bytes<byte[]>, Wire> wireConstruction) {
        final Bytes<byte[]> bytes = Bytes.from("" +
                "!net.openhft.chronicle.wire.AbstractUntypedFieldShouldBeNull$Holder {\n" +
                "  a: !AImpl {\n" +
                "  }\n" +
                "}");
        final Wire textWire = wireConstruction.apply(bytes);

        assumeFalse(textWire instanceof JSONWire);

        final Holder holder = textWire.getValueIn().object(Holder.class);

        System.out.println("holder.a = " + holder.a);

        assertNotNull(holder.a);
    }

    @Test
    void typedFieldsShouldBeNonNullJsonWire() {
        final Bytes<byte[]> bytes = Bytes.from("" +
                "{ \"@net.openhft.chronicle.wire.AbstractUntypedFieldTest$Holder\": {" +
                " \"a\":{ \"@AImpl\": {" +
                "\"b\":true" +
                "} }" +
                "}}");
        final Wire textWire = new JSONWire(bytes).useTypes(true);

        final Holder holder = textWire.getValueIn().object(Holder.class);

        assertNotNull(holder.a);
        assertTrue(((AImpl) holder.a).b);
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
        final ValueIn valueIn = textWire.getValueIn();
        assertNull(valueIn.object(Holder.class).a);
    }

    static abstract class A {
    }

    private static final class AImpl extends A {
        boolean b;
    }

    private static final class Holder {
        A a;
    }


}
