package run.chronicle.wire.demo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class Example1 {
    public static void main(String[] args) {

        // Bytes which wraps a ByteBuffer which is resized as needed.
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        //Now you can choose which format you are using. As the wire formats are themselves
        // unbuffered, you can use them with the same buffer, but in general using one wire format is easier.
        //Options are: BinaryWire, TextWire, JSONWire, CSVWire, QueryWire, YamlWire, RawWire
        Wire wire = new TextWire(bytes);
        //or
        WireType wireType = WireType.TEXT;
        Wire wireB = wireType.apply(bytes);

        //Another Wire with Binary format.
        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        Wire wire2 = new BinaryWire(bytes2);

        //Another Wire with RawWire format.
        Bytes<ByteBuffer> bytes3 = Bytes.elasticByteBuffer();
        Wire wire3 = new RawWire(bytes3);

        //Write to the wire with a simple document.
        wire.write("message").text("Hello World")
                .write("number").int64(1234567890L)
                .write("code").asEnum(TimeUnit.SECONDS)
                .write("price").float64(10.50);

        //Prints out:
/*         message: Hello World
           number: 1234567890
           code: SECONDS
           price: 10.5 */
        System.out.println(bytes);

        //The same code as Binary wire. Using toHexString prints out hex view of the buffer's contents.
        wire2.write("message").text("Hello World")
                .write("number").int64(1234567890L)
                .write("code").asEnum(TimeUnit.SECONDS)
                .write("price").float64(10.50);


        //Prints out:
/*      00000000 c7 6d 65 73 73 61 67 65  eb 48 65 6c 6c 6f 20 57 ·message ·Hello W
        00000010 6f 72 6c 64 c6 6e 75 6d  62 65 72 a6 d2 02 96 49 orld·num ber····I
        00000020 c4 63 6f 64 65 e7 53 45  43 4f 4e 44 53 c5 70 72 ·code·SE CONDS·pr
        00000030 69 63 65 90 00 00 28 41                          ice···(A             */
        System.out.println(bytes2.toHexString());

        // The same code as RawWire
        //Using RawWire strips away all the meta data to reduce the size of the message, and improve speed.
        // The down-side is that we cannot easily see what the message contains.
        wire3.write("message").text("Hello World")
                .write("number").int64(1234567890L)
                .write("code").asEnum(TimeUnit.SECONDS)
                .write("price").float64(10.50);

        //Prints out:
/*      00000000 0b 48 65 6c 6c 6f 20 57  6f 72 6c 64 d2 02 96 49 ·Hello W orld···I
        00000010 00 00 00 00 07 53 45 43  4f 4e 44 53 00 00 00 00 ·····SEC ONDS····
        00000020 00 00 25 40                                      ··%@                     */
        System.out.println(bytes3.toHexString());

        // to obtain the underlying ByteBuffer to write to a Channel
        ByteBuffer byteBuffer = bytes2.underlyingObject();
        byteBuffer.position(0);
        byteBuffer.limit(bytes2.length());

        bytes.releaseLast();
        bytes2.releaseLast();


    }
}
