package net.openhft.chronicle.wire;

import java.util.TreeMap;


class ObjectWithTreeMap extends SelfDescribingMarshallable {
    final TreeMap<String, String> map = new TreeMap<>();
}
