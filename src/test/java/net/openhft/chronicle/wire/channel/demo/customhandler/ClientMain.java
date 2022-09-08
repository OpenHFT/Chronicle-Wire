package software.chronicle.demo.wire.channelDemo2;

import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelSupplier;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMain {

    private static Logger LOGGER = LoggerFactory.getLogger(ClientMain.class);
    private static String url = "tcp://localhost:5556";

    public static void main(String[] args) {

        try (ChronicleContext context = ChronicleContext.newContext(url)) {
            BaseMessageHandler messageHandler = new BaseMessageHandler(new UCHandler());

            final ChronicleChannelSupplier supplier = context.newChannelSupplier(messageHandler);
            messageHandler.buffered(true);
            ChronicleChannel channel = supplier.get();
            LOGGER.info("Channel set up to: {}:{}", channel.channelCfg().hostname(), channel.channelCfg().port());

            final MessageListener echoing = channel.methodWriter(MessageListener.class);
            echoing.message("Hello, world");
            StringBuilder eventType = new StringBuilder();
            String text = channel.readOne(eventType, String.class);

            LOGGER.info(">>>> " + text);
        }
    }

}
