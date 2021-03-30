package run.chronicle.wire.demo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * This is a simple example with a data type.
 * See Data.java for the code for Data type. This Example is much the same as Example1, with the
 * code required wrapped in a method.
 */
public class Example2 {

    public static void main(String[] args) {

    // Bytes which wraps a ByteBuffer which is resized as needed.
    Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

    Wire wire = new TextWire(bytes);

    Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);
    data.writeMarshallable(wire);

        //Prints out:
/*       message: Hello World
         number: 1234567890
         code: NANOSECONDS
         price: 10.5 */
    System.out.println(bytes);


    Data data2 = new Data();
    data2.readMarshallable(wire);

        //Prints out:
/*      Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}    */
        System.out.println(data2);

   //To write in binary instead

    Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
    Wire wire2 = new BinaryWire(bytes2);
    data.writeMarshallable(wire2);

    //Prints out:
/*      00000000 c7 6d 65 73 73 61 67 65  eb 48 65 6c 6c 6f 20 57 ·message ·Hello W
        00000010 6f 72 6c 64 c6 6e 75 6d  62 65 72 a6 d2 02 96 49 orld·num ber····I
        00000020 c8 74 69 6d 65 55 6e 69  74 eb 4e 41 4e 4f 53 45 ·timeUni t·NANOSE
        00000030 43 4f 4e 44 53 c5 70 72  69 63 65 90 00 00 28 41 CONDS·pr ice···(A
 */ System.out.println(bytes2.toHexString());

    Data data3 = new Data();
    data3.readMarshallable(wire2);

    //Prints out:
/*  Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}      */
    System.out.println(data3);

        bytes.releaseLast();
        bytes2.releaseLast();
  }

}
