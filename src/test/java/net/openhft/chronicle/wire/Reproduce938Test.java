package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

public class Reproduce938Test {
    @Test
    public void reproduce() {
        ClassAliasPool.CLASS_ALIASES.addAlias(
            Impl1.class,
            Impl2.class,
            GenericContainer.class
        );

        ContainerWithWorkaround<Impl1> containerWithWorkaround = new ContainerWithWorkaround<>();
        containerWithWorkaround.collection.add(new Impl1());
        String containerWithWorkaroundAsYaml = WireType.YAML_ONLY.asString(containerWithWorkaround);
        WireType.YAML_ONLY.fromString(containerWithWorkaroundAsYaml);

        GenericContainer<Impl1> genericContainer = new GenericContainer<>();
        genericContainer.collection.add(new Impl1());
        String genericContainerAsYaml = WireType.YAML_ONLY.asString(genericContainer);
        WireType.YAML_ONLY.fromString(genericContainerAsYaml);
    }

    static abstract class AbstractGeneric<T extends AbstractGeneric<T>> {
        private long value;

        public long value() {
            return value;
        }

        public T value(long i) {
            this.value = i;
            return (T) this;
        }
    }

    static class Impl1 extends AbstractGeneric<Impl1> {

    }

    static class Impl2 extends AbstractGeneric<Impl2> {

    }

    static class GenericContainer<T extends AbstractGeneric<T>> extends SelfDescribingMarshallable{
        private Collection<T> collection = new ArrayList<>();
    }

    static class ContainerWithWorkaround<T extends AbstractGeneric<T>> extends SelfDescribingMarshallable{
        private Collection collection = new ArrayList<>();
    }
}
