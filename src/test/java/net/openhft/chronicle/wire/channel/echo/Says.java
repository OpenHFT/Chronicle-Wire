package net.openhft.chronicle.wire.channel.echo;

import net.openhft.chronicle.core.io.Syncable;

public interface Says extends Syncable {
    void say(String say);
}
