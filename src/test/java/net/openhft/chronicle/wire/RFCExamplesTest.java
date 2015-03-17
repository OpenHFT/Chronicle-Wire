package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RFCExamplesTest {
    /*
    ChronicleMap<Integer, String> map = context.getMap("test", Integer.class, String.class);

    map.put(1, "hello");
    map.put(2, "world");
    map.put(3, "bye");
     */
    @Test
    public void testPuts() {
        Bytes bytes = NativeBytes.nativeBytes();
/*
%TAG !meta-data!
---
csp:///service-lookup
tid: 1426502826520
...
%TAG !data!
---
lookup: { relativeUri: test, view: !Map, types: [ !Integer, !String ] }
...
 */
        Wire text = new TextWire(bytes);
        writeMessageOne(text);
        bytes.flip();
        assertEquals("%TAG !meta-data!\n" +
                "---\n" +
                "csp:///service-lookup\n" +
                "tid: 149873598325\n" +
                "...\n" +
                "%TAG !data!\n" +
                "---\n" +
                "lookup: { relativeUri: test, view: !Map types: [ !Integer !String ] }\n" +
                "...\n", Wires.fromSizePrefixedBlobs(bytes));
        assertEquals("[pos: 0, lim: 117, cap: 1TiB ] (٠٠@csp:///service-lookup⒑tid: 149873598325⒑E٠٠٠lookup: { relativeUri: test, view: !Map types: [ !Integer !String ] }", bytes.toDebugString());

        bytes.clear();
        Wire bin = new BinaryWire(bytes);
        writeMessageOne(bin);
        bytes.flip();
        assertEquals("[pos: 0, lim: 106, cap: 1TiB ] \u001F٠٠@Ãcspñ///service-lookupÃtid\u0090¦\u0094⒒RC٠٠٠Ælookup\u00827٠٠٠ËrelativeUriätestÄview¶⒊MapÅtypes\u0082⒘٠٠٠¶⒎Integer¶⒍String", bytes.toDebugString());

    }

    public void writeMessageOne(Wire text) {
        Wires.writeData(text, true, out ->
                out.write(() -> "csp").text("///service-lookup")
                        .write(() -> "tid").int64(149873598325L));
        Wires.writeData(text, false, out ->
                out.write(() -> "lookup").marshallable(out2 ->
                        out2.write(() -> "relativeUri").text("test")
                                .write(() -> "view").type("Map")
                                .write(() -> "types").sequence(vo -> {
                            vo.type("Integer");
                            vo.type("String");
                        })));
    }
}
