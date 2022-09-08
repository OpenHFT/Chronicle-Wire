package software.chronicle.demo.wire.channelDemo2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UCHandler implements MessageHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(UCHandler.class);

    private MessageListener msgListener;

    public UCHandler msg( MessageListener msgListener ) {
        this.msgListener = msgListener;
        return this;
    }

    public void message(String msg) {
        LOGGER.info("Processing message {}", msg);
        msgListener.message(msg.toUpperCase());}
}
