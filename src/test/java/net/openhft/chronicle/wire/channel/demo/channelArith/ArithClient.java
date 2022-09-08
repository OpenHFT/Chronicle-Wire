package software.chronicle.demo.wire.channelArith;

import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelSupplier;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.chronicle.demo.wire.channelDemo2.ClientMain;

public class ArithClient {
    private static Logger LOGGER = LoggerFactory.getLogger(ClientMain.class);
    private static String url = "tcp://localhost:6667";

    public static void main(String[] args) {

        try (ChronicleContext context = ChronicleContext.newContext(url)) {
            ArithHandler arithHandler = new ArithHandler(new Calculator());

            final ChronicleChannelSupplier supplier = context.newChannelSupplier(arithHandler);
            arithHandler.buffered(true);
            ChronicleChannel channel = supplier.get();
            LOGGER.info("Channel set up to: {}:{}", channel.channelCfg().hostname(), channel.channelCfg().port());

            final ArithListener outgoing = channel.methodWriter(ArithListener.class);
            outgoing.calculate(new ArithExpr(3, Op.TIMES, 4));
            StringBuilder evtType = new StringBuilder();
            ArithExpr response = channel.readOne(evtType, ArithExpr.class);
            LOGGER.info(" >>> {}", response.toString());
        }
    }

}
