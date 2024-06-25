package net.openhft.chronicle.wire.recursive;

import java.util.ArrayList;
import java.util.List;

public class ReferToSameClass extends Base {
    private List<ReferToSameClass> list = new ArrayList<>();

    public ReferToSameClass() {
    }
}
