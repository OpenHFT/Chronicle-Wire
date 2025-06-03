package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.MyTypes;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ReadQuotedTest {
    private final WireType wireType;

    public ReadQuotedTest(WireType wireType) {
        this.wireType = wireType;
    }

    // Define parameters for this parameterized test
    @Parameterized.Parameters(name = "wireType={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{WireType.JSON_ONLY},
                new Object[]{WireType.YAML_ONLY}
        );
    }

    static final String EXPECTED = "!net.openhft.chronicle.wire.MyTypes {\n" +
            "  text: hi,\n" +
            "  flag: false,\n" +
            "  b: 1,\n" +
            "  s: 2,\n" +
            "  ch: \"3\",\n" +
            "  i: 4,\n" +
            "  f: 5.6,\n" +
            "  d: 7.8,\n" +
            "  l: 9\n" +
            "}\n";

    @Test
    public void testReadSingleQuoted() {
        MyTypes mt = wireType.fromString(MyTypes.class, "{" +
                "  text: 'hi',\n" +
                "  flag: 'false',\n" +
                "  b: '1' ,\n" +
                "  s: '2'\t,\n" +
                "  ch: '3'\n,\n" +
                "  i: '4',\n" +
                "  f: '5.6',\n" +
                "  d: '7.8',\n" +
                "  l: '9'\n" +
                "}");
        assertEquals(EXPECTED, mt.toString());
    }

    @Test
    public void testReadDoubleQuoted() {
        MyTypes mt = wireType.fromString(MyTypes.class, "{" +
                "  text: \"hi\",\n" +
                "  flag: \"false\",\n" +
                "  b: \"1\" ,\n" +
                "  s: \"2\"\t,\n" +
                "  ch: \"3\"\n,\n" +
                "  i: \"4\",\n" +
                "  f: \"5.6\",\n" +
                "  d: \"7.8\",\n" +
                "  l: \"9\"\n" +
                "}");
        assertEquals(EXPECTED, mt.toString());
    }
}

