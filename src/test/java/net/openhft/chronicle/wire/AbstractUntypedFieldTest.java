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
import net.openhft.chronicle.core.io.Validatable;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import net.openhft.chronicle.core.util.ReadResolvable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static org.junit.Assert.*;

/** Uses JUnit 4 so WireTestCommon's resource and exception lifecycle also runs. */
@RunWith(Parameterized.class)
public class AbstractUntypedFieldTest extends WireTestCommon {
    private final String format;
    private final Function<Bytes<byte[]>, Wire> wireConstruction;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> formats() {
        return Arrays.asList(new Object[][] {
                {"JSON", (Function<Bytes<byte[]>, Wire>) JSONWire::new},
                {"Text", (Function<Bytes<byte[]>, Wire>) TextWire::new},
                {"YAML", (Function<Bytes<byte[]>, Wire>) YamlWire::new}
        });
    }

    public AbstractUntypedFieldTest(String name, Function<Bytes<byte[]>, Wire> wireConstruction) {
        this.format = name;
        this.wireConstruction = wireConstruction;
    }

    @Before
    public void aliases() {
        ClassAliasPool.CLASS_ALIASES.addAlias(AImpl.class, "ResumptionAImpl");
        ClassAliasPool.CLASS_ALIASES.addAlias(ThrowingA.class, "ResumptionThrowingA");
        ClassAliasPool.CLASS_ALIASES.addAlias(MissingClassA.class, "ResumptionMissingClassA");
        ClassAliasPool.CLASS_ALIASES.addAlias(NestedAllocationA.class, "ResumptionNestedAllocationA");
        ClassAliasPool.CLASS_ALIASES.addAlias(ReadResolveA.class, "ResumptionReadResolveA");
        ClassAliasPool.CLASS_ALIASES.addAlias(ValidatingReadResolveA.class, "ResumptionValidatingReadResolveA");
        Wires.GENERATE_TUPLES = false;
    }

    @Test
    public void typedFieldPreservesFollowingFieldAndObject() {
        verifyResumption("!ResumptionAImpl { \"value\": 17 }", true);
    }

    @Test
    public void untypedAbstractFieldPreservesFollowingFieldAndObject() {
        expectException("Ignoring exception and setting field 'a' to null");
        verifyResumption("{ \"nested\": { \"b\": \"inside\" }, \"items\": [1, 2] }", false);
    }

    @Test
    public void ignoredMissingAliasPreservesFollowingFieldAndObject() {
        boolean previous = Wires.THROW_CNFRE;
        Wires.THROW_CNFRE = false;
        try {
            expectException("YAML".equals(format)
                    ? "Unable to find ResumptionMissingAlias"
                    : "Cannot find a class for ResumptionMissingAlias are you missing an alias?");
            expectException("Ignoring exception and setting field 'a' to null");
            verifyResumption("!ResumptionMissingAlias { \"nested\": { \"b\": \"inside\" }, \"items\": [1, 2] }", false);
        } finally {
            Wires.THROW_CNFRE = previous;
        }
    }

