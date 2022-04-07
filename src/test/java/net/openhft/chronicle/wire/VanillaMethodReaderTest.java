package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.wire.marshallable.TriviallyCopyableMarketData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VanillaMethodReaderTest {

    @Test
    public void logMessage0() {
        TriviallyCopyableMarketData data = new TriviallyCopyableMarketData();
        data.securityId(0x828282828282L);

        Wire wire = WireType.BINARY_LIGHT.apply(new HexDumpBytes());
        wire.methodWriter(ITCO.class).marketData(data);
        assertEquals("" +
                        "9e 00 00 00                                     # msg-length\n" +
                        "b9 0a 6d 61 72 6b 65 74 44 61 74 61             # marketData: (event)\n" +
                        "80 90 82 82 82 82 82 82 00 00 00 00 00 00 00 00 # TriviallyCopyableMarketData\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00\n",
                wire.bytes().toHexString());
        try (DocumentContext dc = wire.readingDocument()) {
            final ValueIn marketData = dc.wire().read("marketData");
            assertEquals("" +
                            "read md - 00000010 80 90 82 82 82 82 82 82  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "00000020 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "........\n" +
                            "000000a0 00 00                                            ··               ",
                    VanillaMethodReader.logMessage0("md", marketData));
        }
    }

    interface ITCO {
        void marketData(TriviallyCopyableMarketData tcmd);
    }

}