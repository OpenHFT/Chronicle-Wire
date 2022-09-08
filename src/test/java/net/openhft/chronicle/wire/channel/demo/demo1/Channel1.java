package net.openhft.chronicle.wire.channel.demo.simple;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import net.openhft.chronicle.wire.channel.echo.EchoHandler;
import net.openhft.chronicle.wire.channel.echo.Says;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Channel1 {

    private static String url = "tcp://:4441";

    private static Logger LOGGER = LoggerFactory.getLogger(Channel1.class);

    public static void main(String[] args) {

        try (ChronicleContext context = ChronicleContext.newContext(url).name("Channels1")) {
            ChronicleChannel channel = context.newChannelSupplier(new EchoHandler().buffered(false))
                .get();

            LOGGER.info("Channel set up on port: {}", channel.channelCfg().port());
            Says says = channel.methodWriter(Says.class);
            says.say("Well hello there");

            StringBuilder eventType = new StringBuilder();
            String text = channel.readOne(eventType, String.class);

            LOGGER.info(">>>> " + text);
        }

        Jvm.pause(2000);
    }
}
