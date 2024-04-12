package net.openhft.chronicle.wire.channel.echo;

import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelCfg;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChannelSecondaryConnectionTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Constant string that is expected to be returned by the secondary server in the test
    public static final String EXPECTED = "secondary is much better !";

    // Define an interface SayMsg with a single method to send a string message
    public interface SayMsg {
        void say(String say);
    }

    // Override the threadDump() method from the superclass to be called before tests
    @Override
    @Before
    public void threadDump() {
        super.threadDump();
    }

    /**
     * Test case to validate the functionality of echo handler on a secondary connection.
     * <p>
     * This test validates the scenario where if the primary server is unavailable, the client
     * is able to connect to a secondary server and perform communication. Initially, it tries
     * to connect to a non-reachable primary server, and upon failure, it tries to connect
     * to the secondary server and sends a message using the 'SayMsg' interface.
     * The expected message is defined by the 'EXPECTED' constant and the test asserts that
     * the message received from the server equals 'EXPECTED'.
         */
    @Test
    public void testEchoHandlerOnSecondaryConnection() {
        // Expect an exception with a message indicating a connection failure to be thrown during the test
        expectException("failed to connect to host-port");
        try (
            // Create a new Chronicle context and start a new gateway for client-server communication
            ChronicleContext context = ChronicleContext.newContext("tcp://:0?sessionName=testId")
        ) {
            // Start the gateway which facilitates the communication
            context.startNewGateway();

            // Configure the Chronicle Channel to be an initiator and to be buffered
            final ChronicleChannelCfg<?> channelCfg = new ChronicleChannelCfg<>().initiator(true).buffered(true);

            // Add an invalid hostname and port intending for this connection to fail
            channelCfg.addHostnamePort("localhost", 8092);

            // Add a valid hostname and port for the secondary server to connect to, on failure of the first
            channelCfg.addHostnamePort("localhost", context.port());

            try (
                // Establish a new channel, using the defined configuration and an instance of EchoHandler
                ChronicleChannel channel = ChronicleChannel.newChannel(context.socketRegistry(), channelCfg,
                        new EchoHandler())
            ) {
                // Send the 'EXPECTED' message to the secondary server using method writer of the SayMsg interface
                channel.methodWriter(SayMsg.class).say(EXPECTED);

                // Create a StringBuilder to hold the type of event and retrieve the message from the server
                final StringBuilder eventType = new StringBuilder();
                String actual = channel.readOne(eventType, String.class);

                // Assert that the message received from the server equals 'EXPECTED'
                assertEquals(EXPECTED, actual);
            }
        }
    }
}
