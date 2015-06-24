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

import net.openhft.chronicle.bytes.NativeBytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BinaryLongArrayReferenceTest {
    @Test
    public void getSetValues() {
        int length = 1024 + 8;
        try (NativeBytes bytes = NativeBytes.nativeBytes(length + 8)) {
            BinaryLongArrayReference.write(bytes, 128);

            BinaryLongArrayReference array = new BinaryLongArrayReference();
            array.bytesStore(bytes, 0, length);

            assertEquals(128, array.getCapacity());
            for (int i = 0; i < 128; i++)
                array.setValueAt(i, i + 1);

            for (int i = 0; i < 128; i++)
                assertEquals(i + 1, array.getValueAt(i));
        }
    }
}