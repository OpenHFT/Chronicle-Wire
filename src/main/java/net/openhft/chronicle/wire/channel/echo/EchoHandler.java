package net.openhft.chronicle.wire.channel.echo;

import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.core.io.ClosedIORuntimeException;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.channel.AbstractHandler;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelCfg;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import net.openhft.chronicle.wire.channel.echo.internal.EchoChannel;

public class EchoHandler extends AbstractHandler<EchoHandler> {

    @Override
    public void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException {
        try (AffinityLock lock = context.affinityLock()) {
            Pauser pauser = Pauser.balanced();
            while (!channel.isClosed()) {
                try (DocumentContext dc = channel.readingDocument()) {
                    if (!dc.isPresent()) {
                        pauser.pause();
                        continue;
                    }
                    try (DocumentContext dc2 = channel.writingDocument(dc.isMetaData())) {
                        dc.wire().copyTo(dc2.wire());
                    }
                    pauser.reset();
                }
            }
        }
    }

    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg) {
        return new EchoChannel(channelCfg);
    }
}
