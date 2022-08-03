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

        assertEquals("VAL2/VAL3/VAL4", builder.toString());
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

        assertEquals("VAL2/VAL3/VAL4", builder.toString());
    }
}
