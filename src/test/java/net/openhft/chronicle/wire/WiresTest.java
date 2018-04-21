package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/*
 * Created by rob on 14/06/2017.
 */
public class WiresTest {

    private final BytesContainer container1 = new BytesContainer();
    private final BytesContainer container2 = new BytesContainer();

    @After
    public void after() throws Exception {
        container1.bytesField.release();
        container2.bytesField.release();
    }

    @Test
    public void name() {
        System.out.println(Integer.toHexString(Wires.END_OF_DATA));
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
    public void shouldDemonstrateMutableFieldIssue() {
        final MutableContainer container1 = new MutableContainer();
        final MutableContainer container2 = new MutableContainer();

        container1.mutableClass = new MutableClass();
        container1.mutableClass.answer = 42;
        container1.mutableClass.question = "meaning";

        container2.mutableClass = new MutableClass();
        container2.mutableClass.answer = 120;
        container2.mutableClass.question = "5!";

        Wires.reset(container1);
        Wires.reset(container2);

        container1.mutableClass.question = "safe mutation?";

        assertNotSame(container1.mutableClass.question, container2.mutableClass.question);
        assertNotEquals("safe mutation?", container2.mutableClass.question);
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
        Bytes bytesField = Bytes.elasticHeapByteBuffer(64);
    }

    private static final class BytesContainerMarshallable extends AbstractMarshallable {
        Bytes bytesField = Bytes.elasticHeapByteBuffer(64);
    }

    private static final class StringBuilderContainer {
        StringBuilder stringBuilder = new StringBuilder();
    }

    private static final class MutableContainer implements ResetOverride {
        MutableClass mutableClass = new MutableClass();

        @Override
        public void onReset() {
            mutableClass = new MutableClass();
        }
    }

    private static final class MutableClass {
        int answer;
        String question = "";
    }
}