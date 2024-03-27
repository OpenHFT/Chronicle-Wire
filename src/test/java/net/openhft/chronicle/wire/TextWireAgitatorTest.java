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

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

// Test created as a result of agitator tests i.e. random character changes
public class TextWireAgitatorTest extends WireTestCommon {

    @Test
    public void lowerCaseClassTuple() {
        Wires.GENERATE_TUPLES = true;
        Object o = Marshallable.fromString("!" + TextWireTest.MyDto.class.getName().toLowerCase() + " { }");
        assertEquals("!net.openhft.chronicle.wire.textwiretest$mydto {\n" +
                "}\n", o.toString());
    }

    @Test
    public void lowerCaseClassWarn() {
        expectException("Unable to load net.openhft.chronicle.wire.textwiretest$mydto, is a class alias missing");
        Wires.THROW_CNFRE = false;
        Wires.GENERATE_TUPLES = false;
        assertTrue(Marshallable.fromString("!" + TextWireTest.MyDto.class.getName().toLowerCase() + " { }") instanceof Map);
    }

    @Test(expected = ClassNotFoundRuntimeException.class)
    public void lowerCaseClassThrows() {
        Wires.GENERATE_TUPLES = false;
        Object o = Marshallable.fromString("!" + TextWireTest.MyDto.class.getName().toLowerCase() + " { }");
        fail("" + o);
    }

    @Test(expected = IORuntimeException.class)
    public void colonInList() {
        TextWireTest.MyDto md = Marshallable.fromString("!net.openhft.chronicle.wire.TextWireTest$MyDto {\n" +
                "  strings: [\n" +
                "  :\n" +
                "  ]\n" +
                "}\n");
        assertEquals("[hello]", md.toString());
    }

    @Test
    public void notBoolean() {
        // produces a warning.
        MyFlagged mf = Marshallable.fromString("!net.openhft.chronicle.wire.TextWireAgitatorTest$MyFlagged {\n" +
                "  flag: not-false\n" +
                "}");
        assertNotNull(mf);
    }

    static class MyFlagged extends SelfDescribingMarshallable {
        boolean flag;
    }
}
