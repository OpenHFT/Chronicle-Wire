package software.chronicle.demo.wire.channelDemo2;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelSupplier;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageMain {

    private static Logger LOGGER = LoggerFactory.getLogger(MessageMain.class);
    private static String url = "tcp://:5556";

    public static void main(String[] args) {
        try(ChronicleContext context = ChronicleContext.newContext(url)) {

            final ChronicleChannelSupplier channelFactory = context.newChannelSupplier(new BaseMessageHandler( new UCHandler()));
            ChronicleChannel channel = channelFactory.get();

            LOGGER.info("Ready for messages");
            Jvm.park();
        }
    }
}
