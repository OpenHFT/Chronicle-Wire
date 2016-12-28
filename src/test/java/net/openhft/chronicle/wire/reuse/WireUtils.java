/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
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
 *
 * @author gadei
 */
public class WireUtils {

    private static final Random rand = new Random();

    @NotNull
    public static WireProperty randomWireProperty(int i) {
        return new WireProperty("reference" + i, "@:" + i, "name" + i, UUID.randomUUID().toString().replace("-", ""), rand.nextLong(), rand.nextInt(), UUID.randomUUID().toString());
    }

    @NotNull
    public static WireCollection randomWireCollection() {
        @NotNull WireCollection collection = new WireCollection("reference", "@", "name", 234234234, 23, UUID.randomUUID().toString());

        IntStream.range(1, 10).forEach((i) -> {
            collection.addProperty(randomWireProperty(i));
        });

        IntStream.range(1, 4).forEach((i) -> {
            @NotNull WireCollection c = new WireCollection("reference" + i, "@:" + i, "name" + i, rand.nextLong(), rand.nextInt(), UUID.randomUUID().toString());
            collection.addCollection(c);
            IntStream.range(1, 4).forEach((k) -> {
                c.addProperty(new WireProperty("reference" + k, "@:" + i + "-" + k, "name" + k, UUID.randomUUID().toString().replace("-", ""), rand.nextLong(), rand.nextInt(), UUID.randomUUID().toString()));
            });
        });

        return collection;
    }

    public static void compareWireModel(@NotNull WireModel a, @NotNull WireModel b) {
        assertEquals(a.getId(), b.getId());
        assertEquals(a.getRevision(), b.getRevision());
        assertEquals(a.getKey(), b.getKey());
    }

    public static void compareWireProperty(@NotNull WireProperty a, @NotNull WireProperty b) {
        compareWireModel(a, b);
        assertEquals(a.getReference(), b.getReference());
        assertEquals(a.getValue(), b.getValue());
        assertEquals(a.getName(), b.getName());
        assertEquals(a.getPath(), b.getPath());
    }

    public static void compareWireCollection(@NotNull WireCollection a, @NotNull WireCollection b) {
        compareWireModel(a, b);
        assertEquals(a.getReference(), b.getReference());
        assertEquals(a.getName(), b.getName());
        assertEquals(a.getPath(), b.getPath());

        assertEquals(a.getCollections().size(), b.getCollections().size());
        assertEquals(a.getProperties().size(), b.getProperties().size());

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
