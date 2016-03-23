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

package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.Wires;
import org.junit.Before;
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
public class WireCollectionTest {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(WireProperty.class);
    }

    private final Function<Bytes, Wire> wireType;
    private WireCollection collection;// = new WireModel();

    public WireCollectionTest(Function<Bytes, Wire> wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        return Arrays.asList(
                new Object[]{(Function<Bytes, Wire>) bytes -> new BinaryWire(bytes, false, true, false, 128, "binary")},
                new Object[]{WireType.TEXT},
                new Object[]{WireType.BINARY},
                new Object[]{WireType.FIELDLESS_BINARY},
                new Object[]{WireType.JSON}
        );
    }

    @Before
    public void setUp() {
        collection = WireUtils.randomWireCollection();
    }
    
    @Test
    public void testMultipleReads() {
        Bytes bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);

        wire.writeDocument(true, collection);
        System.out.println(Wires.fromSizePrefixedBlobs(bytes));

        WireCollection results = new WireCollection();
        wire.readDocument(results, null);

        assertEquals(collection.toString(), results.toString());
        WireUtils.compareWireCollection(collection, results);
    }
}
