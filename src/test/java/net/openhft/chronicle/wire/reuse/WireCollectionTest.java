/*
 * Copyright 2016-2020 chronicle.software
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
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

/**
 * Test class for WireCollection, using various wire types.
 */
@Ignore("TODO FIX")
@RunWith(value = Parameterized.class)
public class WireCollectionTest extends WireTestCommon {

    // Registering WireProperty class with the ClassAliasPool for serialization/deserialization
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(WireProperty.class);
    }

    private final Function<Bytes<?>, Wire> wireType;
    private WireCollection collection;// = new WireModel();

    /**
     * Constructor for WireCollectionTest.
     *
     * @param wireType A function that defines the type of Wire to be tested.
     */
    public WireCollectionTest(Function<Bytes<?>, Wire> wireType) {
        this.wireType = wireType;
    }

    /**
     * Parameterized test data generator.
     *
     * @return A collection of wire type configurations to be tested.
     */
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        return Arrays.asList(
                // Test with various wire types
                new Object[]{WireType.TEXT},
                new Object[]{WireType.YAML_ONLY},
                new Object[]{(Function<Bytes<?>, Wire>) bytes -> new BinaryWire(bytes, false, true, false, 128, "binary")},
                new Object[]{WireType.BINARY},
                new Object[]{WireType.BINARY_LIGHT},
                new Object[]{WireType.FIELDLESS_BINARY},
                new Object[]{WireType.JSON}
        );
    }

    /**
     * Sets up the test environment before each test.
     */
    @Before
    public void setUp() {
        collection = WireUtils.randomWireCollection();
    }

    /**
     * Tests multiple reads of WireCollection using various wire types.
     */
    @Test
    public void testMultipleReads() {
        Bytes<?> bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);

        // Writing the collection to the wire
        wire.writeDocument(true, collection);
       // System.out.println(Wires.fromSizePrefixedBlobs(bytes));

        @NotNull WireCollection results = new WireCollection();
        // Reading the collection from the wire
        wire.readDocument(results, null);

        // Asserting the collections are equal after the write-read process
        assertEquals(collection.toString(), results.toString());
        WireUtils.compareWireCollection(collection, results);
    }
}
