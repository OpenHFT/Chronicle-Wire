package net.openhft.chronicle.wire.serializable;

import net.openhft.chronicle.wire.Wires;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.openhft.chronicle.wire.WireType.TEXT;

/**
 * Created by peter on 09/05/16.
 */
public class Nested implements Serializable {
    ScalarValues values;
    List<String> strings;
    Set<Integer> ints;
    Map<String, List<Double>> map;

    public Nested() {
    }

    public Nested(ScalarValues values, List<String> strings, Set<Integer> ints, Map<String, List<Double>> map) {
        this.values = values;
        this.strings = strings;
        this.ints = ints;
        this.map = map;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Nested && Wires.isEquals(this, obj);
    }

    @Override
    public String toString() {
        return TEXT.asString(this);
    }
}
