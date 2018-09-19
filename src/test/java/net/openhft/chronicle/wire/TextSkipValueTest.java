package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 16/05/2017.
 */
@RunWith(value = Parameterized.class)
public class TextSkipValueTest {

    final String input;

    public TextSkipValueTest(String input) {
        this.input = input;
    }

    @NotNull
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        List<Object[]> list = new ArrayList<>();
        for (String s : new String[]{
                "data: {\n" +
                        "  a: 123\n" +
                        "  b: 1.1\n" +
                        "  c: \"hi\"\n" +
                        "},\n" +
                        "end",
                "cluster1: {\n" +
                        "  context:  !EngineClusterContext  { }\n" +
                        "  host1: {\n" +
                        "     hostId: 1\n" +
                        "  },\n" +
                        "},\n" +
                        "end",
                "? { MyField: parent }: {\n" +
                        "  ? !sometype { MyField: key1 }: value1,\n" +
                        "  ? !sometype { MyField: key2 }: value2\n" +
                        "},\n" +
                        "end",
                "example: {\n" +
                        "  ? { MyField: aKey }: { MyField: aValue },\n" +
                        "  ? { MyField: aKey2 }: { MyField: aValue2 }\n" +
                        "},\n" +
                        "end",
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

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }
}
