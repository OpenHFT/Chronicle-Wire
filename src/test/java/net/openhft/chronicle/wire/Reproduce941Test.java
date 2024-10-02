package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertNull;

public class Reproduce941Test {
    @Test
    public void reproduce() {
        ClassAliasPool.CLASS_ALIASES.addAlias(CollectionContainer.class);

        CollectionContainer container = WireType.JSON_ONLY.fromString("{ \"@CollectionContainer\": { \"collection\": null } }");
        assertNull(container.collection);
    }

    static class CollectionContainer {
        private Collection<String> collection;
    }
}
