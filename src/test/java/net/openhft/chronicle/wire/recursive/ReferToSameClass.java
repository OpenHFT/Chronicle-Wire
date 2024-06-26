package net.openhft.chronicle.wire.recursive;

import java.util.ArrayList;
import java.util.List;

public class ReferToSameClass extends Base {
    private final List<ReferToSameClass> list = new ArrayList<>();

    public ReferToSameClass(String name) {
        super(name);
    }

    public List<ReferToSameClass> list() {
        return list;
    }
}
