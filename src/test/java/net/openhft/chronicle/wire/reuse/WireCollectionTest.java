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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validates WireCollection marshalling round-trip across all supported wire types and formats.
 */
@Disabled("Disabled until WireCollection round-trip works for all wire types")
class WireCollectionTest extends WireTestCommon {

    // Registering WireProperty class with the ClassAliasPool for serialization/deserialization
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(WireProperty.class);
    }

    private WireCollection collection;// = new WireModel();

    static Collection<Object[]> combinations() {
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
    @BeforeEach
    void setUp() {
        collection = WireUtils.randomWireCollection();
    }

    @ParameterizedTest
    @MethodSource("combinations")
    @DisplayName("WireCollection round-trip across wire types")
    void testMultipleReads(Function<Bytes<?>, Wire> wireType) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = wireType.apply(bytes);

        // Writing the collection to the wire
        wire.writeDocument(true, collection);

        @NotNull WireCollection results = new WireCollection();
        // Reading the collection from the wire
        wire.readDocument(results, null);

        // Asserting the collections are equal after the write-read process
        assertEquals(collection.toString(), results.toString(),
                "WireCollection should round-trip via write and read");
        WireUtils.compareWireCollection(collection, results);
    }
}
