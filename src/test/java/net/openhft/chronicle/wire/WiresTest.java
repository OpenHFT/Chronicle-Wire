package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
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

    public static class BytesContainer {
        @MutableField
        Bytes bytesField = Bytes.elasticHeapByteBuffer(64);
    }

    public static class BytesContainerMarshallable extends AbstractMarshallable {
        Bytes bytesField = Bytes.elasticHeapByteBuffer(64);
    }

    public static class StringBuilderContainer {
        @MutableField
        StringBuilder stringBuilder = new StringBuilder();
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
}