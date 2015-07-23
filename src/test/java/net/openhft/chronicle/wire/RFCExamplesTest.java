/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

import static net.openhft.chronicle.wire.RFCExamplesTest.Fields.*;
import static org.junit.Assert.assertEquals;

/* Based on
https://github.com/OpenHFT/RFC/blob/master/Chronicle/Engine/Remote/Chronicle-Engine-0.1.md
 */
public class RFCExamplesTest {
    /*
    ChronicleMap<Integer, String> map = context.getMap("test", Integer.class, String.class);

    map.put(1, "hello");
    map.put(2, "world");
    map.put(3, "bye");
     */
    @Test
    @Ignore
    public void testPuts() {
        Bytes bytes = NativeBytes.nativeBytes();
/*
--- !!meta-data
csp:///service-lookup
tid: 1426502826520
--- !!data
lookup: { relativeUri: test, view: !Map, types: [ !Integer, !String ] }
 */
        Bytes ebytes = Bytes.expect("(\0\0@csp:///service-lookup\n" +
                "tid: 149873598325\n" +
                "F\0\0\0lookup: { relativeUri: test, view: !Map types: [ !Integer, !String ] }");
        Wire text = new TextWire(bytes);
        writeMessageOne(text);

        System.out.println(Wires.fromSizePrefixedBinaryToText(bytes));
        assertEquals("--- !!meta-data\n" +
                        "csp: ///service-lookup\n" +
                        "tid: 149873598325\n" +
                        "--- !!data\n" +
                        "lookup: {\n" +
                        "  relativeUri: test,\n" +
                        "  view: !Map types: {\n" +
                        "    keyType: !Integer valueType: !String }\n" +
                        "}",
                Wires.fromSizePrefixedBlobs(bytes));

        Wire bin = new BinaryWire(bytes);
        bytes.clear();
        writeMessageOne(bin);

        System.out.println(Wires.fromSizePrefixedBinaryToText(bytes));
        assertEquals("[pos: 0, lim: 124, cap: 8EiB ] \u001F٠٠@Ãcspñ///service-lookup" +
                "Ãtid\u0090¦\u0094⒒RU" +
                "٠٠٠Ælookup\u0082I٠٠٠ËrelativeUriätestÄview¶⒊MapÅtypes\u0082#٠٠٠ÇkeyType¶⒎IntegerÉvalueType¶⒍String", bytes.toDebugString());

        Wire raw = new RawWire(bytes);
        bytes.clear();
        writeMessageOne(raw);

        assertEquals("[pos: 0, lim: 79, cap: 8EiB ] " +
                "\u001A٠٠@⒘///service-lookupu\u009F)å\"٠٠٠-٠٠٠" +
                "⒍lookup\"٠٠٠⒋test⒊Map⒌types⒖٠٠٠⒎Integer⒍String", bytes.toDebugString());
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
        bytes.clear();
        writeMessageTwo(text);
        assertEquals("--- !!meta-data\n" +
                        "\n" +
                        "csp://server1/test\n" +
                        "cid: 1\n" +
                        "--- !!data\n" +
                        "put: { key: 1, value: hello }\n" +
                        "--- !!data\n" +
                        "\n" +
                        "put: { key: 2, value: world }\n" +
                        "--- !!data\n" +
                        "\n" +
                        "put: { key: 3, value: bye\n",
                Wires.fromSizePrefixedBlobs(bytes));
        assertEquals("[pos: 0, lim: 130, cap: 8EiB ] " +
                "\u001B٠٠@⒑csp://server1/test⒑cid: 1⒑" +
                "\u001D٠٠٠put: { key: 1, value: hello }" +
                "\u001E٠٠٠⒑put: { key: 2, value: world }" +
                "\u001C٠٠٠⒑put: { key: 3, value: bye }", bytes.toDebugString());

        bytes.clear();
        writeMessageTwo(bin);

        System.out.println(Wires.fromSizePrefixedBinaryToText(bytes));
        assertEquals("[pos: 0, lim: 86, cap: 8EiB ] " +
                "\u0018٠٠@Ãcspî//server1/testÃcid⒈" +
                "⒗٠٠٠Ãput\u0082⒎٠٠٠⒈åhello" +
                "⒗٠٠٠Ãput\u0082⒎٠٠٠⒉åworld" +
                "⒕٠٠٠Ãput\u0082⒌٠٠٠⒊ãbye", bytes.toDebugString());

        bytes.clear();
        writeMessageTwo(raw);
        assertEquals("[pos: 0, lim: 103, cap: 8EiB ] " +
                "\u0017٠٠@⒕//server1/test⒈٠٠٠٠٠٠٠\u0016" +
                "٠٠٠⒊put⒕٠٠٠⒈٠٠٠٠٠٠٠⒌hello\u0016" +
                "٠٠٠⒊put⒕٠٠٠⒉٠٠٠٠٠٠٠⒌world⒛" +
                "٠٠٠⒊put⒓٠٠٠⒊٠٠٠٠٠٠٠⒊bye", bytes.toDebugString());
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
    }

    private void writeMessageTwo(@NotNull Wire wire) {
        wire.writeDocument(true, out ->
                out.write(() -> "csp").text("//server1/test")
                        .write(() -> "cid").int64(1));
        String[] words = ",hello,world,bye".split(",");
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
