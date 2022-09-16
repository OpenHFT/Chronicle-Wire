package run.chronicle.wire.channel.personservice;

public interface PersonOpsHandler extends PersonOps {
    PersonOpsHandler theService(ResponseSender responder);
}
