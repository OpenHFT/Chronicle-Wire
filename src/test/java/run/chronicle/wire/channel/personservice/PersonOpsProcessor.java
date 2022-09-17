package run.chronicle.wire.channel.personservice;

public class PersonOpsProcessor implements PersonOpsHandler {

    private transient ResponseSender responder;

    public PersonOpsProcessor responder(ResponseSender responseSender) {
        this.responder = responseSender;
        return this;
    }

    @Override
    public void addPerson(Person p) {
        responder.respond(ReqStatus.OK);
    }
}

