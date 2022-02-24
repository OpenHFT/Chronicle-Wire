package run.chronicle.wire.demo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;

import java.util.concurrent.TimeUnit;

/**
 * Simple Example with a nested data type.
 * In this example the data is marshalled as a nested data structure.
 */
public class Example3 {
    public static void main(String[] args) {

        // Bytes what wraps a resized byte[]
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap();

        Wire wire = new TextWire(bytes);
        Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);
        wire.write("mydata").marshallable(data);

        //Prints out:
/*      mydata: {
        message: Hello World,
        number: 1234567890,
        timeUnit: NANOSECONDS,
        price: 10.5
    }*/
        System.out.println(bytes);

        Data data2 = new Data();
        wire.read("mydata").marshallable(data2);

        //Prints out:
        /*  Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}       */
        System.out.println(data2);

        //To write in binary instead
        Bytes bytes2 = new HexDumpBytes();
        Wire wire2 = new BinaryWire(bytes2);
        wire2.write("mydata").marshallable(data);

        //Prints out:
        /*
        c6 6d 79 64 61 74 61                            # mydata
        82 40 00 00 00                                  # Data
        c7 6d 65 73 73 61 67 65                         # message
        eb 48 65 6c 6c 6f 20 57 6f 72 6c 64             # Hello World
        c6 6e 75 6d 62 65 72                            # number
        a6 d2 02 96 49                                  # 1234567890
        c8 74 69 6d 65 55 6e 69 74                      # timeUnit
        eb 4e 41 4e 4f 53 45 43 4f 4e 44 53             # NANOSECONDS
        c5 70 72 69 63 65 90 00 00 28 41                # price
         */
        System.out.println(bytes2.toHexString());

        Data data3 = new Data();
        wire2.read("mydata").marshallable(data3);

        //Prints out:
        /*  Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}    */
        System.out.println(data3);

        bytes.releaseLast();
        bytes2.releaseLast();

    }
}