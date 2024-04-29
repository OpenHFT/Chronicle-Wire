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

package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

interface NoOut {
    Closeable out();
}

@SuppressWarnings("deprecation")
public class ChronicleServiceMainTest extends WireTestCommon {

    @Before
    public void precompile() {
        // as shutdown happens quickly, recompile the NoOut
        Wire.newYamlWireOnHeap()
                .methodWriter(NoOut.class)
                .out()
                .close();
    }

    @Override
    @Before
    public void threadDump() {
        super.threadDump();
    }

    @Override
    public void checkThreadDump() {
        if (threadDump != null)
            threadDump.assertNoNewThreads(5, TimeUnit.SECONDS);
    }

    @Test
    public void handshake() {
        // TODO FIX
        assumeFalse(Jvm.isJava9Plus());
        String cfg = "" +
                "port: 65432\n" +
                "microservice: !" + ClosingMicroservice.class.getName() + " { }";
        ChronicleServiceMain main = Marshallable.fromString(ChronicleServiceMain.class, cfg);
        Thread t = new Thread(main::run);
        t.setDaemon(true);
        t.start();

        final ChronicleChannelCfg<?> channelCfg = new ChronicleChannelCfg<>().addHostnamePort("localhost", 65432).initiator(true).buffered(true);
        try (ChronicleChannel client = ChronicleChannel.newChannel(null, channelCfg, new OkHeader())) {
            assertEquals("" +
                            "!net.openhft.chronicle.wire.channel.OkHeader {\n" +
                            "  systemContext: {\n" +
                            "    availableProcessors: PP,\n" +
                            "    hostId: 0,\n" +
                            "    hostName: HHH,\n" +
                            "    upTime: 20UU,\n" +
                            "    userCountry: UC,\n" +
                            "    userName: UN,\n" +
                            "    javaVendor: JV,\n" +
                            "    javaVersion: JV\n" +
                            "  },\n" +
                            "  sessionName: !!null \"\"\n" +
                            "}\n",
                    client.headerIn().toString()
                            .replaceAll("availableProcessors: .*?,", "availableProcessors: PP,")
                            .replaceAll("hostName: .*?,", "hostName: HHH,")
                            .replaceAll("upTime: 20.*?,", "upTime: 20UU,")
                            .replaceAll("userCountry: .*?,", "userCountry: UC,")
                            .replaceAll("userName: .*?,", "userName: UN,")
                            .replaceAll("javaVendor: \"[^\"]+\",", "javaVendor: JV,")
                            .replaceAll("javaVendor: .*?,", "javaVendor: JV,")
                            .replaceAll("javaVersion: .*", "javaVersion: JV")
            );
        } finally {
            main.close();
        }
    }
}

class ClosingMicroservice extends SelfDescribingMarshallable implements Closeable {
    NoOut out;

    @Override
    public void close() {
    }

    @Override
    public boolean isClosed() {
        return true;
    }
}
