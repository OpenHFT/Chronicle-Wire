package net.openhft.chronicle.wire.channel.echo;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.channel.*;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class EchoHandlerTest extends WireTestCommon {

    @Test
    public void internal() {
        String url = "internal://";
        try (ChronicleContext context = ChronicleContext.newContext(url)) {
            doTest(context, false);
        }
    }

    @Test
    public void server() {
        String url = "tcp://:0";
        IOTools.deleteDirWithFiles("target/server");
        try (ChronicleContext context = ChronicleContext.newContext(url)
                .name("target/server")
                .buffered(true)
                .useAffinity(true)) {
            doTest(context, false);
        }
    }

    @Test
    @Ignore(/* TODO FIX */)
    public void serverBuffered() {
        ignoreException("Closed");
        if (Jvm.isArm()) {
            ignoreException("Using Pauser.balanced() as not enough processors");
            ignoreException("bgWriter died");
        }
        String url = "tcp://:0";
        IOTools.deleteDirWithFiles("target/server");
        try (ChronicleContext context = ChronicleContext.newContext(url)
                .name("target/server")
                .buffered(true)
                .useAffinity(true)) {
            doTest(context, true);
        }
    }

    @Test
    public void redirectedServer() throws IOException {
        ignoreException("ClosedIORuntimeException");
        String urlZzz = "tcp://localhost:65329";
        String url0 = "tcp://localhost:65330";
        String url1 = "tcp://localhost:65331";
        try (ChronicleGatewayMain gateway0 = new ChronicleGatewayMain(url0)) {
            gateway0.name("target/zero");
            // gateway that will handle the request
            gateway0.start();
            try (ChronicleGatewayMain gateway1 = new ChronicleGatewayMain(url1) {
                @Override
                protected ChannelHeader redirect(ChannelHeader channelHandler) {
                    return new RedirectHeader(Arrays.asList(urlZzz, url0));
                }
            }) {
                gateway1.name("target/one");
                // gateway that will handle the redirect request
                gateway1.start();

                try (ChronicleContext context = ChronicleContext.newContext(url1).name("target/client")) {
                    doTest(context, false);
                }
            }
        }
    }

    private void doTest(ChronicleContext context, Boolean buffered) {
        final EchoHandler echoHandler = new EchoHandler().buffered(buffered);
        ChronicleChannel channel = context.newChannelSupplier(echoHandler).connectionTimeoutSecs(1).get();
        Says says = channel.methodWriter(Says.class);
        says.say("Hello World");

        StringBuilder eventType = new StringBuilder();
        String text = channel.readOne(eventType, String.class);
        assertEquals("say: Hello World",
                eventType + ": " + text);
        try (DocumentContext dc = channel.readingDocument()) {
            assertFalse(dc.isPresent());
            assertFalse(dc.isMetaData());
        }

        final long now = SystemTimeProvider.CLOCK.currentTimeNanos();
        channel.testMessage(now);
        try (DocumentContext dc = channel.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isMetaData());
        }
        assertEquals(now, channel.lastTestMessage());
    }

    @Test
    public void readme() {
        // start a server on an unused port
        String url = "tcp://:0";
        // create a context for new channels, all channels are closed when the context is closed
        try (ChronicleContext context = ChronicleContext.newContext(url)) {
            // open a new channel that acts as an EchoHandler
            ChronicleChannel channel = context.newChannelSupplier(new EchoHandler()).get();
            // create a proxy that turns each call to Says into an event on the channel
            Says say = channel.methodWriter(Says.class);
            // add an event
            say.say("Hello World");
            // ad a second event
            say.say("Bye now");

            // A buffer so the event name can be returned as well
            StringBuilder event = new StringBuilder();
            // read one message excepting the object after the event name to be a String
            String text = channel.readOne(event, String.class);
            // check it matches
            assertEquals("say: Hello World", event + ": " + text);

            // read the second message
            String text2 = channel.readOne(event, String.class);
            // check it matches
            assertEquals("say: Bye now", event + ": " + text2);
/*
            final long now = System.currentTimeMillis();
            channel.testMessage(now);

            Says reply = Mocker.logging(Says.class, "reply - ", System.out);
            final MethodReader methodReader = channel.methodReader(reply);
            int count = 0;
            while (channel.lastTestMessage() < now) {
                if (methodReader.readOne())
                    count++;
            }
            assertEquals(2, count);*/
        }
    }
}