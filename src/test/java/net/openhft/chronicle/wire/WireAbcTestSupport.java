package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

final class WireAbcTestSupport {
    private WireAbcTestSupport() {
    }

    static void assertAbcdBytes(Wire wire, boolean resetValueInState) {
        wire.bytes().append(
                "A: \"hi\",\n" +
                        "B: 'hi',\n" +
                        "C: hi,\n" +
                        "D: bye,\n");
        TextWireTest.ABCD abcd = new TextWireTest.ABCD();

        try {
            for (int i = 0; i < 5; i++) {
                wire.bytes().readPosition(0);
                if (resetValueInState) {
                    wire.getValueIn().resetState();
                }
                assertEquals("!net.openhft.chronicle.wire.TextWireTest$ABCD {\n" +
                        "  A: hi,\n" +
                        "  B: hi,\n" +
                        "  C: hi,\n" +
                        "  D: bye\n" +
                        "}\n", wire.getValueIn()
                        .object(abcd, TextWireTest.ABCD.class)
                        .toString());
            }
        } finally {
            abcd.releaseAll();
        }
    }

    static void assertAbcStringBuilder(Wire wire, List<String> expectedComments) {
        String stringA = "A: \"hi\", # This is an A\n";
        String stringB = "B: 'hi', # This is a B\n";
        String stringC = "C: hi, # And that's a C\n";

        StringBuilder sb = new StringBuilder();
        wire.commentListener(s -> sb.append(s).append('\n'));
        TextWireTest.ABC abc = new TextWireTest.ABC();

        for (String input : new String[]{
                stringA + stringB + stringC,
                stringB + stringA + stringC,
                stringC + stringA + stringB,
                stringA + stringC + stringB,
                stringB + stringC + stringA,
                stringC + stringB + stringA}) {
            wire.reset();
            wire.bytes().append(input);
            assertEquals(input, "!net.openhft.chronicle.wire.TextWireTest$ABC {\n" +
                    "  A: hi,\n" +
                    "  B: hi,\n" +
                    "  C: hi\n" +
                    "}\n", wire.getValueIn()
                    .object(abc, TextWireTest.ABC.class)
                    .toString());
            assertEquals(expectedComments,
                    Arrays.stream(sb.toString().split("\n"))
                            .filter(s -> !s.isEmpty())
                            .sorted(Collections.reverseOrder())
                            .collect(toList()));
            sb.setLength(0);
        }
    }
}
