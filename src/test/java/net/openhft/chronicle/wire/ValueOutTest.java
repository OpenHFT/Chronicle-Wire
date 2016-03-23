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

package net.openhft.chronicle.wire;

import junit.framework.TestCase;
import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Rob Austin.
 */
@RunWith(value = Parameterized.class)
public class ValueOutTest extends TestCase {


    private final WireType wireType;

    public ValueOutTest(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {WireType.TEXT},
                {WireType.BINARY}
        });
    }

    @Test
    public void test() {

        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());

        final byte[] expected = "this is my byte array".getBytes();
        wire.writeDocument(false, w ->
                w.write().object(expected)

        );

        System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));

        wire.readDocument(null, w -> {
            final byte[] actual = (byte[]) w.read().object();
            Assert.assertArrayEquals(expected, actual);

        });

    }

    @Test
    public void testRequestedType() {

        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());

        final byte[] expected = "this is my byte array".getBytes();
        wire.writeDocument(false, w -> w.write().object(expected));

        System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));

        wire.readDocument(null, w -> {
            final byte[] actual = w.read().object(byte[].class);
            Assert.assertArrayEquals(expected, actual);
        });

    }


    @Test
    public void testAllBytes() {

        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());

        for (int i = -128; i < 127; i++) {

            final byte[] expected = {(byte) i};
            wire.writeDocument(false, w ->
                    w.write().object(expected)
            );

            System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));

            wire.readDocument(null, w -> {
                final byte[] actual = (byte[]) w.read().object();
                Assert.assertArrayEquals(expected, actual);
            });

        }
    }


}