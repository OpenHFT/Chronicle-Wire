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

import static org.junit.Assume.assumeFalse;

interface NoOut {
    Closeable out();
}

public class ChronicleServiceMainTest extends WireTestCommon {

    @Before
    public void precompile() {
        // as shutdown happens quickly, recompile the NoOut
        Wire.newYamlWireOnHeap()
                .methodWriter(NoOut.class)
                .out()
                .close();
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

        final ChronicleChannelCfg channelCfg = new ChronicleChannelCfg().hostname("localhost").port(65432).initiator(true).buffered(true);
        ChronicleChannel client = ChronicleChannel.newChannel(null, channelCfg, new OkHeader());
        client.close();
        main.close();
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
