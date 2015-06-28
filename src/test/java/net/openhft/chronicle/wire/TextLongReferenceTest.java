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

import net.openhft.chronicle.bytes.NativeBytesStore;
import org.junit.Assert;
import org.junit.Test;

public class TextLongReferenceTest {

    @Test
    public void testSetValue() {
        final TextLongReference value = new TextLongReference();
        try (NativeBytesStore bytesStore = NativeBytesStore.nativeStoreWithFixedCapacity(value.maxSize())) {
            value.bytesStore(bytesStore, 0, value.maxSize());
            int expected = 10;
            value.setValue(expected);

            long l = bytesStore.parseLong(TextLongReference.VALUE);
            System.out.println(l);

//        System.out.println(Bytes.toHexString(bytes,33, bytes.limit() - 33));

            Assert.assertEquals(expected, value.getValue());
        }
    }
}