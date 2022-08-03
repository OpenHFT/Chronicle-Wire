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

public class Base256IntConverterTest extends WireTestCommon {

    @Test
    public void parse() {
        assertEquals(0, Base256IntConverter.INSTANCE.parse(""));
        assertEquals(1, Base256IntConverter.INSTANCE.parse("\u0001"));
        assertEquals(255, Base256IntConverter.INSTANCE.parse("\u00FF"));
        assertEquals(256, Base256IntConverter.INSTANCE.parse("\u0001\0"));
        assertEquals(('U' << 16) + ('5' << 8) + '1', Base256IntConverter.INSTANCE.parse("U51"));

    }

    @Test
    public void append() {
        assertEquals("", Base256IntConverter.INSTANCE.asString(0));
        assertEquals("A", Base256IntConverter.INSTANCE.asString('A'));
        assertEquals("U51", Base256IntConverter.INSTANCE.asString(('U' << 16) + ('5' << 8) + '1'));
        assertEquals(0, Base256IntConverter.INSTANCE.asString(0).length());
        assertEquals(1, Base256IntConverter.INSTANCE.asString(1).length());
        assertEquals(1, Base256IntConverter.INSTANCE.asString(255).length());
        assertEquals(2, Base256IntConverter.INSTANCE.asString(256).length());
        assertEquals(2, Base256IntConverter.INSTANCE.asString((1 << 16) - 1).length());
        assertEquals(3, Base256IntConverter.INSTANCE.asString((1 << 16)).length());
        assertEquals(3, Base256IntConverter.INSTANCE.asString((1 << 24) - 1).length());
        assertEquals(4, Base256IntConverter.INSTANCE.asString((1 << 24)).length());
        assertEquals(4, Base256IntConverter.INSTANCE.asString(~0).length());
    }
}