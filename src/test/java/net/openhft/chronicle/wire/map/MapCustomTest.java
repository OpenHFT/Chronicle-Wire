/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire.map;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.Wires;
import org.junit.Test;

import java.io.Closeable;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.assertTrue;

/**
 * Test suite for custom map implementations and their serialization.
 * Inherits from WireTestCommon for common test setup and teardown functionalities.
 */
@SuppressWarnings("serial")
public class MapCustomTest extends WireTestCommon {

    /**
     * Test the deep copy functionality of custom map holders.
     * Validates the equality of the original and the copied objects.
     */
    @Test
    public void test() {
        // Initialize a MapsHolder with sample values
        MapsHolder<Integer> mapsHolder = new MapsHolder<>(10, "one");

        // Perform a deep copy of the maps holder
        MapsHolder<Integer> result = Wires.deepCopy(mapsHolder);

        // Assert that the copied object is equivalent to the original
        assertTrue(result.equals(mapsHolder));
    }

    /**
     * Custom data holder that encapsulates various map implementations.
     * Inherits from SelfDescribingMarshallable for marshalling capabilities.
     */
    @SuppressWarnings("rawtypes")
    private static class MapsHolder<T extends Integer> extends SelfDescribingMarshallable {
        // Define custom maps and their instances
        private IntToStringMap i2sMap = new IntToStringMap();
        private IntMap<Void, String> iMap = new IntMap<>();
        private IntSuperMap<String, MapsHolder> isMap = new IntSuperMap<>();
        private TransformingMap<T, BigDecimal, String> transMap = new TransformingMap<>(BigDecimal::new);
        private GeneralMap gMap = new GeneralMap();
        // ClassCastException: net.openhft.chronicle.core.util.ObjectUtils$$Lambda$73/1401132667 cannot be cast to Map
        // private MarkedMap<String> mMap = i2sMap;

        // Constructor to initialize maps with provided key-value pairs
        @SuppressWarnings("unchecked")
        public MapsHolder(T x, String y) {
            i2sMap.put(x, y);
            iMap.put(x, y);
            isMap.put(x, y);
            transMap.put(x, y);
            gMap.put(x, y);
        }

        // Overridden equals method for custom equivalence check
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            MapsHolder that = (MapsHolder) o;
            return i2sMap.equals(that.i2sMap) && iMap.equals(that.iMap) && isMap.equals(that.isMap) &&
                    transMap.equals(that.transMap) && gMap.equals(that.gMap);
        }

        // Overridden hashCode method for custom hash computation
        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), i2sMap, iMap, transMap, gMap);
        }
    }

    /**
     * Custom TreeMap implementation marked with an interface.
     */
    private static class IntToStringMap extends TreeMap<Integer, String> implements MarkedMap<String> {
        @Override
        public void close() {
            // No-op.
        }
    }

    /**
     * Custom HashMap that contains an additional generic field.
     */
    private static class IntMap<IGNORE, V> extends HashMap<Integer, V> {
        private IGNORE me;
    }

    /**
     * Extended version of IntMap.
     */
    private static class IntSuperMap<SUPER, SELF> extends IntMap<Function<Integer, SELF>, SUPER> {
        // No-op.
    }

    /**
     * Custom LinkedHashMap that supports value transformation.
     */
    private static class TransformingMap<K, O, I> extends LinkedHashMap<K, I> {
        private Function<I, O> transform;

        // Constructor to set the transformation function
        public TransformingMap(Function<I, O> transform) {
            this.transform = transform;
        }

        // Method to fetch a transformed value based on the key
        public O getTransformed(K key) {
            I value = get(key);

            if (value != null)
                return transform.apply(value);

            return null;
        }
    }

    /**
     * A more generic version of IntMap.
     */
    @SuppressWarnings("rawtypes")
    private static class GeneralMap extends IntMap {
        // No-op.
    }

    /**
     * Marked map interface that extends Map and Closeable.
     */
    private interface MarkedMap<V> extends Map<Integer, V>, Closeable {
        // No-op.
    }
}
