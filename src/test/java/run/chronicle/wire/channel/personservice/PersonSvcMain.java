package run.chronicle.wire.channel.personservice;

import net.openhft.chronicle.wire.channel.ChronicleGatewayMain;

import java.io.IOException;

public class PersonSvcMain {

    static final int PORT = Integer.getInteger("port", 7771);

    public static void main(String... args) throws IOException {
        System.setProperty("port", "" + PORT);
        ChronicleGatewayMain.main(args);
    }
}
