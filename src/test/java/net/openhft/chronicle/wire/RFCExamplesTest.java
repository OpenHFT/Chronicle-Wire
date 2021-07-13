/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static net.openhft.chronicle.wire.RFCExamplesTest.Fields.*;
import static org.junit.Assert.assertEquals;

/* Based on
https://github.com/OpenHFT/RFC/blob/master/Chronicle/Engine/Remote/Chronicle-Engine-0.1.md
 */
public class RFCExamplesTest extends WireTestCommon {
    /*
    ChronicleMap<Integer, String> map = context.getMap("test", Integer.class, String.class);

    map.put(1, "hello");
    map.put(2, "world");
    map.put(3, "bye");
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testPuts() {
        @NotNull Bytes bytes = Bytes.allocateElasticOnHeap();
/*
--- !!meta-data
csp:///service-lookup
tid: 1426502826520
--- !!data
lookup: { relativeUri: test, view: !Map, types: [ !Integer, !String ] }
 */
        @NotNull Wire text = new TextWire(bytes);
        text.usePadding(true);
        writeMessageOne(text);

        // System.out.println(Wires.fromSizePrefixedBlobs(bytes));
        assertEquals("--- !!meta-data\n" +
                        "csp: ///service-lookup\n" +
                        "tid: 149873598325\n" +
                        "\0\0\0\n" +
                        "# position: 48, header: 0\n" +
                        "--- !!data\n" +
                        "lookup: {\n" +
                        "  relativeUri: test,\n" +
                        "  view: !type Map,\n" +
                        "  types: {\n" +
                        "    keyType: !type Integer,\n" +
                        "    valueType: !type String\n" +
                        "  }\n" +
                        "}\n" +
                        "\0\n",
                Wires.fromSizePrefixedBlobs(bytes));

        @NotNull Wire wire = new BinaryWire(bytes);
        wire.usePadding(true);

        clear(bytes);
        writeMessageOne(wire);

        // System.out.println(Wires.fromSizePrefixedBlobs(bytes));
        assertEquals("[pos: 0, rlim: 132, wlim: 2147483632, cap: 2147483632 ] ǁ$٠٠@Ãcspñ///service-lookupÃtid§u\\u009F)å\"٠٠٠٠X٠٠٠Ælookup\\u0082I٠٠٠ËrelativeUriätestÄview¼⒊MapÅtypes\\u0082#٠٠٠ÇkeyType¼⒎IntegerÉvalueType¼⒍String٠٠٠‡٠٠٠٠٠٠٠٠",
                bytes.toDebugString());

        @NotNull Wire raw = new RawWire(bytes);
        raw.usePadding(true);
        clear(bytes);
        writeMessageOne(raw);

        assertEquals("[pos: 0, rlim: 68, wlim: 2147483632, cap: 2147483632 ] ǁ\\u001C٠٠@⒘///service-lookupu\\u009F)å\"٠٠٠٠٠ ٠٠٠\\u001C٠٠٠⒋test⒊Map⒖٠٠٠⒎Integer⒍String‡٠٠٠٠٠٠٠٠",
                bytes.toDebugString());
/*
--- !!meta-data
cid: 1
# or
csp://server1/test
tid: 1426502826525
--- !!data
put: [ 1, hello ]
--- !!data
put: [ 2, world ]
--- !!data
put: [ 3, bye ]
*/
        clear(bytes);
        writeMessageTwo(text);
        assertEquals("" +
                        "--- !!meta-data\n" +
                        "csp: //server1/test\n" +
                        "cid: 1\n" +
                        "\0\n" +
                        "# position: 32, header: 0\n" +
                        "--- !!data\n" +
                        "put: {\n" +
                        "  key: 1,\n" +
                        "  value: hello\n" +
                        "}\n" +
                        "\0\0\n" +
                        "# position: 72, header: 1\n" +
                        "--- !!data\n" +
                        "put: {\n" +
                        "  key: 2,\n" +
                        "  value: world\n" +
                        "}\n" +
                        "\0\0\n" +
                        "# position: 112, header: 2\n" +
                        "--- !!data\n" +
                        "put: {\n" +
                        "  key: 3,\n" +
                        "  value: bye\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(bytes));
        assertEquals("" +
                        "[pos: 0, rlim: 148, wlim: 2147483632, cap: 2147483632 ] ǁ\\u001C٠٠@csp: //server1/test⒑cid: 1⒑٠$٠٠٠put: {⒑  key: 1,⒑  value: hello⒑}⒑٠٠$٠٠٠put: {⒑  key: 2,⒑  value: world⒑}⒑٠٠ ٠٠٠put: {⒑  key: 3,⒑  value: bye⒑}⒑‡٠٠٠٠٠٠٠٠",
                bytes.toDebugString());

        clear(bytes);
        writeMessageTwo(wire);

        // System.out.println(Wires.fromSizePrefixedBlobs(bytes));
        assertEquals("" +
                        "[pos: 0, rlim: 120, wlim: 2147483632, cap: 2147483632 ] ǁ\\u0018٠٠@Ãcspî//server1/testÃcid⒈\\u001C٠٠٠Ãput\\u0082⒘٠٠٠Ãkey⒈Åvalueåhello٠٠\\u001C٠٠٠Ãput\\u0082⒘٠٠٠Ãkey⒉Åvalueåworld٠٠\\u0018٠٠٠Ãput\\u0082⒖٠٠٠Ãkey⒊Åvalueãbye‡٠٠٠٠٠٠٠٠",
                bytes.toDebugString());

        clear(bytes);
        writeMessageTwo(raw);
        assertEquals("" +
                        "[pos: 0, rlim: 96, wlim: 2147483632, cap: 2147483632 ] ǁ\\u0018٠٠@⒕//server1/test⒈٠٠٠٠٠٠٠٠⒛٠٠٠⒕٠٠٠⒈٠٠٠٠٠٠٠⒌hello٠٠⒛٠٠٠⒕٠٠٠⒉٠٠٠٠٠٠٠⒌world٠٠⒗٠٠٠⒓٠٠٠⒊٠٠٠٠٠٠٠⒊bye‡٠٠٠٠٠٠٠٠",
                bytes.toDebugString());
    }

    @SuppressWarnings("rawtypes")
    public void clear(@NotNull Bytes bytes) {
        bytes.clear();
        bytes.zeroOut(0, bytes.realCapacity());
    }

    public void writeMessageOne(@NotNull Wire wire) {
        wire.writeDocument(true, out ->
                out.write(csp).text("///service-lookup")
                        .write(tid).int64(149873598325L));
        wire.writeDocument(false, out ->
                out.write(lookup).marshallable(out2 ->
                        out2.write(relativeUri).text("test")
                                .write(view).typeLiteral("Map")
                                .write(types).marshallable(m ->
                                m.write(() -> "keyType").typeLiteral("Integer")
                                        .write(() -> "valueType").typeLiteral("String"))));
        // System.out.println(wire);
    }

    private void writeMessageTwo(@NotNull Wire wire) {
        wire.writeDocument(true, out ->
                out.write(() -> "csp").text("//server1/test")
                        .write(() -> "cid").int64(1));
        @NotNull String[] words = ",hello,world,bye".split(",");
        for (int i = 1; i < words.length; i++) {
            int n = i;
            wire.writeDocument(false, out ->
                    out.write(() -> "put").marshallable(m -> m.write(() -> "key").int64(n)
                            .write(() -> "value").text(words[n])));
        }
    }

    enum Fields implements WireKey {
        csp, tid, lookup, relativeUri, view, types
    }
}
