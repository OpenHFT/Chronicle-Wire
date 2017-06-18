package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by rob on 14/06/2017.
 */
public class WiresTest {

    private final BytesContainer container1 = new BytesContainer();
    private final BytesContainer container2 = new BytesContainer();

    public static class BytesContainer {
        @MutableField
        Bytes bytesField = Bytes.elasticByteBuffer();
    }

    public static class BytesContainerMarshallable extends AbstractMarshallable {
        Bytes bytesField = Bytes.elasticByteBuffer();
    }

    public static class StringBuilderContainer {
        @MutableField
        StringBuilder stringBuilder = new StringBuilder();
    }

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

        Assert.assertEquals("", container2.bytesField.toString());
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

        Assert.assertEquals("", container2.stringBuilder.toString());
    }

    @Test
    public void copyToShouldMutateBytes() {
        BytesContainerMarshallable container1 = new BytesContainerMarshallable();
        container1.bytesField.append("1");
        container1.bytesField.append("2");
        BytesContainerMarshallable container2 = new BytesContainerMarshallable();
        Bytes container2Bytes = container2.bytesField;
        Wires.copyTo(container1, container2);
        Assert.assertEquals(container2Bytes, container2.bytesField);
        Assert.assertEquals("12", container2.bytesField.toString());
        container2.bytesField.append("123");
    }
}