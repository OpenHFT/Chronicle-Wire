/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.Validatable;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Arrays.asList;
import static net.openhft.chronicle.wire.WireType.TEXT;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings({"deprecation", "removal"})
class WiresTest extends WireTestCommon {

    private final BytesContainer container1 = new BytesContainer();
    private final BytesContainer container2 = new BytesContainer();

    @Override
    void preAfter() {
        container1.bytesField.releaseLast();
        container2.bytesField.releaseLast();
    }

    @Test
    @DisplayName("Default compiler options include debug flags")
    void defaultCompilerOptions() throws Exception {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for default compiler options test");

        Field compiler = Jvm.getField(Wires.class, "CACHED_COMPILER");
        compiler.set(null, null);
        Wires.loadFromJava(this.getClass().getClassLoader(), this.getClass().getName(), "");
        assertNotNull(compiler.get(null), "wires.loadFromJava should initialize cached compiler instance");
        List<String> options = Jvm.getValue(compiler.get(null), "options");

        assertTrue(options.containsAll(asList("-g", "-nowarn")), "default compiler options should include debug symbols and suppress warnings");
        assertEquals(2, options.size(), "default compiler options should contain exactly two flags");
    }

    @Test
    @DisplayName("Custom compiler options include parameter names")
    void customCompilerOptions() throws Exception {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for custom compiler options test");

        Field compiler = Jvm.getField(Wires.class, "CACHED_COMPILER");
        compiler.set(null, null);
        System.setProperty("compiler.options", "-g -parameters");
        Wires.loadFromJava(this.getClass().getClassLoader(), this.getClass().getName(), "");
        assertNotNull(compiler.get(null), "wires.loadFromJava should initialize cached compiler with custom options");
        List<String> options = Jvm.getValue(compiler.get(null), "options");

        assertTrue(options.containsAll(asList("-g", "-parameters")), "custom compiler options should include debug symbols and parameter names");
        assertEquals(2, options.size(), "custom compiler options should contain exactly two flags");

        System.clearProperty("compiler.options");
    }

    @Test
    @DisplayName("Text wire parses NaN and Infinity values")
    void textWireNumberTest() {
        Assertions.assertTrue(Double.isNaN(TEXT.apply(Bytes.from("NaN")).getValueIn().float64()), "text wire should parse 'NaN' as double NaN");
        Assertions.assertTrue(Double.isInfinite(TEXT.apply(Bytes.from("Infinity")).getValueIn().float64()), "text wire should parse 'Infinity' as positive infinity");
        Assertions.assertTrue(Double.isInfinite(TEXT.apply(Bytes.from("-Infinity")).getValueIn().float64()), "text wire should parse '-Infinity' as negative infinity");

        // -0.0 is sent to denote and error
        Assertions.assertEquals(-0.0, TEXT.apply(Bytes.from("''")).getValueIn().float64(), 0.0, "text wire should return -0.0 for empty string to denote error");

        // -0.0 is sent to denote and error
        Assertions.assertEquals(-0.0, TEXT.apply(Bytes.from("Broken")).getValueIn().float64(), 0, "text wire should return -0.0 for invalid number string to denote error");

        // there is no number after the zero so it is assumed ot be 1e0
        Assertions.assertEquals(1, TEXT.apply(Bytes.from("1e")).getValueIn().float64(), 0, "text wire should parse incomplete exponent '1e' as 1e0");
    }

    @Test
    @DisplayName("Wires reset clears bytes fields safely")
    void resetShouldClearBytes() {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for bytes reset test");

        container1.bytesField.clear().append("value1");
        container2.bytesField.clear().append("value2");

        Wires.reset(container1);
        Wires.reset(container2);

        container1.bytesField.clear().append("value1");
        assertEquals("", container2.bytesField.toString(), "wires.reset should prevent cross-contamination between independent container instances");
    }

    @Test
    @DisplayName("Wires reset clears StringBuilder fields safely")
    void resetShouldClearArbitraryMutableFields() {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for StringBuilder reset test");

        StringBuilderContainer container1 = new StringBuilderContainer();
        container1.stringBuilder.setLength(0);
        container1.stringBuilder.append("value1");

        StringBuilderContainer container2 = new StringBuilderContainer();
        container2.stringBuilder.setLength(0);
        container2.stringBuilder.append("value2");

        Wires.reset(container1);
        Wires.reset(container2);

        container1.stringBuilder.append("value1");

        assertEquals("", container2.stringBuilder.toString(), "wires.reset should prevent cross-contamination between arbitrary mutable fields like StringBuilder");
    }

    @Test
    @DisplayName("CopyTo mutates existing bytes instances in place")
    void copyToShouldMutateBytes() {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for bytes copyTo test");

        BytesContainerMarshallable container1 = new BytesContainerMarshallable();
        container1.bytesField.append('1');
        container1.bytesField.append('2');
        BytesContainerMarshallable container2 = new BytesContainerMarshallable();
        Bytes<?> container2Bytes = container2.bytesField;
        Wires.copyTo(container1, container2);
        assertEquals(container2Bytes, container2.bytesField, "wires.copyTo should mutate existing bytes instance rather than replacing it");
        assertEquals("12", container2.bytesField.toString(), "wires.copyTo should copy bytes field content to destination container");
    }

