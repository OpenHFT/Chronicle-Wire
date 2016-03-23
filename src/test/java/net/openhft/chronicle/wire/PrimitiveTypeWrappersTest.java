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

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Rob Austin.
 */
@RunWith(value = Parameterized.class)
public class PrimitiveTypeWrappersTest {

    private boolean isTextWire;

    public PrimitiveTypeWrappersTest(Object isTextWire) {
        this.isTextWire = (Boolean) isTextWire;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(
                new Object[]{Boolean.FALSE}
                , new Object[]{Boolean.TRUE}

        );
    }

    @Test
    public void testNumbers() throws Exception {
        final Class[] types = new Class[]{Byte.class,
                Short.class, Float.class,
                Integer.class, Long.class, Double.class};

        final Number[] nums = new Number[]{(byte) 1, (short) 1, (float) 1, 1, (long) 1, (double) 1};

        for (Number num : nums) {
            for (Class type : types) {
                final Wire wire = wireFactory();

                wire.write().object(num);
                final Object object = wire.read().object(type);
                Assert.assertTrue(num.getClass() + " to " + type.getName(), type.isAssignableFrom(object.getClass()));
                Assert.assertEquals(num.getClass() + " to " + type.getName(), 1, ((Number) object).intValue());
            }
        }
    }

    @Test
    public void testNumbers2() throws Exception {
        final Number[] nums = new Number[]{(byte) 1, (short) 1, (float) 1, 1, (long) 1, (double) 1};

        for (Number num : nums) {
            final Wire wire = wireFactory();

            wire.write().object(num);
            System.out.println(num.getClass() + " of " + num + " is " + (isTextWire ? wire.toString() : wire.bytes().toHexString()));
            final Object object = wire.read().object(Object.class);
            Assert.assertSame(num.getClass(), object.getClass());
            Assert.assertEquals(num.getClass().getName(), num, object);
        }
    }

    @Test
    public void testCharacter() throws Exception {
        final Wire wire = wireFactory();
        wire.write().object('1');
        final Object object = wire.read().object(Character.class);
        Assert.assertTrue(object instanceof Character);
        Assert.assertEquals('1', object);
    }

    @Test
    public void testCharacterWritenAsString() throws Exception {
        final Wire wire = wireFactory();
        wire.write().object("1");
        final Object object = wire.read().object(Character.class);
        Assert.assertTrue(object instanceof Character);
        Assert.assertEquals('1', object);
    }

    @Test
    public void testCharReadAsString() throws Exception {
        final Wire wire = wireFactory();
        wire.write().object('1');
        final Object object = wire.read().object(String.class);
        Assert.assertTrue(object instanceof String);
        Assert.assertEquals("1", object);
    }

    @Test
    public void testStoreStringReadAsChar() throws Exception {
        final Wire wire = wireFactory();
        wire.write().object("LONG STRING");
        final Object object = wire.read().object(Character.class);
        Assert.assertTrue(object instanceof Character);
        Assert.assertEquals('L', object);
    }

    @NotNull
    private Wire wireFactory() {
        final Bytes bytes = Bytes.allocateElasticDirect();
        return (isTextWire) ? new TextWire(bytes) : new BinaryWire(bytes);
    }

}
