package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class UnknownClassBase extends SelfDescribingMarshallable {
    private Map<Object, Object> map = new LinkedHashMap<>();

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException, InvalidMarshallableException {
        map = wire.readAllAsMap(Object.class, Object.class, new LinkedHashMap<>());
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) throws InvalidMarshallableException {
        wire.writeAllAsMap(Object.class, Object.class, map);
    }
}