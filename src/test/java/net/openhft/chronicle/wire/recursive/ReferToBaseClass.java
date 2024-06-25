package net.openhft.chronicle.wire.recursive;

import java.util.ArrayList;
import java.util.List;

public class ReferToBaseClass extends Base {
    private List<Base> list = new ArrayList<>();

    public ReferToBaseClass() {
    }
}
