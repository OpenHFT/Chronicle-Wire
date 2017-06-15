package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by rob on 14/06/2017.
 */
public class WiresTest {
    public static class BytesContainer {
        @MutableField
        Bytes bytesField = Bytes.elasticByteBuffer();
    }

    public static class StringBuilderContainer {
        @MutableField
        StringBuilder stringBuilder = new StringBuilder();
    }

    @Test
    public void resetShouldClearBytes() {
        BytesContainer container1 = new BytesContainer();
        container1.bytesField.clear().append("value1");

        BytesContainer container2 = new BytesContainer();
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
}