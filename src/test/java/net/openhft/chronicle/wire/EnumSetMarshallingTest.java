/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Verifies EnumSet marshalling and unmarshalling across BinaryWire with expected text forms.
 */
public class EnumSetMarshallingTest extends WireTestCommon {

    // Serialized representation of a complete set of thread states
    private static final String FULL_SET_SERIALISED_FORM =
            "--- !!data #binary\n" +
                    "key: {\n" +
                    "  f: [\n" +
                    "    NEW,\n" +
                    "    RUNNABLE,\n" +
                    "    BLOCKED,\n" +
                    "    WAITING,\n" +
                    "    TIMED_WAITING,\n" +
                    "    TERMINATED\n" +
                    "  ]\n" +
                    "}\n";

    // Serialized representation of an empty set of thread states
    private static final String EMPTY_SET_SERIALISED_FORM =
            "--- !!data #binary\n" +
                    "key: {\n" +
                    "  f: [ ]\n" +
                    "}\n";

    /**
     * Covers marshalling an empty set of thread states via BinaryWire.
     */
    @Test
    @DisplayName("BinaryWire should serialise and read empty EnumSet values")
    public void shouldMarshallEmptySet() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip empty EnumSet marshalling");

        // Initialization of resources and test data
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final Foo written = new Foo(EnumSet.noneOf(Thread.State.class));
        final Foo read = new Foo(EnumSet.allOf(Thread.State.class));

        @NotNull Wire tw = new BinaryWire(bytes);
        tw.usePadding(true);
        tw.writeDocument(false, w ->
                w.write(() -> "key").marshallable(written));

        // Validate serialized form and read data back into object
        assertEquals(EMPTY_SET_SERIALISED_FORM, Wires.fromSizePrefixedBlobs(bytes),
                "empty EnumSet should serialise to expected binary text form");
        tw.readingDocument().wire().read("key").marshallable(read);

        // Ensure original and read data match
        assertEquals(written.f, read.f,
                "empty EnumSet should round-trip through binary wire");
        bytes.releaseLast();
    }

    /**
     * Covers marshalling a full set of thread states via BinaryWire.
     */
    @Test
    @DisplayName("BinaryWire should serialise and read full EnumSet")
    public void shouldMarshallFullSet() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip full EnumSet marshalling");

        // Initialization of resources and test data
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final Foo written = new Foo(EnumSet.allOf(Thread.State.class));
        final Foo read = new Foo(EnumSet.noneOf(Thread.State.class));

        @NotNull Wire tw = new BinaryWire(bytes);
        tw.usePadding(false);

        tw.writeDocument(false, w ->
                w.write(() -> "key").marshallable(written));

        // Validate serialized form and read data back into object
        assertEquals(FULL_SET_SERIALISED_FORM, Wires.fromSizePrefixedBlobs(bytes),
                "full EnumSet should serialise to expected binary text form with padding disabled");
        tw.readingDocument().wire().read("key").marshallable(read);

        // Ensure original and read data match
        assertEquals(written.f, read.f,
                "full EnumSet should round-trip through binary wire");
        bytes.releaseLast();
    }

    /**
     * Covers unmarshalling when the EnumSet target is null.
     */
    @Test
    @DisplayName("BinaryWire should allocate EnumSet when target is null")
    public void shouldUnmarshallToContainerWithNullValue() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip null EnumSet unmarshalling");

        // Initialization of resources and test data
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final Foo written = new Foo(EnumSet.allOf(Thread.State.class));
        final Foo read = new Foo(EnumSet.noneOf(Thread.State.class));
        // this forces the framework to allocate a new instance of EnumSet
        read.f = null;

        @NotNull Wire tw = new BinaryWire(bytes);
        tw.usePadding(false);
        tw.writeDocument(false, w ->
                w.write(() -> "key").marshallable(written));

        // Validate serialized form and read data back into object
        assertEquals(FULL_SET_SERIALISED_FORM, Wires.fromSizePrefixedBlobs(bytes),
                "full EnumSet should serialise to expected binary text form for null target");
        tw.readingDocument().wire().read("key").marshallable(read);

        // Ensure original and read data match
        assertEquals(written.f, read.f,
                "EnumSet should be allocated and match when target is null");
        bytes.releaseLast();
    }

    /**
     * Covers multiple EnumSet instances within an object graph.
     */
    @Test
    @DisplayName("BinaryWire should keep EnumSet instances distinct")
    public void shouldAllowMultipleInstancesInObjectGraph() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip multi instance EnumSet test");

        // Initialization of resources and test data
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final Container written = new Container();
        final Container read = new Container();

        @NotNull Wire tw = new BinaryWire(bytes);
        tw.usePadding(true);
        tw.writeDocument(false, w ->
                w.write(() -> "key").marshallable(written));

        // Read data back into object
        tw.readingDocument().wire().read("key").marshallable(read);

        // Ensure that the two EnumSets in the object graph are distinct
        assertNotSame(read.f1.get(0).f, read.f2.get(0).f,
                "EnumSet instances in object graph should not alias");
        bytes.releaseLast();
    }

    /**
     * Container class with two lists containing EnumSet instances.
     */
    private static final class Container extends SelfDescribingMarshallable {
        private final List<Foo> f1 = new ArrayList<>(Collections.singletonList(new Foo(EnumSet.allOf(Thread.State.class))));
        private final List<Foo> f2 = new ArrayList<>(Collections.singletonList(new Foo(EnumSet.noneOf(Thread.State.class))));
    }

    /**
     * Simple class encapsulating an EnumSet of thread states.
     */
    private static final class Foo extends SelfDescribingMarshallable {
        private EnumSet<Thread.State> f;

        private Foo(final EnumSet<Thread.State> membership) {
            f = membership;
        }
    }
}
