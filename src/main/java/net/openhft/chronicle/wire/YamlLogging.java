package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Rob Austin
 */
public class YamlLogging {

    public static final boolean showServerWrites = false;
    public static volatile boolean clientWrites = false;
    public static volatile String title = "";
    @NotNull
    public static volatile String writeMessage = "";
    public static volatile boolean clientReads = false;
    public static volatile boolean showServerReads = false;

    @NotNull
    public static String readMessage = "";
}

