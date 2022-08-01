package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.wire.channel.echo.DummyData;

interface Echoed {
    //    @MethodId(2)
    void echoed(DummyData data);
}
