package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.Base40IntConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class TestIntConversion {
    public static char SEPARATOR = '/';
    private IntConverter intConverter;
    
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                new Object[]{Base32IntConverter.INSTANCE},
                new Object[]{Base40IntConverter.INSTANCE},
                new Object[]{Base85IntConverter.INSTANCE},
                new Object[]{Base256IntConverter.INSTANCE}
        });
    }

    public TestIntConversion(IntConverter intConverter) {
        this.intConverter = intConverter;
    }

    @Test
    public void parseRawIntsV1() {

        final StringBuilder builder = new StringBuilder();

        final int value1 = intConverter.parse("VAL2");
        final int value2 = intConverter.parse("VAL3");
        final int value3 = intConverter.parse("VAL4");

        intConverter.append(builder, value1);
        builder.append(SEPARATOR);
        intConverter.append(builder, value2);
        builder.append(SEPARATOR);
        intConverter.append(builder, value3);

        assertEquals(builder.toString(), "VAL2/VAL3/VAL4");
    }

    // is this the only intended usage?
    @Test
    public void parseRawIntsV2() {

        final StringBuilder builder = new StringBuilder();

        final int value1 = intConverter.parse("VAL2");
        final int value2 = intConverter.parse("VAL3");
        final int value3 = intConverter.parse("VAL4");

        final StringBuilder buffer = new StringBuilder();

        intConverter.append(buffer, value1);
        builder.append(buffer).append(SEPARATOR);
        buffer.setLength(0);
        intConverter.append(buffer, value2);
        builder.append(buffer).append(SEPARATOR);
        buffer.setLength(0);
        intConverter.append(buffer, value3);
        builder.append(buffer);

        assertEquals(builder.toString(), "VAL2/VAL3/VAL4");
    }
}
