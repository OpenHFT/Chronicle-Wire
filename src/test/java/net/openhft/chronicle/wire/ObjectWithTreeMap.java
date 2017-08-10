package net.openhft.chronicle.wire;

import java.util.TreeMap;

/*
 * Created by Peter Lawrey on 10/01/2017.
 */
class ObjectWithTreeMap extends AbstractMarshallable {
    final TreeMap<String, String> map = new TreeMap<>();
}
