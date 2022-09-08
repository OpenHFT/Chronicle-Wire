package software.chronicle.demo.wire.channelDemo2;

import net.openhft.chronicle.wire.Marshallable;

public interface MessageHandler extends MessageListener, Marshallable {
    UCHandler msg( MessageListener messageListener);
}