    @Test
    public void missingAliasStillThrowsWhenEnabled() {
        Wires.THROW_CNFRE = true;
        final Bytes<byte[]> bytes = Bytes.from("{ \"a\": !ResumptionMissingAlias {}, \"b\": \"after\" }");
        try {
            final Wire wire = wireConstruction.apply(bytes);
            ClassNotFoundRuntimeException failure = assertThrows(ClassNotFoundRuntimeException.class,
                    () -> wire.getValueIn().object(Holder.class));
            assertTrue(failure.getMessage(), failure.getMessage().contains("ResumptionMissingAlias"));
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void unrelatedTypeFailureIsNotIgnored() {
        final Bytes<byte[]> bytes = Bytes.from("{ \"a\": !ResumptionThrowingA {}, \"b\": \"after\" }");
        try {
            final Wire wire = wireConstruction.apply(bytes);
            ClassCastException failure = assertThrows(ClassCastException.class,
                    () -> wire.getValueIn().object(Holder.class));
            assertEquals("unrelated type failure", failure.getMessage());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void knownReaderClassFailureIsNotIgnored() {
        ClassNotFoundRuntimeException failure = knownReaderFailure("!ResumptionMissingClassA {}");
        assertEquals("failure inside known reader", failure.getCause().getMessage());
    }

    @Test
    public void knownReaderNestedAllocationFailureIsNotIgnored() {
        ClassNotFoundRuntimeException failure = knownReaderFailure(
                "!ResumptionNestedAllocationA { \"child\": {} }");
        assertTrue(failure.getCause().getMessage(),
                failure.getCause().getMessage().contains("failed to create instance"));
    }

    @Test
    public void knownReadResolveAllocationFailureIsNotIgnored() {
        ClassNotFoundRuntimeException failure = knownReaderFailure("!ResumptionReadResolveA {}");
        assertTrue(failure.getCause().getMessage(),
                failure.getCause().getMessage().contains("failed to create instance"));
    }

    @Test
    public void knownValidationAllocationFailureIsNotIgnored() {
        ClassNotFoundRuntimeException failure = knownReaderFailure("!ResumptionValidatingReadResolveA {}");
        assertTrue(failure.getCause().getMessage(),
                failure.getCause().getMessage().contains("failed to create instance"));
    }

    private ClassNotFoundRuntimeException knownReaderFailure(String value) {
        boolean previous = Wires.THROW_CNFRE;
        Wires.THROW_CNFRE = false;
        final Bytes<byte[]> bytes = Bytes.from("{ \"a\": " + value + ", \"b\": \"after\" }");
        try {
            final Wire wire = wireConstruction.apply(bytes);
            return assertThrows(ClassNotFoundRuntimeException.class,
                    () -> wire.getValueIn().object(Holder.class));
        } finally {
            Wires.THROW_CNFRE = previous;
            bytes.releaseLast();
        }
    }

    private void verifyResumption(String firstValue, boolean typed) {
        final Bytes<byte[]> bytes = Bytes.from("{ \"a\": " + firstValue + ", \"b\": \"after\" }\n"
                + "{ \"a\": !ResumptionAImpl { \"value\": 29 }, \"b\": \"next\" }");
        try {
            final Wire wire = wireConstruction.apply(bytes);
            Holder first = wire.getValueIn().object(Holder.class);
            if (typed) {
                assertNotNull(first.a);
                assertEquals(17, ((AImpl) first.a).value);
            } else {
                assertNull(first.a);
            }
            assertEquals("after", first.b);
            wire.consumePadding();
            Holder next = wire.getValueIn().object(Holder.class);
            assertNotNull(next.a);
            assertEquals(29, ((AImpl) next.a).value);
            assertEquals("next", next.b);
            wire.consumePadding();
            assertEquals("Both complete objects must be consumed", 0, bytes.readRemaining());
        } finally {
            bytes.releaseLast();
        }
    }

    static abstract class A {
    }

    private static final class AImpl extends A {
        int value;
    }

    private static final class ThrowingA extends A implements ReadMarshallable {
        @Override
        public void readMarshallable(WireIn wire) {
            throw new ClassCastException("unrelated type failure");
        }
    }

    private static final class MissingClassA extends A implements ReadMarshallable {
        @Override
        public void readMarshallable(WireIn wire) {
            throw new ClassNotFoundRuntimeException(new ClassNotFoundException("failure inside known reader"));
        }
    }

    private static final class NestedAllocationA extends A implements ReadMarshallable {
        @Override
        public void readMarshallable(WireIn wire) {
            wire.read("child").object(A.class);
        }
    }

    private static final class ReadResolveA extends A implements ReadMarshallable, ReadResolvable<A> {
        @Override
        public void readMarshallable(WireIn wire) {
        }

        @Override
        public A readResolve() {
            failNestedAllocation();
            return this;
        }
    }

    private static final class ValidatingReadResolveA extends A implements ReadMarshallable, ReadResolvable<A> {
        @Override
        public void readMarshallable(WireIn wire) {
        }

        @Override
        public A readResolve() {
            // This new value is validated only after object1 returns it.
            return new ValidatingA();
        }
    }

    private static final class ValidatingA extends A implements Validatable {
        @Override
        public void validate() {
            failNestedAllocation();
        }
    }

    private static void failNestedAllocation() {
        Bytes<byte[]> bytes = Bytes.from("{}");
        try {
            new TextWire(bytes).getValueIn().object(A.class);
        } finally {
            bytes.releaseLast();
        }
    }

    private static final class Holder {
        A a;
        String b;
    }
}
