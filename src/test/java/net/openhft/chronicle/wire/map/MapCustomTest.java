package net.openhft.chronicle.wire.map;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.Closeable;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.assertTrue;

public class MapCustomTest extends WireTestCommon {

    @Test
    public void test() {
        MapsHolder<Integer> mapsHolder = new MapsHolder<>(10, "one");

        @NotNull MapsHolder result = Wires.deepCopy(mapsHolder);

        assertTrue(result.equals(mapsHolder));
    }

    private static class MapsHolder<T extends Integer> extends SelfDescribingMarshallable {
        private IntToStringMap i2sMap = new IntToStringMap();
        private IntMap<Void, String> iMap = new IntMap<>();
        private IntSuperMap<String, MapsHolder> isMap = new IntSuperMap<>();
        private TransformingMap<T, BigDecimal, String> transMap = new TransformingMap<>(BigDecimal::new);
        private GeneralMap gMap = new GeneralMap();
        // ClassCastException: net.openhft.chronicle.core.util.ObjectUtils$$Lambda$73/1401132667 cannot be cast to Map
        // private MarkedMap<String> mMap = i2sMap;

        public MapsHolder(T x, String y) {
            i2sMap.put(x, y);
            iMap.put(x, y);
            isMap.put(x, y);
            transMap.put(x, y);
            gMap.put(x, y);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            MapsHolder that = (MapsHolder) o;
            return i2sMap.equals(that.i2sMap) && iMap.equals(that.iMap) && isMap.equals(that.isMap) &&
                    transMap.equals(that.transMap) && gMap.equals(that.gMap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), i2sMap, iMap, transMap, gMap);
        }
    }

    private static class IntToStringMap extends TreeMap<Integer, String> implements MarkedMap<String> {
        @Override
        public void close() {
            // No-op.
        }
    }

    private static class IntMap<IGNORE, V> extends HashMap<Integer, V> {
        private IGNORE me;
    }

    private static class IntSuperMap<SUPER, SELF> extends IntMap<Function<Integer, SELF>, SUPER> {
        // No-op.
    }

    private static class TransformingMap<K, O, I> extends LinkedHashMap<K, I> {
        private Function<I, O> transform;

        public TransformingMap(Function<I, O> transform) {
            this.transform = transform;
        }

        // For consideration
        public O getTransformed(K key) {
            I value = get(key);

            if (value != null)
                return transform.apply(value);

            return null;
        }
    }

    @SuppressWarnings({"rawtypes"})
    private static class GeneralMap extends IntMap {
        // No-op.
    }

    private interface MarkedMap<V> extends Map<Integer, V>, Closeable {
        // No-op.
    }
}