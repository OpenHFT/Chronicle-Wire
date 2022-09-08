package software.chronicle.demo.wire.channelArith;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelSupplier;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.chronicle.demo.wire.channelDemo2.BaseMessageHandler;
import software.chronicle.demo.wire.channelDemo2.UCHandler;

public class ArithSvcMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArithSvcMain.class);
    private static String url = "tcp://:6667";

    public static void main(String[] args) {
        try(ChronicleContext context = ChronicleContext.newContext(url)) {

            final ChronicleChannelSupplier channelFactory = context.newChannelSupplier(new ArithHandler( new Calculator()));
            ChronicleChannel channel = channelFactory.get();

            LOGGER.info("Ready for messages");
            Jvm.park();
        }

    }
}
