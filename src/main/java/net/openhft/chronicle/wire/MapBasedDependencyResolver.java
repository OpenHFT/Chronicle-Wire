package net.openhft.chronicle.wire;

import java.util.HashMap;
import java.util.Map;

public class MapBasedDependencyResolver implements DependencyResolver {
    public Map<String, Object> objects = new HashMap<>();

    @Override
    public void register(String name, Object object) {
        objects.put(name, object);
    }

    @Override
    public Object resolve(String name) {
        return objects.get(name);
    }

    @Override
    public boolean containsDependencyKey(String name) {
        return objects.containsKey(name);
    }
}
