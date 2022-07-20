package run.chronicle.wire.demo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;

import java.util.concurrent.TimeUnit;

/**
 * Simple example with a data type with a type.
 * In this example, the type is encoded with the data. Instead of showing the entire package name
 * which will almost certainly not work on any other platform, an alias for the type is used.
 * It also means the message is shorter and faster.
 */
public class Example4 {
    public static void main(String[] args) {

        Wire wire = new TextWire(Bytes.allocateElasticOnHeap());
        ClassAliasPool.CLASS_ALIASES.addAlias(Data.class);
        Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);
        wire.write("mydata").object(data);

        //Prints out:
/*      mydata: !Data {
        message: Hello World,
        number: 1234567890,
        timeUnit: NANOSECONDS,
        price: 10.5
    }*/
        System.out.println(wire);

        Data data2 = wire.read("mydata").object(Data.class);

        //Prints out:
        /*  Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}    */
        System.out.println(data2);

        //To write in binary instead
        Wire wire2 = new BinaryWire(new HexDumpBytes());
        wire2.write("mydata").object(data);

        //Prints out:
/*
c6 6d 79 64 61 74 61                            # mydata
b6 04 44 61 74 61                               # Data
80 40                                           # Data
c7 6d 65 73 73 61 67 65                         # message
eb 48 65 6c 6c 6f 20 57 6f 72 6c 64             # Hello World
c6 6e 75 6d 62 65 72                            # number
a6 d2 02 96 49                                  # 1234567890
c8 74 69 6d 65 55 6e 69 74                      # timeUnit
eb 4e 41 4e 4f 53 45 43 4f 4e 44 53             # NANOSECONDS
c5 70 72 69 63 65 90 00 00 28 41                # price
 */
        System.out.println(wire2.bytes().toHexString());

        Data data3 = wire2.read("mydata").object(Data.class);

        //Prints out:
        /*  Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}    */
        System.out.println(data3);

        wire.bytes().releaseLast();
        wire2.bytes().releaseLast();
    }

}