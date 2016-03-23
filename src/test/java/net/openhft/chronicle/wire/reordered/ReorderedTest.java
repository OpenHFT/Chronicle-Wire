/*
 *
 *  *     Copyright (C) ${YEAR}  higherfrequencytrading.com
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Lesser General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Lesser General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Lesser General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package net.openhft.chronicle.wire.reordered;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter.lawrey on 01/02/2016.
 */
@RunWith(value = Parameterized.class)
public class ReorderedTest {
    private static final OuterClass outerClass1 = new OuterClass();
    private static final OuterClass outerClass2 = new OuterClass();

    static {
        outerClass1.setText("text1");
        outerClass2.setText("text2");
        outerClass1.setWireType(WireType.BINARY);
        outerClass2.setWireType(WireType.TEXT);
        outerClass1.clearListA();
        outerClass2.clearListA();
        outerClass1.clearListB();
        outerClass2.clearListB();
        outerClass1.addListA().setTextNumber("num1A", 11);
        outerClass1.addListB().setTextNumber("num1B", 12);
        outerClass1.addListA().setTextNumber("num1AA", 111);
        outerClass1.addListB().setTextNumber("num1BB", 122);
        outerClass2.addListA().setTextNumber("num2A", 21);
        outerClass2.addListB().setTextNumber("num2B", 22);
    }

    private final Function<Bytes, Wire> wireType;

    public ReorderedTest(Function<Bytes, Wire> wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        return Arrays.asList(
                new Object[]{WireType.BINARY},
                new Object[]{WireType.TEXT},
                new Object[]{WireType.JSON}
        );
    }

    @Test
    public void testWithReorderedFields() {
        Bytes bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);
        wire.writeEventName(() -> "test1").marshallable(outerClass1);
        if (wireType == WireType.JSON)
            wire.bytes().writeUnsignedByte('\n');
        wire.writeEventName(() -> "test2").marshallable(outerClass2);

        System.out.println(bytes.readByte(0) < 0 ? bytes.toHexString() : bytes.toString());
        StringBuilder sb = new StringBuilder();
        OuterClass outerClass0 = new OuterClass();

        wire.readEventName(sb).marshallable(outerClass0);
        assertEquals("test1", sb.toString());
        assertEquals(outerClass1.toString().replace(',', '\n'), outerClass0.toString().replace(',', '\n'));

        wire.readEventName(sb).marshallable(outerClass0);
        assertEquals("test2", sb.toString());
        assertEquals(outerClass2.toString().replace(',', '\n'), outerClass0.toString().replace(',', '\n'));

    }
}
