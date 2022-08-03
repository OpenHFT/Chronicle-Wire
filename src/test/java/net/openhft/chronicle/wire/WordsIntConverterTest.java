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

import static org.junit.Assert.assertEquals;

public class WordsIntConverterTest extends WireTestCommon {
    @Test
    public void parse() {
        IntConverter bic = new WordsIntConverter();
        for (int l : new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            String text = bic.asString(l);
           // System.out.println(text);
            assertEquals(l, bic.parse(text));
        }
    }
}