package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

// This test class is for validating JSON sequences.
public class JSONEmptySequencesTest extends net.openhft.chronicle.wire.WireTestCommon {
    // Test for verifying the handling of empty and non-empty JSON sequences.
    @Test
    public void emptySequence() {
        // Add an alias for the Foo class to simplify the YAML representation.
        ClassAliasPool.CLASS_ALIASES.addAlias(Foo.class);

        final Bytes<byte[]> data = Bytes.allocateElasticOnHeap();
        data.append("!Foo {\n" +
                "  field1: 1234,\n" +
                "  field2: 456,\n" +
                "  field3: [ ],\n" +
                "  field4: [\n" +
                "    abc,\n" +
                "    xyz\n" +
                "  ]\n" +
                "}");

        // Create a JSONWire object using the populated Bytes data.
        final JSONWire wire = new JSONWire(data);
        // Create a new Foo instance.
        final Foo f = new Foo();
        // Populate the Foo instance using the data from the wire.
        wire.getValueIn().object(f, Foo.class);

        // Assert to check if the populated Foo instance matches the expected string representation.
        Assert.assertEquals("!Foo {\n" +
                "  field1: 1234,\n" +
                "  field2: 456,\n" +
                "  field3: [ ],\n" +
                "  field4: [\n" +
                "    abc,\n" +
                "    xyz\n" +
                "  ]\n" +
                "}\n", f.toString());
    }

    // A private static class Foo with four fields, where two of them are lists.
    private static final class Foo extends SelfDescribingMarshallable {
        int field1;  // A field of type int.
        int field2;  // Another field of type int.
        // A list of strings that is initialized as an empty ArrayList.
        final List<String> field3 = new ArrayList<>();
        // Another list of strings that is initialized as an empty ArrayList.
        final List<String> field4 = new ArrayList<>();
    }
}
