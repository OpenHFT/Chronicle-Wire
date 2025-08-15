package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import org.junit.Test;

import static net.openhft.chronicle.bytes.Bytes.elasticHeapByteBuffer;

/**
 * Demonstrates a simple round-trip of writing and reading method calls
 * over a Chronicle Wire instance using a method writer and method reader.
 * <p>
 * This test shows how an object implementing {@link Demarshallable} can be
 * transmitted via Chronicle Wire and reconstituted on the receiving end.
 */

public class GenericWireMethodReaderTest {

    /**
     * A generic message container that supports marshalling and demarshalling
     * using Chronicle Wire. The payload type is generic, but at runtime
     * serialization will depend on the actual type provided.
     *
     * @param <T> the type of the message payload
     */
    public static class Message<T> extends SelfDescribingMarshallable implements Demarshallable {
        private T payload;

        /**
         * Constructs a {@code Message} instance by reading its state from the provided
         * {@link WireIn} source. This is used by Chronicle Wire during demarshalling.
         *
         * @param w the {@link WireIn} source to read from
         */
        @UsedViaReflection
        public Message(WireIn w) {
            readMarshallable(w);
        }

        /**
         * Constructs a {@code Message} instance with the given payload.
         *
         * @param payload the message content
         */
        public Message(T payload) {
            this.payload = payload;
        }
    }

    /**
     * Listener interface for receiving messages. Uses {@code Object} as the
     * parameter type to allow flexibility in message payload types.
     */
    public interface MessageListener {
        /**
         * Handles an incoming message.
         *
         * @param value the message payload
         */
        void onMessage(Object value);
    }

    /**
     * Writes two messages into a {@link BinaryWire} using a method writer,
     * then reads them back using a method reader.
     * <p>
     * This verifies that messages written with Chronicle Wire's method writer
     * can be correctly deserialized by a method reader implementation.
     */
    @Test
    public void writesAndReadsMultipleMessages() {
        Wire wire = new BinaryWire(elasticHeapByteBuffer());

        MessageListener writer = wire.methodWriter(MessageListener.class);
        writer.onMessage(new Message<>("msg1"));
        writer.onMessage(new Message<>("msg2"));

        try (MethodReader reader = wire.methodReader((MessageListener) System.out::println)) {
            reader.readOne();
            reader.readOne();
        }
    }
}