package net.openhft.chronicle.wire.channel.impl.tcp;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL u) {
        throw new UnsupportedOperationException();
    }
}
