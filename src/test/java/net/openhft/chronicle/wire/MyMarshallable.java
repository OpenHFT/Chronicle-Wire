package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by peter on 11/05/16.
 */
class MyMarshallable extends AbstractMarshallable {

    @Nullable
    String someData;

    MyMarshallable(String someData) {
        this.someData = someData;
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "MyField").text(someData);
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        someData = wire.read(() -> "MyField").text();
    }
}
