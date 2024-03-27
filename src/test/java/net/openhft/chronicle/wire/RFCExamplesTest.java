/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
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

    // This is what this test is trying to recreate.
    /*
    ChronicleMap<Integer, String> map = context.getMap("test", Integer.class, String.class);

    map.put(1, "hello");
    map.put(2, "world");
    map.put(3, "bye");
     */
    @Test
    public void testPuts() {
        // Allocate an elastic buffer on heap.
        @NotNull Bytes<?> bytes = Bytes.allocateElasticOnHeap();

/*
        This represents a serialized format of the metadata and data.
        It shows the service lookup, the type of view (Map), and the key/value types.
         */
/*
--- !!meta-data
csp:///service-lookup
tid: 1426502826520
--- !!data
lookup: { relativeUri: test, view: !Map, types: [ !Integer, !String ] }
 */
        @NotNull Wire text = WireType.TEXT.apply(bytes);
        text.usePadding(true);
        writeMessageOne(text);

        // System.out.println(Wires.fromSizePrefixedBlobs(bytes));
        // Validate the serialization result.
        assertEquals("" +
                        "--- !!meta-data\n" +
                        "csp: ///service-lookup\n" +
                        "tid: 149873598325\n" +
                        "# position: 48, header: 0\n" +
                        "--- !!data\n" +
                        "lookup: {\n" +
                        "  relativeUri: test,\n" +
                        "  view: !type Map,\n" +
                        "  types: {\n" +
                        "    keyType: !type Integer,\n" +
                        "    valueType: !type String\n" +
                        "  }\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(bytes));

        // Create a binary representation of the wire.
        @NotNull Wire wire = new BinaryWire(bytes);
        wire.usePadding(true);

        clear(bytes);
        writeMessageOne(wire);

        // Validate the binary representation.
        assertEquals("" +
                        "[pos: 0, rlim: 132, wlim: 2147483632, cap: 2147483632 ] ǁ$٠٠@Ãcspñ///service-lookupÃtid§u\\u009F)å\"٠٠٠\\u008FX٠٠٠Ælookup\\u0082I٠٠٠ËrelativeUriätestÄview¼⒊MapÅtypes\\u0082#٠٠٠ÇkeyType¼⒎IntegerÉvalueType¼⒍String\\u008F\\u008F\\u008F‡٠٠٠٠٠٠٠٠٠٠٠٠٠",
                bytes.toDebugString());

        // Create a raw representation of the wire.
        @NotNull Wire raw = new RawWire(bytes);
        raw.usePadding(true);
        clear(bytes);
        writeMessageOne(raw);

        // Validate the raw representation.
        assertEquals("" +
                        "[pos: 0, rlim: 68, wlim: 2147483632, cap: 2147483632 ] ǁ\\u001C٠٠@⒘///service-lookupu\\u009F)å\"٠٠٠٠٠ ٠٠٠\\u001C٠٠٠⒋test⒊Map⒖٠٠٠⒎Integer⒍String‡٠٠٠٠٠٠٠٠",
                bytes.toDebugString());

        /*
        This serialized format is supposed to be a representation of put operations.
        It shows the metadata (like the server URL and transaction id) and the key/value pairs to put.
         */
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

        // Validate the serialized format of the put operations.
        assertEquals("" +
                        "--- !!meta-data\n" +
                        "csp: //server1/test\n" +
                        "cid: 1\n" +
                        "# position: 32, header: 0\n" +
                        "--- !!data\n" +
                        "put: {\n" +
                        "  key: 1,\n" +
                        "  value: hello\n" +
                        "}\n" +
                        "# position: 72, header: 1\n" +
                        "--- !!data\n" +
                        "put: {\n" +
                        "  key: 2,\n" +
                        "  value: world\n" +
                        "}\n" +
                        "# position: 112, header: 2\n" +
                        "--- !!data\n" +
                        "put: {\n" +
                        "  key: 3,\n" +
                        "  value: bye\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(bytes));
        assertEquals("" +
                        "[pos: 0, rlim: 148, wlim: 2147483632, cap: 2147483632 ] ǁ\\u001C٠٠@csp: //server1/test⒑cid: 1⒑ $٠٠٠put: {⒑  key: 1,⒑  value: hello⒑}⒑  $٠٠٠put: {⒑  key: 2,⒑  value: world⒑}⒑   ٠٠٠put: {⒑  key: 3,⒑  value: bye⒑}⒑‡٠٠٠٠٠٠٠٠٠٠٠٠٠",
                bytes.toDebugString());

        clear(bytes);
        writeMessageTwo(wire);

        // System.out.println(Wires.fromSizePrefixedBlobs(bytes));
        // Validate the binary format of the put operations.
        assertEquals("" +
                        "[pos: 0, rlim: 128, wlim: 2147483632, cap: 2147483632 ] ǁ\\u001C٠٠@Ãcspî//server1/testÃcid¡⒈\\u008F\\u008F\\u008F\\u001C٠٠٠Ãput\\u0082⒙٠٠٠Ãkey¡⒈Åvalueåhello\\u008F\\u001C٠٠٠Ãput\\u0082⒙٠٠٠Ãkey¡⒉Åvalueåworld\\u008F\\u001C٠٠٠Ãput\\u0082⒗٠٠٠Ãkey¡⒊Åvalueãbye\\u008F\\u008F\\u008F‡٠٠٠٠٠٠٠٠٠",
                bytes.toDebugString());

        clear(bytes);
        writeMessageTwo(raw);

        // Validate the raw format of the put operations.
        assertEquals("" +
                        "[pos: 0, rlim: 96, wlim: 2147483632, cap: 2147483632 ] ǁ\\u0018٠٠@⒕//server1/test⒈٠٠٠٠٠٠٠٠⒛٠٠٠⒕٠٠٠⒈٠٠٠٠٠٠٠⒌hello٠٠⒛٠٠٠⒕٠٠٠⒉٠٠٠٠٠٠٠⒌world٠٠⒗٠٠٠⒓٠٠٠⒊٠٠٠٠٠٠٠⒊bye‡٠٠٠٠٠٠٠٠",
                bytes.toDebugString());
    }

    // Clear the byte buffer.
    @SuppressWarnings("rawtypes")
    public void clear(@NotNull Bytes<?> bytes) {
        bytes.clear();
        bytes.zeroOut(0, bytes.realCapacity());
    }

    /**
     * Serializes metadata and data using the given Wire object.
     * This method writes information related to service lookup and its properties.
     *
     * @param wire The Wire object used for serialization.
     */
    public void writeMessageOne(@NotNull Wire wire) {
        // Write meta-data with the given service lookup and transaction id.
        wire.writeDocument(true, out ->
                out.write(csp).text("///service-lookup")
                        .write(tid).int64(149873598325L));

        // Write actual data, containing a relative URI, view type, and key-value types.
        wire.writeDocument(false, out ->
                out.write(lookup).marshallable(out2 ->
                        out2.write(relativeUri).text("test")
                                .write(view).typeLiteral("Map")
                                .write(types).marshallable(m ->
                                m.write(() -> "keyType").typeLiteral("Integer")
                                        .write(() -> "valueType").typeLiteral("String"))));

        // Uncomment to print the serialized data.
        // System.out.println(wire);
    }

    /**
     * Serializes metadata and multiple 'put' operations using the given Wire object.
     * This method writes information related to server location and a series of key-value pairs.
     *
     * @param wire The Wire object used for serialization.
     */
    private void writeMessageTwo(@NotNull Wire wire) {
        // Write meta-data with server location and CID.
        wire.writeDocument(true, out ->
                out.write(() -> "csp").text("//server1/test")
                        .write(() -> "cid").int64(1));

        // Define key-values to be serialized.
        @NotNull String[] words = ",hello,world,bye".split(",");

        // Iterate over the words and write each as a 'put' operation.
        for (int i = 1; i < words.length; i++) {
            int n = i;
            wire.writeDocument(false, out ->
                    out.write(() -> "put").marshallable(m -> m.write(() -> "key").int64(n)
                            .write(() -> "value").text(words[n])));
        }
    }

    /**
     * Enum representing various fields used in the serialization.
     */
    enum Fields implements WireKey {
        csp, tid, lookup, relativeUri, view, types
    }
}
