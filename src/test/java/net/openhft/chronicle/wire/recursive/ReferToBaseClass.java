package net.openhft.chronicle.wire.recursive;

import java.util.ArrayList;
import java.util.List;

public class ReferToBaseClass extends ReferToSameClass {
    private final List<ReferToSameClass> list = new ArrayList<>();

    public ReferToBaseClass(String name) {
        super(name);
    }

    public List<ReferToSameClass> list() {
        return list;
    }
}
