package net.openhft.chronicle.wire.marshallable;


import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.Wire;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import static net.openhft.chronicle.wire.Marshallable.fromString;
import static org.junit.Assert.assertEquals;


public class NewOrderSingleSerializationTest {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(NewOrderSingle.class);
    }

    final static String NOS_STR = "!NewOrderSingle {\n" +
            "  eventTime: 0,\n" +
            "  clOrdID: myTrade,\n" + "  reason: \"\",\n" +
            "  expireDate: \"20261010\",\n" +
            "  symbol: XAU/USD,\n" +
            "  ordType: MARKET,\n" +
            "  execInst: NONE,\n" +
            "  timeInForce: GOOD_TILL_CANCEL,\n" +
            "  side: BUY,\n" +
            "  orderQty: 234,\n" +
            "  price: 123.0,\n" +
            "  transactTime: 0,\n" +
            "  partyID: IdentifiersWhoTheCounterpartyIs\n" +
            "}\n";

    private static void onNewOrderSingle(@NotNull NewOrderSingle newOrderSingle) {
        assertEquals(NOS_STR, newOrderSingle.toString());
    }

    interface NOSListener {
        void newOrderSingle(NewOrderSingle newOrderSingle);
    }

    interface NewOrderSingleListener {
        void newOrderSingle(NewOrderSingle newOrderSingle);
    }

    @Test
    public void test() {
        HexDumpBytes bytes = new HexDumpBytes();

        try {
            Wire w = new BinaryWire(bytes);
            NewOrderSingleListener newOrderSingleWriter = w.methodWriter(NewOrderSingleListener.class);
            System.out.println("bytes.toHexString() = " + bytes.toHexString());
            NewOrderSingle newOrderSingle = fromString(NewOrderSingle.class, NOS_STR);
            System.out.println("newOrderSingle.$length() = " + newOrderSingle.$length());
            newOrderSingleWriter.newOrderSingle(newOrderSingle);
            /*Assert.assertEquals("" +
                    "d9 00 00 00                                     # msg-length\n" +
                    "b9 0e 6e 65 77 4f 72 64 65 72 53 69 6e 67 6c 65 # newOrderSingle: (event)\n" +
                    "82 c4 00 00 00                                  # NewOrderSingle\n" +
                    "   c9 65 76 65 6e 74 54 69 6d 65                   # eventTime:\n" +
                    "   a1 00                                           # 0\n" +
                    "   c7 63 6c 4f 72 64 49 44 80 08 8a 6d 79 54 72 61 # clOrdID:\n" +
                    "   64 65 c6 72 65 61 73 6f 6e 80 01 8a             # reason:\n" +
                    "   ca 65 78 70 69 72 65 44 61 74 65 80 09 8a 32 30 # expireDate:\n" +
                    "   32 36 31 30 31 30 c6 73 79 6d 62 6f 6c          # symbol:\n" +
                    "   a7 d9 37 aa 80 ca 0d 00 00                      # 15163393193945\n" +
                    "   c7 6f 72 64 54 79 70 65                         # ordType:\n" +
                    "   a1 31                                           # 49\n" +
                    "   c8 65 78 65 63 49 6e 73 74                      # execInst:\n" +
                    "   a1 30                                           # 48\n" +
                    "   cb 74 69 6d 65 49 6e 46 6f 72 63 65             # timeInForce:\n" +
                    "   a1 31                                           # 49\n" +
                    "   c4 73 69 64 65                                  # side:\n" +
                    "   a1 31                                           # 49\n" +
                    "   c8 6f 72 64 65 72 51 74 79                      # orderQty:\n" +
                    "   a1 ea                                           # 234\n" +
                    "   c5 70 72 69 63 65                               # price:\n" +
                    "   a1 7b                                           # 123\n" +
                    "   cc 74 72 61 6e 73 61 63 74 54 69 6d 65          # transactTime:\n" +
                    "   a1 00                                           # 0\n" +
                    "   c7 70 61 72 74 79 49 44 80 20 8a 49 64 65 6e 74 # partyID:\n" +
                    "   69 66 69 65 72 73 57 68 6f 54 68 65 43 6f 75 6e\n" +
                    "   74 65 72 70 61 72 74 79 49 73\n", bytes.toHexString());*/


            w.methodReader((NewOrderSingleListener) NewOrderSingleSerializationTest::check).readOne();
        } catch (Exception e) {
            System.out.println("bytes.toHexString() = " + bytes.toHexString());

        }
    }

    private static void check(NewOrderSingle newOrderSingle) {
        Assert.assertEquals(NOS_STR, newOrderSingle.toString());
    }

}