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
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.Validatable;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Arrays.asList;
import static net.openhft.chronicle.wire.WireType.TEXT;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class WiresTest extends WireTestCommon {

    private final BytesContainer container1 = new BytesContainer();
    private final BytesContainer container2 = new BytesContainer();

    @Override
    public void preAfter() {
        container1.bytesField.releaseLast();
        container2.bytesField.releaseLast();
    }

    @Test
    public void defaultCompilerOptions() throws Exception {
        Field compiler = Jvm.getField(Wires.class, "CACHED_COMPILER");
        compiler.set(null, null);
        Wires.loadFromJava(this.getClass().getClassLoader(), this.getClass().getName(), "");
        assertNotNull(compiler.get(null));
        List<String> options = Jvm.getValue(compiler.get(null), "options");

        assertTrue(options.containsAll(asList("-g", "-nowarn")));
        assertEquals(2, options.size());
    }

    @Test
    public void customCompilerOptions() throws Exception {
        Field compiler = Jvm.getField(Wires.class, "CACHED_COMPILER");
        compiler.set(null, null);
        System.setProperty("compiler.options", "-g -parameters");
        Wires.loadFromJava(this.getClass().getClassLoader(), this.getClass().getName(), "");
        assertNotNull(compiler.get(null));
        List<String> options = Jvm.getValue(compiler.get(null), "options");

        assertTrue(options.containsAll(asList("-g", "-parameters")));
        assertEquals(2, options.size());

        System.clearProperty("compiler.options");
    }

    @Test
    public void textWireNumberTest() {
        Assert.assertTrue(Double.isNaN(TEXT.apply(Bytes.from("NaN")).getValueIn().float64()));
        Assert.assertTrue(Double.isInfinite(TEXT.apply(Bytes.from("Infinity")).getValueIn().float64()));
        Assert.assertTrue(Double.isInfinite(TEXT.apply(Bytes.from("-Infinity")).getValueIn().float64()));

        // -0.0 is sent to denote and error
        Assert.assertEquals(-0.0, TEXT.apply(Bytes.from("''")).getValueIn().float64(), 0.0);

        // -0.0 is sent to denote and error
        Assert.assertEquals(-0.0, TEXT.apply(Bytes.from("Broken")).getValueIn().float64(), 0);

        // there is no number after the zero so it is assumed ot be 1e0
        Assert.assertEquals(1, TEXT.apply(Bytes.from("1e")).getValueIn().float64(), 0);
    }

    @Test
    public void resetShouldClearBytes() {

        container1.bytesField.clear().append("value1");
        container2.bytesField.clear().append("value2");

        Wires.reset(container1);
        Wires.reset(container2);

        container1.bytesField.clear().append("value1");
        assertEquals("", container2.bytesField.toString());
    }

    @Test
    public void resetShouldClearArbitraryMutableFields() {
        StringBuilderContainer container1 = new StringBuilderContainer();
        container1.stringBuilder.setLength(0);
        container1.stringBuilder.append("value1");

        StringBuilderContainer container2 = new StringBuilderContainer();
        container2.stringBuilder.setLength(0);
        container2.stringBuilder.append("value2");

        Wires.reset(container1);
        Wires.reset(container2);

        container1.stringBuilder.append("value1");

        assertEquals("", container2.stringBuilder.toString());
    }

    @Test
    public void copyToShouldMutateBytes() {
        BytesContainerMarshallable container1 = new BytesContainerMarshallable();
        container1.bytesField.append("1");
        container1.bytesField.append("2");
        BytesContainerMarshallable container2 = new BytesContainerMarshallable();
        Bytes<?> container2Bytes = container2.bytesField;
        Wires.copyTo(container1, container2);
        assertEquals(container2Bytes, container2.bytesField);
        assertEquals("12", container2.bytesField.toString());
    }

    @Test
    public void unknownType() throws NoSuchFieldException {
        Wires.GENERATE_TUPLES = true;

        Marshallable marshallable = Wires.tupleFor(Marshallable.class, "UnknownType");
        marshallable.setField("one", 1);
        marshallable.setField("two", 2.2);
        marshallable.setField("three", "three");
        String toString = marshallable.toString();
        assertEquals("!UnknownType {\n" +
                "  one: !int 1,\n" +
                "  two: 2.2,\n" +
                "  three: three\n" +
                "}\n", toString);
        Object o = Marshallable.fromString(toString);
        assertEquals(toString, o.toString());
    }

    @Test(expected = ClassNotFoundRuntimeException.class)
    public void unknownType2Throws2() {
        Wires.GENERATE_TUPLES = false;

        String text = "!FourValues {\n" +
                "  string: Hello,\n" +
                "  num: 123,\n" +
                "  big: 1E6,\n" +
                "  also: extra\n" +
                "}\n";
        ThreeValues tv = Marshallable.fromString(ThreeValues.class, text);
        assertEquals(text, tv.toString());
        assertEquals("Hello", tv.string());
        tv.string("Hello World");
        assertEquals("Hello World", tv.string());

        assertEquals(123, tv.num());
        tv.num(1234);
        assertEquals(1234, tv.num());

        assertEquals(1e6, tv.big(), 0.0);
        tv.big(0.128);
        assertEquals(0.128, tv.big(), 0.0);

        assertEquals("!FourValues {\n" +
                "  string: Hello World,\n" +
                "  num: !int 1234,\n" +
                "  big: 0.128,\n" +
                "  also: extra\n" +
                "}\n", tv.toString());
    }

    @Test
    public void recordAsYaml() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Says says = Wires.recordAsYaml(Says.class, ps);
        says.say("One");
        says.say("Two");
        says.say("Three");

        assertEquals("" +
                "---\n" +
                "say: One\n" +
                "...\n" +
                "---\n" +
                "say: Two\n" +
                "...\n" +
                "---\n" +
                "say: Three\n" +
                "...\n",
                new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
    }
    @Test
    public void replay() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
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
                "...\n",says);

        assertEquals("" +
                "---\n" +
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
                "...\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
    }

    @Test
    public void deepCopyNotBoundToThread() {
        BytesContainerMarshallable bcm = new BytesContainerMarshallable();
        bcm.bytesField.append("Hello");
        assumeFalse(Jvm.getValue(bcm.bytesField, "usedByThread") == null);
        BytesContainerMarshallable bcm2 = bcm.deepCopy();
        assertNull(Jvm.getValue(bcm2.bytesField, "usedByThread"));
    }

    interface ThreeValues {
        ThreeValues string(String s);

        String string();

        ThreeValues num(int n);

        int num();

        ThreeValues big(double d);

        double big();
    }

    private static final class BytesContainer {
        Bytes<?> bytesField = Bytes.allocateElasticOnHeap(64);
    }

    private static final class BytesContainerMarshallable extends SelfDescribingMarshallable {
        Bytes<?> bytesField = Bytes.allocateElasticOnHeap(64);
    }

    private static final class StringBuilderContainer {
        StringBuilder stringBuilder = new StringBuilder();
    }

    @Test
    public void copyTo() {
        OneTwoFour o124 = new OneTwoFour(11, 222, 44444);
        TwoFourThree o243 = new TwoFourThree(2, 4, 3);
        Wires.copyTo(o124, o243);
        // source overwrites fields in dest. As source does not have a "three" field, that field is
        // defaulted to 0 (as overwrite=true in Marshallable.readMarshallable)
        assertEquals("!net.openhft.chronicle.wire.WiresTest$TwoFourThree {\n" +
                "  two: 222,\n" +
                "  four: 44444,\n" +
                "  three: 0\n" +
                "}\n", o243.toString());
    }

    @Test
    public void copyToIncompleteValidation() {
        OneTwoFour o124 = new OneTwoFour(11, 222, 44444);
        TwoFourThreeValidatable o243 = new TwoFourThreeValidatable(2, 4, 3);
        // o243's validate method used to be called and would blow up as o243 was incomplete.
        // Using copyTo to partially hydrate an object is perfectly valid
        Wires.copyTo(o124, o243);
    }

    @Test
    public void copyToContainsBytesMarshallable() {
        ContainsBM containsBM = new ContainsBM(new BasicBytesMarshallable("Harold"));
        ContainsBM containsBM2 = new ContainsBM(null);
        Wires.copyTo(containsBM, containsBM2);
        assertEquals(containsBM.inner.name, containsBM2.inner.name);
    }

    @Test
    public void deepCopyWillWorkWhenDynamicEnumIsAnnotatedAsMarshallable() {
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
        assertEquals(thing2, thingCopy);
    }

    @SuppressWarnings("deprecation")
    static class Thing extends AbstractEventCfg<Thing> {
        @AsMarshallable
        DynamicEnum dee1;
        String someString;
    }

    @SuppressWarnings("deprecation")
    enum EnumThing implements DynamicEnum {
        ONE,
        TWO;
    }

    static class OneTwoFour extends BytesInBinaryMarshallable {
        long one, two, four;

        OneTwoFour(long one, long two, long four) {
            this.one = one;
            this.two = two;
            this.four = four;
        }
    }

    static class TwoFourThree extends BytesInBinaryMarshallable {
        long two, four, three;

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
        String name;

        BasicBytesMarshallable(String name) {
            this.name = name;
        }
    }

    static class ContainsBM extends BytesInBinaryMarshallable {
        BasicBytesMarshallable inner;

        ContainsBM(BasicBytesMarshallable inner) {
            this.inner = inner;
        }
    }

    interface Says {
        void say(String word);
    }
}
