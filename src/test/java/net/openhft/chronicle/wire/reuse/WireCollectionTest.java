/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for WireCollection, using various wire types.
 */
@Disabled("TODO FIX")
public class WireCollectionTest extends WireTestCommon {

    // Registering WireProperty class with the ClassAliasPool for serialization/deserialization
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(WireProperty.class);
    }

    private WireCollection collection;// = new WireModel();

    /**
     * Constructor for WireCollectionTest.
     *
     * @param wireType A function that defines the type of Wire to be tested.
     */
    /**
     * Parameterized test data generator.
     *
     * @return A collection of wire type configurations to be tested.
     */
    public static Collection<Function<Bytes<?>, Wire>> combinations() {
        return Arrays.asList(
                // Test with various wire types
                WireType.TEXT,
                WireType.YAML_ONLY,
                bytes -> new BinaryWire(bytes, false, true, false, 128, "binary"),
                WireType.BINARY,
                WireType.BINARY_LIGHT,
                WireType.FIELDLESS_BINARY,
                WireType.JSON
        );
    }

    /**
     * Sets up the test environment before each test.
     */
    @BeforeEach
    public void setUp() {
        collection = WireUtils.randomWireCollection();
    }

    /**
     * Tests multiple reads of WireCollection using various wire types.
     */
    @ParameterizedTest
    @MethodSource("combinations")
    public void testMultipleReads(Function<Bytes<?>, Wire> wireType) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
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
