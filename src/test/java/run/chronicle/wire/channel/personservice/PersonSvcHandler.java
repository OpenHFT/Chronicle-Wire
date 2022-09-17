package run.chronicle.wire.channel.personservice;

import net.openhft.chronicle.wire.channel.AbstractHandler;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelCfg;
import net.openhft.chronicle.wire.channel.ChronicleContext;

public class PersonSvcHandler extends AbstractHandler<PersonSvcHandler> {

    private final PersonOpsHandler personOpsHandler;

    public PersonSvcHandler(PersonOpsHandler personOpsHandler) {
        this.personOpsHandler = personOpsHandler;
    }

    public void run(ChronicleContext context, ChronicleChannel channel) {
        channel.eventHandlerAsRunnable(
            personOpsHandler.responder(channel.methodWriter(ResponseSender.class))
        ).run();
    }

    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg) {
        throw new UnsupportedOperationException("Internal Channel not supported");
    }
}
