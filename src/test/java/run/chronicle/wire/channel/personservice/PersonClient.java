package run.chronicle.wire.channel.personservice;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import run.chronicle.wire.channel.channelArith.*;

public class PersonClient {

    private static final String URL = System.getProperty("url", "tcp://localhost:" + PersonSvcMain.PORT);

    public static void main(String[] args) {

        try (ChronicleContext context = ChronicleContext.newContext(URL)) {

            ChronicleChannel channel = context.newChannelSupplier(new PersonSvcHandler(new PersonOpsProcessor())).get();

            Jvm.startup().on(ArithClient.class, "Channel connected to: " + channel.channelCfg().hostname() + "[" + channel.channelCfg().port() + "]");

            final PersonOps personOps = channel.methodWriter(PersonOps.class);

            Person thePerson = new Person().name("George").timestampNS(SystemTimeProvider.CLOCK.currentTimeNanos());
            Jvm.startup().on(PersonClient.class, "adding " + thePerson.toString());
            personOps.addPerson(thePerson);

            StringBuilder evtType = new StringBuilder();
            ReqStatus response = channel.readOne(evtType, ReqStatus.class);

            Jvm.startup().on(PersonClient.class, " >>> " + evtType + ": " + response);
        }
    }
}
