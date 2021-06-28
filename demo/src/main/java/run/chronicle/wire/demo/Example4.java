package run.chronicle.wire.demo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Simple example with a data type with a type.
 * In this example, the type is encoded with the data. Instead of showing the entire package name
 * which will almost certainly not work on any other platform, an alias for the type is used.
 * It also means the message is shorter and faster.
 */
public class Example4 {
    public static void main(String[] args) {

        // Bytes which wraps a ByteBuffer which is resized as needed.
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        Wire wire = new TextWire(bytes);
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
        System.out.println(bytes);

        Data data2 = wire.read("mydata").object(Data.class);

        //Prints out:
        /*  Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}    */
        System.out.println(data2);

        //To write in binary instead
        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        Wire wire2 = new BinaryWire(bytes2);
        wire2.write("mydata").object(data);

        //Prints out:
/*      00000000 c6 6d 79 64 61 74 61 b6  04 44 61 74 61 82 40 00 ·mydata· ·Data·@·
        00000010 00 00 c7 6d 65 73 73 61  67 65 eb 48 65 6c 6c 6f ···messa ge·Hello
        00000020 20 57 6f 72 6c 64 c6 6e  75 6d 62 65 72 a6 d2 02  World·n umber···
        00000030 96 49 c8 74 69 6d 65 55  6e 69 74 eb 4e 41 4e 4f ·I·timeU nit·NANO
        00000040 53 45 43 4f 4e 44 53 c5  70 72 69 63 65 90 00 00 SECONDS· price···
        00000050 28 41                                            (A                     */
        System.out.println(bytes2.toHexString());

        Data data3 = wire2.read("mydata").object(Data.class);

        //Prints out:
        /*  Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}    */
        System.out.println(data3);

        bytes.releaseLast();
        bytes2.releaseLast();
    }

}