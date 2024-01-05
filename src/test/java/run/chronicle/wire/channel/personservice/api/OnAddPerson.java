package run.chronicle.wire.channel.personservice.api;

public class OnAddPerson extends AddPerson {

    private boolean success;

    private String reason;

    public boolean success() { return success; }

    public OnAddPerson success( boolean success ) {
        this.success = success;
        return this;
    }

    public String reason() { return reason; }

    public OnAddPerson reason( String reason ) {
        this.reason = reason;
        return this;
    }

}
