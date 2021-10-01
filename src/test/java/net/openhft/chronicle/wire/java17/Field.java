package net.openhft.chronicle.wire.java17;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.util.HashMap;
import java.util.Map;

public class Field extends SelfDescribingMarshallable {

    private final Map<String, Required> required = new HashMap<>();

    public void required(String name, Required required) {
        this.required.put(name, required);
    }
}
