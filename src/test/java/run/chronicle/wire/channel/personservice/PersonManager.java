package run.chronicle.wire.channel.personservice;

import run.chronicle.wire.channel.personservice.api.AddPerson;
import run.chronicle.wire.channel.personservice.api.OnAddPerson;
import run.chronicle.wire.channel.personservice.api.PersonManagerIn;
import run.chronicle.wire.channel.personservice.api.PersonManagerOut;

public class PersonManager implements PersonManagerIn {

    private PersonManagerOut out;

    private OnAddPerson onAddPerson = new OnAddPerson();

    public PersonManagerOut out() { return out; };
    public PersonManager out ( PersonManagerOut out ) {
        this.out = out;
        return this;
    }

    @Override
    public void addPerson(AddPerson addPerson) {
        onAddPerson.reset();
        addPerson.copyTo(onAddPerson);
        out.OnAddPerson(onAddPerson.success(true));
    }
}

