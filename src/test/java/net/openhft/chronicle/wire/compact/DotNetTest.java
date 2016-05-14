package net.openhft.chronicle.wire.compact;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

/**
 * Created by peter on 14/05/16.
 */
public class DotNetTest {
    @Test
    public void testCode() {
        final Bytes bytes = Bytes.fromHexString("000000: B9 06 75 73 65 72 49 64 E5 61 6E 64 72 65 B9 06\n" +
                "000016: 64 6F 6D 61 69 6E EB 54 45 53 54 2D 44 4F 4D 41\n" +
                "000032: 49 4E B9 0D 73 65 63 75 72 69 74 79 54 6F 6B 65\n" +
                "000048: 6E EE 53 69 6D 70 6C 65 50 61 73 73 77 6F 72 64\n" +
                "000064: B9 08 63 6C 69 65 6E 74 49 64 B8 24 34 62 34 32\n" +
                "000080: 35 61 31 38 2D 61 62 34 39 2D 34 65 39 37 2D 39\n" +
                "000096: 66 65 38 2D 65 37 36 30 32 38 38 31 34 34 64 39\n");
        System.out.println(bytes.toHexString());
        Wire wire = new BinaryWire(bytes);
        Bytes text = Bytes.allocateElasticDirect();
        wire.copyTo(new TextWire(text));
        System.out.println(text);
    }
}
