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

package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.JsonUtil;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class Issue328Test extends WireTestCommon {

    @Test
    public void map() {
        final Wire wire = new JSONWire().useTypes(true);
        final int size = 3;
        // keys must be strings in JSON
        final Map<Integer, String> map = IntStream.range(0, size)
                .boxed()
                .collect(Collectors.toMap(Function.identity(), i -> Integer.toString(i)));

        wire.getValueOut().object(map);
        final String actual = wire.toString();
        final String expected = IntStream.range(0, size)
                .boxed()
                .map(i -> String.format("\"%d\":\"%d\"", i, i))
                .collect(Collectors.joining(",", "{", "}"));

        // Note: The output should pass a test at https://jsonlint.com/

        System.out.println("actual = " + actual);
        JsonUtil.assertBalancedBrackets(actual);
        assertEquals(expected, actual);
    }
}
