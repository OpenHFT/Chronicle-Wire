package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.JsonUtil;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class Issue328Test extends WireTestCommon {

    @Test
    public void map() {
        final Wire wire = new JSONWire().useTypes(true);
        final int size = 3;
        // keys must be strings in JSON
        final Map<Integer, String> map = IntStream.range(0, size)
                .boxed()
                .collect(Collectors.toMap(Function.identity(), i -> Integer.toString(i)));

        wire.getValueOut().object(map);
        final String actual = wire.toString();
        final String expected = IntStream.range(0, size)
                .boxed()
                .map(i -> String.format("\"%d\":\"%d\"", i, i))
                .collect(Collectors.joining(",", "{", "}"));

        // Note: The output should pass a test at https://jsonlint.com/

        System.out.println("actual = " + actual);
        JsonUtil.assertBalancedBrackets(actual);
        assertEquals(expected, actual);
    }
}
