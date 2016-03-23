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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.openhft.chronicle.wire.reuse;

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

    public static WireProperty randomWireProperty(int i) {
        return new WireProperty("reference" + i, "@:" + i, "name" + i, UUID.randomUUID().toString().replace("-", ""), rand.nextLong(), rand.nextInt(), UUID.randomUUID().toString());
    }

    public static WireCollection randomWireCollection() {
        WireCollection collection = new WireCollection("reference", "@", "name", 234234234, 23, UUID.randomUUID().toString());

        IntStream.range(1, 10).forEach((i) -> {
            collection.addProperty(randomWireProperty(i));
        });

        IntStream.range(1, 4).forEach((i) -> {
            WireCollection c = new WireCollection("reference" + i, "@:" + i, "name" + i, rand.nextLong(), rand.nextInt(), UUID.randomUUID().toString());
            collection.addCollection(c);
            IntStream.range(1, 4).forEach((k) -> {
                c.addProperty(new WireProperty("reference" + k, "@:" + i + "-" + k, "name" + k, UUID.randomUUID().toString().replace("-", ""), rand.nextLong(), rand.nextInt(), UUID.randomUUID().toString()));
            });
        });

        return collection;
    }

    public static void compareWireModel(WireModel a, WireModel b) {
        assertEquals(a.getId(), b.getId());
        assertEquals(a.getRevision(), b.getRevision());
        assertEquals(a.getKey(), b.getKey());
    }

    public static void compareWireProperty(WireProperty a, WireProperty b) {
        compareWireModel(a, b);
        assertEquals(a.getReference(), b.getReference());
        assertEquals(a.getValue(), b.getValue());
        assertEquals(a.getName(), b.getName());
        assertEquals(a.getPath(), b.getPath());
    }

    public static void compareWireCollection(WireCollection a, WireCollection b) {
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
