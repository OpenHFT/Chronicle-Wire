package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.converter.NanoTime;
import net.openhft.chronicle.wire.converter.ShortText;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The purpose of this test to show a HexDumpBytes of a realistic series of events
 */
public class HexDumpMethodWriterTest {
    @Test
    public void hexDump() {
        Wire wire = WireType.BINARY_LIGHT.apply(new HexDumpBytes());
        EventsOut eventsOut = wire.methodWriter(EventsOut.class);
        NewOrderSingle nos = new NewOrderSingle();
        nos.clOrdId = "ORD-12345678";
        nos.transactTime = NanoTime.INSTANCE.parse("2021-01-01T12:34:56.123456789");
        nos.symbol = ShortText.INSTANCE.parse("MSFT");
        nos.price = 123.45;
        nos.orderQty = 1000;
        assertEquals("" +
                        "!net.openhft.chronicle.wire.method.HexDumpMethodWriterTest$NewOrderSingle {\n" +
                        "  transactTime: 2021-01-01T12:34:56.123456789,\n" +
                        "  symbol: MSFT,\n" +
                        "  clOrdId: ORD-12345678,\n" +
                        "  price: 123.45,\n" +
                        "  orderQty: 1E3\n" +
                        "}\n",
                nos.toString());
        NewOrderSingle nos2 = Marshallable.fromString(nos.toString());
        assertEquals(nos, nos2);

        eventsOut.newOrderSingle(nos);
        assertEquals("" +
                        "61 00 00 00                                     # msg-length\n" +
                        "b9 0e 6e 65 77 4f 72 64 65 72 53 69 6e 67 6c 65 # newOrderSingle: (event)\n" +
                        "82 4c 00 00 00                                  # NewOrderSingle\n" +
                        "   cc 74 72 61 6e 73 61 63 74 54 69 6d 65          # transactTime:\n" +
                        "   a7 15 2d d5 cb cf 1b 56 16                      # 1609504496123456789\n" +
                        "   c6 73 79 6d 62 6f 6c                            # symbol:\n" +
                        "   a6 16 a4 13 01                                  # 18064406\n" +
                        "   c7 63 6c 4f 72 64 49 64                         # clOrdId:\n" +
                        "   ec 4f 52 44 2d 31 32 33 34 35 36 37 38          # ORD-12345678\n" +
                        "   c5 70 72 69 63 65                               # price:\n" +
                        "   92 b9 60                                        # 12345/1e2\n" +
                        "   c8 6f 72 64 65 72 51 74 79                      # orderQty:\n" +
                        "   a2 e8 03                                        # 1000\n",
                wire.bytes().toHexString());
    }

    interface EventsOut {
        void newOrderSingle(NewOrderSingle nos);
    }

    static class NewOrderSingle extends SelfDescribingMarshallable {
        @NanoTime
        long transactTime;
        @ShortText
        long symbol;
        String clOrdId;
        double price;
        double orderQty;
    }

}
