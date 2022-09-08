package software.chronicle.demo.wire.channelArith;

import net.openhft.chronicle.wire.channel.AbstractHandler;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelCfg;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.chronicle.demo.wire.channelDemo2.MessageListener;

public class ArithHandler extends AbstractHandler<ArithHandler> {

    private static Logger LOGGER = LoggerFactory.getLogger(ArithHandler.class);

    private CalcHandler calcHandler;

    public ArithHandler ( CalcHandler calcHandler ) {
        this.calcHandler = calcHandler;
    }

    public void run (ChronicleContext context, ChronicleChannel channel) {
        LOGGER.info("BaseMessageHandler::run");

        calcHandler.calculator(channel.methodWriter(ArithListener.class));
        channel.eventHandlerAsRunnable(calcHandler).run();
    }

    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg) {
        throw new UnsupportedOperationException();
    }

}
