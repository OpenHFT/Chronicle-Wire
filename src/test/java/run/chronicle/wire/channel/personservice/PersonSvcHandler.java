package run.chronicle.wire.channel.personservice;

import net.openhft.chronicle.wire.channel.AbstractHandler;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelCfg;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import run.chronicle.wire.channel.personservice.api.PersonManagerOut;

public class PersonSvcHandler extends AbstractHandler<PersonSvcHandler> {

    public void run(ChronicleContext context, ChronicleChannel channel) {
        channel.eventHandlerAsRunnable(
            new PersonManager().out(channel.methodWriter(PersonManagerOut.class))
        ).run();
    }

    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg) {
        throw new UnsupportedOperationException("Internal Channel not supported");
    }


}
