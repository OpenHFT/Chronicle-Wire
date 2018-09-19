package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.AbstractMarshallable;
import net.openhft.chronicle.wire.WireIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class ThreeSequence extends AbstractMarshallable {
    @NotNull
    transient List<Rung> aBuffer = new ArrayList<>();
    @NotNull
    transient List<Rung> bBuffer = new ArrayList<>();
    @NotNull
    transient List<Rung> cBuffer = new ArrayList<>();
    @NotNull
    List<Rung> a = new ArrayList<>();
    @NotNull
    List<Rung> b = new ArrayList<>();
    @NotNull
    List<Rung> c = new ArrayList<>();
    @Nullable
    String text;

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        wire.read("b").sequence(b, bBuffer, Rung::new);
        wire.read("a").sequence(a, aBuffer, Rung::new);
        wire.read("c").sequence(c, cBuffer, Rung::new);
        text = wire.read("text").text();
    }
}

