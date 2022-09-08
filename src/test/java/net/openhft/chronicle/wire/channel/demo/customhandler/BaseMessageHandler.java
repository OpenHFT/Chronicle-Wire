package software.chronicle.demo.wire.channelDemo2;

import net.openhft.chronicle.wire.channel.AbstractHandler;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelCfg;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseMessageHandler extends AbstractHandler<BaseMessageHandler> {

    private static Logger LOGGER = LoggerFactory.getLogger(BaseMessageHandler.class);

    private final MessageHandler theHandler;

    public BaseMessageHandler(MessageHandler nestedHandler) {
        this.theHandler = nestedHandler;
    }

    @Override
    public void run (ChronicleContext context, ChronicleChannel channel) {
        LOGGER.info("BaseMessageHandler::run");

        theHandler.msg(channel.methodWriter(MessageListener.class));
        channel.eventHandlerAsRunnable(theHandler).run();
    }

    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg) {
        throw new UnsupportedOperationException();
    }
}
