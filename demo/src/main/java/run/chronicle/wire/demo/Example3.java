package run.chronicle.wire.demo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Simple Example with a data type.
 * In this example the data is marshalled as a nested data structure.
 */
public class Example3 {
    public static void main(String[] args) {

        // Bytes which wraps a ByteBuffer which is resized as needed.
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        Wire wire = new TextWire(bytes);
        Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);
        wire.write(() -> "mydata").marshallable(data);

        //Prints out:
/*      mydata: {
        message: Hello World,
        number: 1234567890,
        timeUnit: NANOSECONDS,
        price: 10.5
    }*/ System.out.println(bytes);

        Data data2 = new Data();
        wire.read(() -> "mydata").marshallable(data2);

        //Prints out:
/*      Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}       */
        System.out.println(data2);

        //To write in binary instead
        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        Wire wire2 = new BinaryWire(bytes2);
        wire2.write(() -> "mydata").marshallable(data);

        //Prints out:
/*      00000000 c6 6d 79 64 61 74 61 82  40 00 00 00 c7 6d 65 73 ·mydata· @····mes
        00000010 73 61 67 65 eb 48 65 6c  6c 6f 20 57 6f 72 6c 64 sage·Hel lo World
        00000020 c6 6e 75 6d 62 65 72 a6  d2 02 96 49 c8 74 69 6d ·number· ···I·tim
        00000030 65 55 6e 69 74 eb 4e 41  4e 4f 53 45 43 4f 4e 44 eUnit·NA NOSECOND
        00000040 53 c5 70 72 69 63 65 90  00 00 28 41             S·price· ··(A
*/      System.out.println(bytes2.toHexString());

        Data data3 = new Data();
        wire2.read(() -> "mydata").marshallable(data3);

        //Prints out:
/*      Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}    */
        System.out.println(data3);

        bytes.releaseLast();
        bytes2.releaseLast();

    }
}
