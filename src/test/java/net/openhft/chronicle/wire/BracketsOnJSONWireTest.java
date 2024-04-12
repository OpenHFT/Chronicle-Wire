package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class BracketsOnJSONWireTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Variable to store the actual message from the wire
    String actual;

    // Interface to define a Printer with a single method 'print'
    interface Printer {
        void print(String msg);
    }

    // Test the JSON_ONLY wire type with a method writer and reader using the Printer interface
    @Test
    public void test() {

        // Create an elastic byte buffer to hold the wire data
        final Bytes<ByteBuffer> t = Bytes.elasticByteBuffer();

        // Initialize the wire with JSON_ONLY type and apply it to the buffer
        Wire wire = WireType.JSON_ONLY.apply(t);

        // Use a method writer to write a print message to the wire
        wire.methodWriter(Printer.class)
                .print("hello");

        // Assert that the wire representation matches the expected JSON format
        assertEquals("{\"print\":\"hello\"}", wire.toString());

        // Use a method reader to read the message from the wire and set the 'actual' variable
        wire.methodReader((Printer) msg -> actual = msg).readOne();

        // Release the buffer to free up resources
        t.releaseLast();

        // Assert that the read message matches the original message written to the wire
        assertEquals("hello", actual);
    }
}
