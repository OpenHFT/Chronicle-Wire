package net.openhft.chronicle.wire.recursive;

import net.openhft.chronicle.wire.AbstractEventCfg;

public class Base extends AbstractEventCfg<Base> {
    private final String name;

    public  Base(String name) {
         this.name = name;
    }

    public String name() {
        return name;
    }
}
