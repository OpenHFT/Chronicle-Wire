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
        Wire text = new TextWire(bytes);
        /*
        %TAG !meta-data!
        ---
        csp:///service-lookup
        tid: 149873598325 # unique transaction id to a reply to on the channel sending the request.
        ...
        %TAG !data!
        ---
        lookup: { relativeUri: Colours }
        ...
         */
        Wires.writeData(text, true, out ->
                out.write(() -> "csp").text("///service-lookup")
                        .write(() -> "tid").int64(149873598325L));
        Wires.writeData(text, false, out ->
                out.write(() -> "lookup").marshallable(out2 ->
                        out2.write(() -> "relativeUri").text("Colours")));
        bytes.flip();
        assertEquals("[pos: 0, lim: 81, cap: 1TiB ] " +
                ")٠٠@csp: ///service-lookup⒑tid: 149873598325⒑" +
                " ٠٠٠lookup: { relativeUri: Colours }", bytes.toDebugString());
        assertEquals("%TAG !meta-data!\n" +
                "---\n" +
                "csp: ///service-lookup\n" +
                "tid: 149873598325\n" +
                "...\n" +
                "%TAG !data!\n" +
                "---\n" +
                "lookup: { relativeUri: Colours }\n" +
                "...\n", Wires.fromSizePrefixedBlobs(bytes));

    }
}
