package net.openhft.chronicle.wire;

import java.util.TreeMap;

class ObjectWithTreeMap extends SelfDescribingMarshallable {
    TreeMap<String, String> map = new TreeMap<>();
}
