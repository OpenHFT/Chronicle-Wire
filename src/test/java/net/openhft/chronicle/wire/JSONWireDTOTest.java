package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JSONWireDTOTest extends WireTestCommon {
    @Test
    public void dto() {
        expectException("Found this$0, in class");
        // underlying bytes
        Bytes<?> bytes = Bytes.allocateElasticDirect();

        // Use the JSON serialization
        JSONWire wire = new JSONWire(bytes);

        // test object
        JSOuterClass dto = new JSOuterClass();
        dto.text = "hi";
        dto.d = 3.1415;
        dto.nested.add(new JSNestedClass("there", 1));

        // write the DTO to the JSONWire
        wire.getValueOut().marshallable(dto);

        // check the bytes
        assertEquals("{\"text\":\"hi\",\"nested\":[ {\"str\":\"there\",\"num\":1} ], \"b\":false,\"bb\":0,\"s\":0,\"f\":0.0,\"d\":3.1415,\"l\":0,\"i\":0}",
                bytes.toString());

        // create another instance to deserialize into
        JSOuterClass dto2 = new JSOuterClass();

        // read the data.
        wire.getValueIn().marshallable(dto2);

        // dump using YAML
        assertEquals("!net.openhft.chronicle.wire.JSONWireDTOTest$JSOuterClass {\n" +
                "  text: hi,\n" +
                "  nested: [\n" +
                "    { str: there, num: 1 }\n" +
                "  ],\n" +
                "  b: false,\n" +
                "  bb: 0,\n" +
                "  s: 0,\n" +
                "  f: 0.0,\n" +
                "  d: 3.1415,\n" +
                "  l: 0,\n" +
                "  i: 0\n" +
                "}\n", dto2.toString());
        bytes.releaseLast();
    }

    static class JSOuterClass extends SelfDescribingMarshallable {
        String text;
        @NotNull
        List<JSNestedClass> nested = new ArrayList<>();
        boolean b;
        byte bb;
        short s;
        float f;
        double d;
        long l;
        int i;

        JSOuterClass() {
        }
    }

    class JSNestedClass extends SelfDescribingMarshallable {
        String str;
        int num;

        public JSNestedClass(String str, int num) {
            this.str = str;
            this.num = num;
        }
    }
}
