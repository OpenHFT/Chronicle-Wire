package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.AbstractMarshallable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by peter on 09/05/16.
 */
public class Nested extends AbstractMarshallable {
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
}
