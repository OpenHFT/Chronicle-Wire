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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class TestLongConversion {
    public static char SEPARATOR = '/';
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
