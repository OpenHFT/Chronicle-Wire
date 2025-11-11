/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class TestLongConversion {
    private static char SEPARATOR = '/';
    private final LongConverter longConverter;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[]{Base32LongConverter.INSTANCE},
                new Object[]{Base64LongConverter.INSTANCE},
                new Object[]{Base85LongConverter.INSTANCE});
    }

    public TestLongConversion(LongConverter longConverter) {
        this.longConverter = longConverter;
    }

    @Test
    public void parseRawIntsV1() {

        final StringBuilder builder = new StringBuilder();

        final long value1 = longConverter.parse("VAL2");
        final long value2 = longConverter.parse("VAL3");
        final long value3 = longConverter.parse("VAL4");

        longConverter.append(builder, value1);
        builder.append(SEPARATOR);
        longConverter.append(builder, value2);
        builder.append(SEPARATOR);
        longConverter.append(builder, value3);

        assertEquals("VAL2/VAL3/VAL4", builder.toString());
    }

    @Test
    public void parseRawIntsV2() {

        final StringBuilder builder = new StringBuilder();

        final long value1 = longConverter.parse("VAL2");
        final long value2 = longConverter.parse("VVAL3", 1, 5);
        final long value3 = longConverter.parse("VAL45", 0, 4);

        final StringBuilder buffer = new StringBuilder();

        longConverter.append(buffer, value1);
        builder.append(buffer).append(SEPARATOR);
        buffer.setLength(0);
        longConverter.append(buffer, value2);
        builder.append(buffer).append(SEPARATOR);
        buffer.setLength(0);
        longConverter.append(buffer, value3);
        builder.append(buffer);

        assertEquals("VAL2/VAL3/VAL4", builder.toString());
    }
}