    @Test
    @DisplayName("TupleFor handles unknown types with tuples")
    void unknownType() throws NoSuchFieldException {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for unknown type tuple test");

        Marshallable marshallable = Wires.tupleFor(Marshallable.class, "UnknownType");
        marshallable.setField("one", 1);
        marshallable.setField("two", 2.2);
        marshallable.setField("three", "three");
        String toString = marshallable.toString();
        assertEquals("!UnknownType {\n" +
                "  one: !int 1,\n" +
                "  two: 2.2,\n" +
                "  three: three\n" +
                "}\n", toString, "wires.tupleFor should create unknown type tuple with dynamic fields");

        Wires.setGenerateTuples(true);

        Object o = Marshallable.fromString(toString);
        assertEquals(toString, o.toString(), "wires.tupleFor should generate marshallable tuple with matching toString output");
    }

    @Test
    @DisplayName("Unknown types throw when tuples disabled")
    void unknownType2Throws2() {
        assertThrows(ClassNotFoundRuntimeException.class, () -> {
            Wires.setGenerateTuples(false);

            String text = "!FourValues {\n" +
                    "  string: Hello,\n" +
                    "  num: 123,\n" +
                    "  big: 1E6,\n" +
                    "  also: extra\n" +
                    "}\n";
            ThreeValues tv = Marshallable.fromString(ThreeValues.class, text);
            assertEquals(text, tv.toString(), "generated tuple should preserve extra fields from yaml including type alias");
            assertEquals("Hello", tv.string(), "generated tuple should read string field from yaml");
            tv.string("Hello World");
            assertEquals("Hello World", tv.string(), "generated tuple should support modifying string field");

            assertEquals(123, tv.num(), "generated tuple should read num field from yaml");
            tv.num(1234);
            assertEquals(1234, tv.num(), "generated tuple should support modifying num field");

            assertEquals(1e6, tv.big(), 0.0, "generated tuple should read big field from yaml");
            tv.big(0.128);
            assertEquals(0.128, tv.big(), 0.0, "generated tuple should support modifying big field");

            assertEquals("!FourValues {\n" +
                    "  string: Hello World,\n" +
                    "  num: !int 1234,\n" +
                    "  big: 0.128,\n" +
                    "  also: extra\n" +
                    "}\n", tv.toString(), "generated tuple should serialize modified fields and preserve extra fields with type alias");
        }, "Tuple generation should fail when disabled");
    }

    @Test
    @DisplayName("RecordAsYaml writes each call as document")
    void recordAsYaml() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = isoPrintStream(baos);
        Says says = Wires.recordAsYaml(Says.class, ps);
        says.say("One");
        says.say("Two");
        says.say("Three");

