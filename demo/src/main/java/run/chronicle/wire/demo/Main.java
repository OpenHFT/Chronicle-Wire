package run.chronicle.wire.demo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;

import java.nio.ByteBuffer;

public class Main {

    public static void main(String[] args) {
        // Bytes which wraps a ByteBuffer which is resized as needed.
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        Wire wire = new TextWire(bytes);


        wire.write("message").text("Hello World");

        System.out.println(bytes);

        bytes.releaseLast();
    }

}
