package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.internal.URLMarshallableOut;

import java.net.URL;
import java.util.function.Supplier;

public class MarshallableOutBuilder implements Supplier<MarshallableOut> {
    private final URL url;

    public MarshallableOutBuilder(URL url) {
        this.url = url;
    }

    @Override
    public MarshallableOut get() {
        switch (url.getProtocol()) {
            case "tcp":
                throw new UnsupportedOperationException("Direct TCP connection not implemented");
            case "file":
                return new URLMarshallableOut(this, WireType.YAML);
            case "http":
            case "https":
                return new URLMarshallableOut(this, WireType.JSON);
            default:
                throw new UnsupportedOperationException("Writing to " + url.getProtocol() + " is  not implemented");
        }
    }

    public URL url() {
        return url;
    }
}