        assertEquals("---\n" +
                        "say: One\n" +
                        "...\n" +
                        "---\n" +
                        "say: Two\n" +
                        "...\n" +
                        "---\n" +
                        "say: Three\n" +
                        "...\n",
                new String(baos.toByteArray(), ISO_8859_1), "wires.recordAsYaml should output each method call as separate yaml document");
    }

    @Test
    @DisplayName("Replay appends YAML documents to output")
    void replay() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = isoPrintStream(baos);
        Says says = Wires.recordAsYaml(Says.class, ps);
        says.say("zero");
        Wires.replay("=" +
                "---\n" +
                "say: One\n" +
                "...\n" +
                "---\n" +
                "say: Two\n" +
                "...\n" +
                "---\n" +
                "say: Three\n" +
                "...\n", says);

        assertEquals("---\n" +
                "say: zero\n" +
                "...\n" +
                "---\n" +
                "say: One\n" +
                "...\n" +
                "---\n" +
                "say: Two\n" +
                "...\n" +
                "---\n" +
                "say: Three\n" +
                "...\n", new String(baos.toByteArray(), ISO_8859_1), "wires.replay should execute yaml documents on target method writer appending to existing output");
    }

    private static PrintStream isoPrintStream(ByteArrayOutputStream baos) {
        try {
            return new PrintStream(baos, true, ISO_8859_1.name());
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("ISO-8859-1 encoding should be available", e);
        }
    }

    @Test
    @DisplayName("Deep copy clears thread binding on bytes")
    void deepCopyNotBoundToThread() {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for BytesMarshallable copyTo test");
        BytesContainerMarshallable bcm = new BytesContainerMarshallable();
        bcm.bytesField.append("Hello");
        assumeFalse(Jvm.getValue(bcm.bytesField, "usedByThread") == null,
                "usedByThread must be set for deep copy test");
        BytesContainerMarshallable bcm2 = bcm.deepCopy();
        assertNull(Jvm.getValue(bcm2.bytesField, "usedByThread"), "deep copied bytes should not be bound to thread allowing safe transfer between threads");
    }

    @Test
    @DisplayName("CopyTo overwrites destination fields for marshallables")
    void copyTo() {
        OneTwoFour o124 = new OneTwoFour(11, 222, 44444);
        TwoFourThree o243 = new TwoFourThree(2, 4, 3);
        Wires.copyTo(o124, o243);
        // source overwrites fields in dest. As source does not have a "three" field, that field is
        // defaulted to 0 (as overwrite=true in Marshallable.readMarshallable)
        assertEquals("!net.openhft.chronicle.wire.WiresTest$TwoFourThree {\n" +
                "  two: 222,\n" +
                "  four: 44444,\n" +
                "  three: 0\n" +
                "}\n", o243.toString(), "wires.copyTo should copy matching fields and reset unmatched destination fields to default values");
    }

    @Test
    @DisplayName("CopyTo allows partial hydration with validation")
    void copyToIncompleteValidation() {
        OneTwoFour o124 = new OneTwoFour(11, 222, 44444);
        TwoFourThreeValidatable o243 = new TwoFourThreeValidatable(2, 4, 3);
        assertEquals("!net.openhft.chronicle.wire.WiresTest$TwoFourThreeValidatable {\n" +
                "  two: 2,\n" +
                "  four: 4,\n" +
                "  three: 3\n" +
                "}\n", o243.toString(), "validatable object should serialize initial state correctly");
        // Using copyTo to partially hydrate an object is perfectly valid
        Wires.copyTo(o124, o243);
        assertEquals("!net.openhft.chronicle.wire.WiresTest$TwoFourThreeValidatable {\n" +
                "  two: 222,\n" +
                "  four: 44444,\n" +
                "  three: 0\n" +
                "}\n", o243.toString(), "wires.copyTo should allow partial hydration even if result would fail validation");
    }

    @Test
    @DisplayName("CopyTo handles nested BytesMarshallable fields properly")
    void copyToContainsBytesMarshallable() {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for copyTo validation test");

        ContainsBM containsBM = new ContainsBM(new BasicBytesMarshallable("Harold"));
        ContainsBM containsBM2 = new ContainsBM(null);
        Wires.copyTo(containsBM, containsBM2);
        assertEquals(containsBM.inner.name, containsBM2.inner.name, "wires.copyTo should handle nested BytesMarshallable objects copying field values");
    }

    @Test
    @DisplayName("Deep copy preserves DynamicEnum marshallable fields")
    void deepCopyWillWorkWhenDynamicEnumIsAnnotatedAsMarshallable() {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for DynamicEnum deep copy test");

        ClassAliasPool.CLASS_ALIASES.addAlias(Thing.class, EnumThing.class);

        Thing thing2 = Marshallable.fromString(
                "!Thing {" +
                        "   eventTime: 2020-09-09T01:46:41,\n" +
                        "   dee1: !EnumThing {\n" +
                        "      name: ONE,\n" +
                        "   }\n" +
                        "   someString: bla bla,\n" +
                        "}\n");
        final Thing thingCopy = thing2.deepCopy();
        assertEquals(thing2, thingCopy, "deep copy should preserve DynamicEnum fields annotated with @AsMarshallable");
    }

    @SuppressWarnings("deprecation")
    enum EnumThing implements DynamicEnum {
        ONE,
        TWO
    }

    interface ThreeValues {
        ThreeValues string(String s);

        String string();

        ThreeValues num(int n);

        int num();

        ThreeValues big(double d);

        double big();
    }

    interface Says {
        void say(String word);
    }

    private static final class BytesContainer {
        final Bytes<?> bytesField = Bytes.allocateElasticOnHeap(64);
    }

    private static final class BytesContainerMarshallable extends SelfDescribingMarshallable {
        final Bytes<?> bytesField = Bytes.allocateElasticOnHeap(64);
    }

    private static final class StringBuilderContainer {
        final StringBuilder stringBuilder = new StringBuilder();
    }

    @SuppressWarnings("deprecation")
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class Thing extends AbstractEventCfg<Thing> {
        @AsMarshallable
        DynamicEnum dee1;
        String someString;
    }

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class OneTwoFour extends BytesInBinaryMarshallable {
        final long one;
        final long two;
        final long four;

        OneTwoFour(long one, long two, long four) {
            this.one = one;
            this.two = two;
            this.four = four;
        }
    }

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class TwoFourThree extends BytesInBinaryMarshallable {
        final long two;
        final long four;
        final long three;

        TwoFourThree(long two, long four, long three) {
            this.two = two;
            this.four = four;
            this.three = three;
        }
    }

    static class TwoFourThreeValidatable extends TwoFourThree implements Validatable {
        TwoFourThreeValidatable(long two, long four, long three) {
            super(two, four, three);
        }

        @Override
        public void validate() throws InvalidMarshallableException {
            if (three == 0) {
                throw new InvalidMarshallableException("three is 0");
            }
        }
    }

    static class BasicBytesMarshallable implements BytesMarshallable {
        final String name;

        BasicBytesMarshallable(String name) {
            this.name = name;
        }
    }

    static class ContainsBM extends BytesInBinaryMarshallable {
        final BasicBytesMarshallable inner;

        ContainsBM(BasicBytesMarshallable inner) {
            this.inner = inner;
        }
    }
}
