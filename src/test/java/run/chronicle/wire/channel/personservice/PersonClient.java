package run.chronicle.wire.channel.personservice;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import run.chronicle.wire.channel.channelArith.*;
import run.chronicle.wire.channel.personservice.api.AddPerson;
import run.chronicle.wire.channel.personservice.api.OnAddPerson;
import run.chronicle.wire.channel.personservice.api.PersonManagerIn;

public class PersonClient {

    private static final String URL = System.getProperty("url", "tcp://localhost:" + PersonSvcMain.PORT);

    public static void main(String[] args) {

        try (ChronicleContext context = ChronicleContext.newContext(URL)) {

            ChronicleChannel channel = context.newChannelSupplier(new PersonSvcHandler()).get();

            Jvm.startup().on(ArithClient.class, "Channel connected to: " + channel.channelCfg().hostname() + "[" + channel.channelCfg().port() + "]");

            final PersonManagerIn personOps = channel.methodWriter(PersonManagerIn.class);

            AddPerson addPerson = new AddPerson().name("George").time(SystemTimeProvider.CLOCK.currentTimeNanos());
            Jvm.startup().on(PersonClient.class, "adding " + addPerson.toString());
            personOps.addPerson(addPerson);

            StringBuilder evtType = new StringBuilder();
            OnAddPerson response = channel.readOne(evtType, OnAddPerson.class);

            Jvm.startup().on(PersonClient.class, " >>> " + evtType + ": " + response);
        }
    }
}
