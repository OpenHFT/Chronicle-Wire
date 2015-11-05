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
import net.openhft.chronicle.bytes.NativeBytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TextLongArrayReferenceTest {
    @Test
    public void getSetValues() {
        int length = 5 * 22 + 47;
        try (NativeBytes bytes = Bytes.allocateElasticDirect(length)) {
            TextLongArrayReference.write(bytes, 5);

            TextLongArrayReference array = new TextLongArrayReference();
            array.bytesStore(bytes, 0, length);

            assertEquals(5, array.getCapacity());
            for (int i = 0; i < 5; i++)
                array.setValueAt(i, i + 1);

            for (int i = 0; i < 5; i++)
                assertEquals(i + 1, array.getValueAt(i));

            assertEquals("{ capacity: 00000000000000000005, values: [ 00000000000000000001, 00000000000000000002, 00000000000000000003, 00000000000000000004, 00000000000000000005 ] }\n", bytes.toString());
        }
    }
}