package net.openhft.chronicle.wire.channel;

import java.io.IOException;

public class EchoingMicroserviceMain {
    public static void main(String... args) throws IOException {
        ChronicleServiceMain.main("echoing.yaml");
    }
}
