/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.*;

public class MarshallableTest {
    @Test
    public void testBytesMarshallable() {
        Marshallable m = new MyTypes();

        Bytes bytes = nativeBytes();
        assertTrue(bytes.isElastic());
        TextWire wire = new TextWire(bytes);
        m.writeMarshallable(wire);

        m.readMarshallable(wire);
    }
    @Test
    public void testEquals() {
        final Bytes bytes = nativeBytes();
        assertTrue(bytes.isElastic());
        final MyTypes source = new MyTypes();
        //change default value fields in order to let destination to be changed from its default values too
        source.b(true);
        source.s((short) 1);
        source.d(1.0);
        source.l(1L);
        source.i(1);
        source.text("a");
        final Marshallable destination = new MyTypes();
        assertNotEquals(source, destination);
        final TextWire wire = new TextWire(bytes);
        source.writeMarshallable(wire);
        destination.readMarshallable(wire);
        assertEquals(source, destination);
    }

}
