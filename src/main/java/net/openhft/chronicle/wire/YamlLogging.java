package net.openhft.chronicle.wire;

/**
 * Created by Rob Austin
 */
public class YamlLogging {

    public static volatile boolean showServerWrites = false;
    public static volatile boolean clientWrites = false;
    public static volatile String title = "";
    public static volatile String writeMessage = "";
    public static volatile boolean clientReads = false;
    public static volatile boolean showServerReads = false;
    public static String readMessage = "";
}

