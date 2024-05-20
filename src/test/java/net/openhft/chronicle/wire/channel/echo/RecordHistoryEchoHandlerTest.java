/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.channel.echo;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.wire.*;
import net.openhft.chronicle.wire.channel.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class RecordHistoryEchoHandlerTest extends WireTestCommon {

    @SuppressWarnings("deprecation")
    private static void doTest(ChronicleContext context, ChannelHandler handler) {
        ChronicleChannel channel = context.newChannelSupplier(handler).connectionTimeoutSecs(1).get();
        assertTrue(channel.recordHistory());
        VanillaMessageHistory history = (VanillaMessageHistory) MessageHistory.get();
        history.reset(1, 128);

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
        history.historyWallClock(true);
        String expected = ", timings: [ 20";
        String expected2 = ", timings: [ 19";
        String string = history.toString();
        if (!string.contains(expected) && !string.contains(expected2))
            fail("Expected " + string + " to contain'" + expected + "' or '" + expected2 + "'");
    }

    @Before
    public void useWallClockInHistory() {
        System.setProperty("history.wall.clock", "true");
    }

    @After
    public void clearWallClockInHistory() {
        System.clearProperty("history.wall.clock");
    }

    @Test
    @Ignore(/* TODO FIX */)
    public void internal() {
        String url = "internal://";
        try (ChronicleContext context = ChronicleContext.newContext(url)) {
            doTest(context, new RecordHistoryEchoHandler().buffered(false));
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
            doTest(context, new RecordHistoryEchoHandler().buffered(false));
        }
    }

    @Test
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
            doTest(context, new RecordHistoryEchoHandler().buffered(true));
        }
    }

    @Test
    @Ignore(/* TODO FIX */)
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
                protected ChannelHeader replaceOutHeader(ChannelHeader channelHeader) {
                    return new RedirectHeader(Arrays.asList(urlZzz, url0));
                }
            }) {
                gateway1.name("target/one");
                // gateway that will handle the redirect request
                gateway1.start();

                try (ChronicleContext context = ChronicleContext.newContext(url1).name("target/client")) {
                    doTest(context, new RecordHistoryEchoHandler().buffered(false));
                }
            }
        }
    }
}
