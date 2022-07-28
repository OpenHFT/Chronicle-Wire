package net.openhft.chronicle.wire.channel.impl.internal;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {
    static {
        addMyPackage();
    }

    @SuppressWarnings("EmptyMethod")
    public static void init() {
        // call static initialiser
    }

    private static void addMyPackage() {
        // Ensure that we are registered as an url protocol handler for JavaFxCss:/path css files.
        String was = System.getProperty("java.protocol.handler.pkgs", "");
        String pkg = Handler.class.getPackage().getName();
        int ind = pkg.lastIndexOf('.');
        assert ind != -1 : "You can't add url handlers in the base package";

        System.setProperty("java.protocol.handler.pkgs",
                pkg.substring(0, ind) + (was.isEmpty() ? "" : "|" + was));
    }

    @Override
    protected URLConnection openConnection(URL u) {
        throw new UnsupportedOperationException();
    }

}
