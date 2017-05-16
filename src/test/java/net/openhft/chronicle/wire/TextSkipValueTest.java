package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 16/05/2017.
 */
@RunWith(value = Parameterized.class)
public class TextSkipValueTest {

    final String input;

    public TextSkipValueTest(String input) {
        this.input = input;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        List<Object[]> list = new ArrayList<>();
        for (String s : new String[]{
                "a: [ !Type { b: 'a, a', bb: aa }, !Type { c: 1.0, d: x } ]\n" +
                        "end",
                "a: [ { b: 'a, a', bb: aa }, { c: 1.0, d: x } ]\n" +
                        "end",
                "a: [ { b: a, bb: aa }, { c: 1.0, d: x } ]\n" +
                        "end",
                "a: [ { b: a }, { c: 1.0 } ]\n" +
                        "end",
                "a: { b: a },\n" +
                        "end",
                "a: [ a ],\n" +
                        "end",
                "a: a,\n" +
                        "end",
                "a,\n" +
                        "end"
        }) {
            list.add(new Object[]{s});
        }
        return list;
    }

    @Test
    public void skipValue() {
        Wire wire = new TextWire(Bytes.from(input));
        wire.getValueIn()
                .skipValue();
        wire.consumePadding();
        assertEquals("end", wire.bytes().toString());
    }
}
