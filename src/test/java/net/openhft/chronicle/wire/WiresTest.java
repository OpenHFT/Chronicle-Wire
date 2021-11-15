package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesMarshallable;
import org.junit.Assert;
import org.junit.Test;

import static net.openhft.chronicle.wire.WireType.TEXT;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("rawtypes")
public class WiresTest extends WireTestCommon {

    private final BytesContainer container1 = new BytesContainer();
    private final BytesContainer container2 = new BytesContainer();

    @Override
    public void preAfter() {
        container1.bytesField.releaseLast();
        container2.bytesField.releaseLast();
    }

    @Test
    public void textWireNumberTest() {
        Assert.assertTrue(Double.isNaN(TEXT.apply(Bytes.from("NaN")).getValueIn().float64()));
        Assert.assertTrue(Double.isInfinite(TEXT.apply(Bytes.from("Infinity")).getValueIn().float64()));
        Assert.assertTrue(Double.isInfinite(TEXT.apply(Bytes.from("-Infinity")).getValueIn().float64()));

        // -0.0 is sent to denote and error
        Assert.assertEquals(-0.0, TEXT.apply(Bytes.from("'1.0'")).getValueIn().float64(), 0);

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
        Bytes container2Bytes = container2.bytesField;
        Wires.copyTo(container1, container2);
        assertEquals(container2Bytes, container2.bytesField);
        assertEquals("12", container2.bytesField.toString());
        container2.bytesField.append("123");
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

    @Test
    public void unknownType2() {
        Wires.GENERATE_TUPLES = true;

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

    interface ThreeValues {
        ThreeValues string(String s);

        String string();

        ThreeValues num(int n);

        int num();

        ThreeValues big(double d);

        double big();
    }

    private static final class BytesContainer {
        Bytes bytesField = Bytes.allocateElasticOnHeap(64);
    }

    private static final class BytesContainerMarshallable extends SelfDescribingMarshallable {
        Bytes bytesField = Bytes.allocateElasticOnHeap(64);
    }

    private static final class StringBuilderContainer {
        StringBuilder stringBuilder = new StringBuilder();
    }

    @Test
    public void copyTo() {
        OneTwoFour o124 = new OneTwoFour(11, 222, 44444);
        TwoFourThree o243 = new TwoFourThree(0, 0, 0);
        Wires.copyTo(o124, o243);
        assertEquals("!net.openhft.chronicle.wire.WiresTest$TwoFourThree {\n" +
                "  two: 222,\n" +
                "  four: 44444,\n" +
                "  three: 0\n" +
                "}\n", o243.toString());
    }

    @Test
    public void copyToContainsBytesMarshallable() {
        ContainsBM containsBM = new ContainsBM(new BasicBytesMarshallable("Harold"));
        ContainsBM containsBM2 = new ContainsBM(null);
        Wires.copyTo(containsBM, containsBM2);
        assertEquals(containsBM.inner.name, containsBM2.inner.name);
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
}