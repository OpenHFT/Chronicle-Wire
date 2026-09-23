package net.openhft.chronicle.wire;

public interface DependencyResolver {
    void register(String name, Object object);
    Object resolve(String name);
    boolean containsDependencyKey(String name);
}
