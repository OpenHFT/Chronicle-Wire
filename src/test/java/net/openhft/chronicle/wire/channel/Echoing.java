package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.wire.channel.echo.DummyData;

interface Echoing {
    //    @MethodId(1)
    void echo(DummyData data);
}
