/*
 *
 *  *     Copyright (C) 2016  higherfrequencytrading.com
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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

import static net.openhft.chronicle.wire.WireType.*;

/**
 * @author Rob Austin.
 */
public class ReadAnyWireTest {

    @Test
    public void testReadAny() throws Exception {
        final Bytes<ByteBuffer> t = Bytes.elasticByteBuffer();
        final Wire wire = TEXT.apply(t);
        wire.write((() -> "hello")).text("world");
        Assert.assertEquals("world", READ_ANY.apply(t).read(() -> "hello").text());
    }

    @Test
    public void testCreateReadAnyFirstTextWire() throws Exception {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final String expected = "world";
        TEXT.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
    }


    @Test
    public void testCreateReadAnyFirstBinaryWire() throws Exception {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final String expected = "world";
        BINARY.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
    }


    @Test
    public void testCreateReadAnyFirstFIELDLESS_BINARYWire() throws Exception {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final String expected = "world";
        FIELDLESS_BINARY.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
    }
}


