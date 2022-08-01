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
