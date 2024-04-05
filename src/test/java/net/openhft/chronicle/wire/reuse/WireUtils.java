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
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.openhft.chronicle.wire.reuse;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * Utility class for creating and comparing WireProperty and WireCollection objects.
 */
public class WireUtils {

    // Random object for generating random values
    private static final Random rand = new Random();

    /**
     * Creates a randomly generated WireProperty instance.
     *
     * @param i An integer to influence the generated property's values.
     * @return A WireProperty with randomly generated attributes.
     */
    @NotNull
    public static WireProperty randomWireProperty(int i) {
        return new WireProperty("reference" + i, "@:" + i, "name" + i, UUID.randomUUID().toString().replace("-", ""), rand.nextLong(), rand.nextInt(), UUID.randomUUID().toString());
    }

    /**
     * Creates a WireCollection with randomly generated WireProperty instances.
     *
     * @return A WireCollection with nested properties and sub-collections.
     */
    @NotNull
    public static WireCollection randomWireCollection() {
        @NotNull WireCollection collection = new WireCollection("reference", "@", "name", 234234234, 23, UUID.randomUUID().toString());

        // Add randomly generated properties
        IntStream.range(1, 10).forEach((i) -> {
            collection.addProperty(randomWireProperty(i));
        });

        // Add sub-collections with their properties

        IntStream.range(1, 4).forEach((i) -> {
            @NotNull WireCollection c = new WireCollection("reference" + i, "@:" + i, "name" + i, rand.nextLong(), rand.nextInt(), UUID.randomUUID().toString());
            collection.addCollection(c);
            IntStream.range(1, 4).forEach((k) -> {
                c.addProperty(new WireProperty("reference" + k, "@:" + i + "-" + k, "name" + k, UUID.randomUUID().toString().replace("-", ""), rand.nextLong(), rand.nextInt(), UUID.randomUUID().toString()));
            });
        });

        return collection;
    }

    /**
     * Compares two WireModel instances for equality.
     *
     * @param a The first WireModel instance.
     * @param b The second WireModel instance.
     */
    public static void compareWireModel(@NotNull WireModel a, @NotNull WireModel b) {
        assertEquals(a.getId(), b.getId());
        assertEquals(a.getRevision(), b.getRevision());
        assertEquals(a.getKey(), b.getKey());
    }

    /**
     * Compares two WireProperty instances for equality.
     *
     * @param a The first WireProperty instance.
     * @param b The second WireProperty instance.
     */
    public static void compareWireProperty(@NotNull WireProperty a, @NotNull WireProperty b) {
        compareWireModel(a, b);
        assertEquals(a.getReference(), b.getReference());
        assertEquals(a.getValue(), b.getValue());
        assertEquals(a.getName(), b.getName());
        assertEquals(a.getPath(), b.getPath());
    }

    /**
     * Compares two WireCollection instances for equality.
     *
     * @param a The first WireCollection instance.
     * @param b The second WireCollection instance.
     */
    public static void compareWireCollection(@NotNull WireCollection a, @NotNull WireCollection b) {
        compareWireModel(a, b);
        assertEquals(a.getReference(), b.getReference());
        assertEquals(a.getName(), b.getName());
        assertEquals(a.getPath(), b.getPath());

        assertEquals(a.getCollections().size(), b.getCollections().size());
        assertEquals(a.getProperties().size(), b.getProperties().size());

        // Check each property and collection in 'a' against the corresponding elements in 'b'
        a.getProperties().values().forEach(c -> {
            if (b.getProperties().containsKey(c.getReference())) {
                compareWireProperty(c, b.getProperties().get(c.getReference()));
            } else {
                Assert.fail("Cannot match property child element WireProperty");
            }
        });

        a.getCollections().values().forEach(c -> {
            if (b.getCollections().containsKey(c.getReference())) {
                compareWireCollection(c, b.getCollections().get(c.getReference()));
            } else {
                //Assert.fail("Cannot match collection child element WireCollection");
            }
        });
    }
}
