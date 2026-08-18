/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class WireContextListenerLifecycleTest extends WireTestCommon {

    @Test
    public void recordingLifecycleReceivesWritingDocumentEntryPoint() {
        AbstractWire wire = newWire();
        RecordingWireContextListenerLifecycle lifecycle = new RecordingWireContextListenerLifecycle();
        wire.contextListenerLifecycle(lifecycle);

        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire().write("meta").text("header");
        }
        try (DocumentContext dc = wire.acquireWritingDocument(false)) {
            dc.wire().write("data").text("event");
        }

        assertEquals(2, lifecycle.documentCalls);
        assertEquals(java.util.Arrays.asList(true, false), lifecycle.metadata);
        assertTrue(lifecycle.started());
    }

    @Test
    public void recordingLifecycleReceivesDirectWriteEntryPoints() {
        AbstractWire completeWire = newWire();
        RecordingWireContextListenerLifecycle completeLifecycle = new RecordingWireContextListenerLifecycle();
        completeWire.contextListenerLifecycle(completeLifecycle);
        completeWire.writeDocument(false, wire -> wire.write("event").text("complete"));

        AbstractWire incompleteWire = newWire();
        RecordingWireContextListenerLifecycle incompleteLifecycle = new RecordingWireContextListenerLifecycle();
        incompleteWire.contextListenerLifecycle(incompleteLifecycle);
        incompleteWire.writeNotCompleteDocument(false, wire -> wire.write("event").text("incomplete"));

        assertEquals(1, completeLifecycle.documentCalls);
        assertEquals(1, incompleteLifecycle.documentCalls);
    }

    @Test
    public void startedTestLifecycleFreezesListenerConfiguration() {
        AbstractWire wire = newWire();
        RecordingWireContextListenerLifecycle lifecycle = new RecordingWireContextListenerLifecycle();
        wire.contextListenerLifecycle(lifecycle);
        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("event").text("start");
        }

        assertThrows(IllegalStateException.class,
                () -> wire.contextListener(Runnable.class, Runnable::run));
    }

    private static AbstractWire newWire() {
        return (AbstractWire) WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
    }

    /** Test fake proving lifecycle entry points can be exercised without a real context listener. */
    private static final class RecordingWireContextListenerLifecycle implements WireContextListenerLifecycle {
        private final List<Boolean> metadata = new ArrayList<>();
        private int documentCalls;
        private boolean started;

        @Override
        public boolean started() {
            return started;
        }

        @Override
        public void beforeDocument(AbstractWire wire, boolean metaData) {
            started = true;
            documentCalls++;
            metadata.add(metaData);
        }
    }
}
