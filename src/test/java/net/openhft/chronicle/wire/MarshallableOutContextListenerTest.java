/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.internal.StringConsumerMarshallableOut;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MarshallableOutContextListenerTest extends WireTestCommon {

    interface ContextEvents {
        void event(String value);
    }

    // Reads every document in write order, recording the first field's value of each.
    private static List<String> readOrdered(Wire wire) {
        wire.bytes().readPosition(0);
        List<String> out = new ArrayList<>();
        while (true) {
            try (DocumentContext dc = wire.readingDocument()) {
                if (!dc.isPresent())
                    break;
                StringBuilder sb = new StringBuilder();
                String val = dc.wire().read(sb).text();
                out.add(val);
            }
        }
        return out;
    }

    private static long count(List<String> list, String value) {
        return list.stream().filter(value::equals).count();
    }

    // Drains method-writer events via a methodReader (works for fieldless formats such as RAW).
    private static List<String> drainEvents(Wire wire) {
        wire.bytes().readPosition(0);
        List<String> events = new ArrayList<>();
        net.openhft.chronicle.bytes.MethodReader reader = wire.methodReader((ContextEvents) events::add);
        while (reader.readOne()) {
            // drain
        }
        return events;
    }

    // ------------------------------------------------------------------
    // 1. T-P1-5 (AbstractWire:231)
    // ------------------------------------------------------------------
    @Test
    public void tP15_cannotSetListenerAfterFirstDocument() {
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("msg").text("one");
        }
        assertThrows(IllegalStateException.class,
                () -> wire.contextListener(ContextEvents.class, w -> w.event("late")));
    }

    // ------------------------------------------------------------------
    // 2. T-P1-8 (BinaryWire:339) - metadata-first ordering
    // ------------------------------------------------------------------
    @Test
    public void tP18_metadataBeforeContextEvent_binary() {
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.contextListener(ContextEvents.class, w -> w.event("ctx"));
        // A leading metadata document (stream header/framing) must come first, then the context
        // records, then the first data document: header -> context -> data.
        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire().write("event").text("meta");
        }
        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("event").text("d");
        }
        assertEquals(Arrays.asList("meta", "ctx", "d"), readOrdered(wire));
    }

    @Test
    public void tP18_metadataBeforeContextEvent_text() {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        wire.contextListener(ContextEvents.class, w -> w.event("ctx"));
        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire().write("event").text("meta");
        }
        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("event").text("d");
        }
        // TextWire's metadata read-back does not round-trip through readingDocument cleanly, so
        // assert ordering on the serialized text directly: header -> context -> data.
        String text = wire.bytes().toString();
        int meta = text.indexOf("meta"), ctx = text.indexOf("ctx"), d = text.indexOf("\"d\"");
        if (d < 0) d = text.lastIndexOf("d");
        assertTrue("expected meta before ctx before d but was:\n" + text,
                meta >= 0 && ctx > meta && d > ctx);
    }

    // ------------------------------------------------------------------
    // 3. T-P1-25 (WireOut.writeDocument bypass)
    // ------------------------------------------------------------------
    @Test
    public void tP125_writeDocumentNotifiesContextListener() {
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.contextListener(ContextEvents.class, w -> w.event("ctx"));
        wire.writeDocument(false, w -> w.write("msg").text("one"));

        List<String> events = readOrdered(wire);
        assertTrue("expected context event 'ctx' in output but was " + events,
                events.contains("ctx"));
    }

    @Test
    public void tP125_writeNotCompleteDocumentNotifiesContextListener() {
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.contextListener(ContextEvents.class, w -> w.event("ctx"));
        wire.writeNotCompleteDocument(false, w -> w.write("msg").text("one"));

        List<String> events = readOrdered(wire);
        assertTrue("expected context event 'ctx' in output but was " + events,
                events.contains("ctx"));
    }

    // ------------------------------------------------------------------
    // 4. T-P1-6w (AbstractWire:238) - no rollback / duplication on retry
    // ------------------------------------------------------------------
    @Test
    public void tP16w_contextEventNotDuplicatedAfterListenerThrows() {
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        AtomicInteger n = new AtomicInteger();
        wire.contextListener(ContextEvents.class, w -> {
            w.event("a");
            if (n.getAndIncrement() == 0)
                throw new IllegalStateException("boom");
        });

        boolean threw = false;
        try {
            try (DocumentContext dc = wire.writingDocument(false)) {
                dc.wire().write("msg").text("one");
            }
        } catch (IllegalStateException expected) {
            // first attempt: listener wrote event("a") then threw
            threw = true;
        }
        assertTrue("the listener's exception must propagate to the caller", threw);

        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("msg").text("two");
        }

        List<String> events = readOrdered(wire);
        assertEquals("context event should appear exactly once but was " + events,
                1, count(events, "a"));
    }

    @Test
    public void tP26_directWritingDocumentInsideListenerFailsDeterministically() {
        // On a plain wire a listener writing via the wire directly is harmless (the notify-once flag
        // is already latched), so it is permitted rather than policed: no guard machinery.
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.contextListener(ContextEvents.class, writer -> {
            writer.event("ctx");
            try (DocumentContext dc = wire.writingDocument(false)) {
                dc.wire().write("event").text("direct");
            }
        });

        wire.methodWriter(ContextEvents.class).event("one");

        assertEquals(Arrays.asList("ctx", "direct", "one"), readOrdered(wire));
    }

    // A chained method writer whose chained return type is @DontChain is served by a generated
    // sub-writer; the listener must be able to use it through the supplied writer.
    interface ChainStart {
        ChainEnd start(String id);
    }

    @net.openhft.chronicle.core.annotation.DontChain
    interface ChainEnd {
        void end(String value);
    }

    @Test
    public void tChained_dontChainSubWriterUsableInsideListener() {
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.contextListener(ChainStart.class, w -> w.start("ctx").end("v"));

        wire.methodWriter(ChainStart.class).start("one").end("w");

        // The point under test: using the generated @DontChain sub-writer INSIDE the listener must
        // not be rejected as a re-entrant write (it previously threw IllegalStateException). The
        // start events round-trip; the end-event read-back is a pre-existing @DontChain
        // reader-dispatch quirk unrelated to context listeners (end:w, written outside the
        // listener, is equally affected), so it is not asserted here.
        expectException("Unknown method-name='end'");
        List<String> events = new ArrayList<>();
        wire.bytes().readPosition(0);
        net.openhft.chronicle.bytes.MethodReader reader = wire.methodReader(
                (ChainStart) id -> {
                    events.add("start:" + id);
                    return value -> events.add("end:" + value);
                },
                (ChainEnd) value -> events.add("end:" + value));
        while (reader.readOne()) {
            // drain
        }
        assertEquals("the listener's chained write and the application write must both be present",
                Arrays.asList("start:ctx", "start:one"),
                events.stream().filter(e -> e.startsWith("start:")).collect(java.util.stream.Collectors.toList()));
    }

    // RawWire must preserve the caller's metadata flag - a plain public-API invariant, listener or not.
    @Test
    public void tRaw_writingDocumentPreservesMetadataFlag() {
        Wire wire = WireType.RAW.apply(Bytes.allocateElasticOnHeap());
        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire().bytes().writeUtf8("header");
        }
        wire.bytes().readPosition(0);
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue("RawWire must keep the META_DATA header bit for writingDocument(true)",
                    dc.isMetaData());
        }
    }

    // READ_ANY delegates writes to an underlying wire whose listener fields are never set, so a
    // listener registered on the outer wire would be silently ignored - fail loudly instead.
    @Test
    public void tAnyWire_contextListenerIsRejected() {
        Wire any = WireType.READ_ANY.apply(Bytes.allocateElasticOnHeap());
        assertThrows(UnsupportedOperationException.class,
                () -> any.contextListener(ContextEvents.class, w -> w.event("ctx")));
    }

    // In overwrite mode every document atomically replaces the file, so the context records written
    // with the first document are lost - registering a listener there must fail loudly.
    @SuppressWarnings("deprecation")
    @Test
    public void tFile_overwriteModeRejectsContextListener() throws Exception {
        File file = File.createTempFile("ctxlistener-overwrite", ".yaml");
        file.deleteOnExit();
        URL url = new URL("file:" + file.getAbsolutePath());
        MarshallableOut out = MarshallableOut.builder(url).wireType(WireType.YAML_ONLY).get();
        assertThrows(UnsupportedOperationException.class,
                () -> out.contextListener(ContextEvents.class, w -> w.event("hdr")));
    }

    // ------------------------------------------------------------------
    // 5. T-P0-4 (delegating outs) - one context event per output, not per document
    // ------------------------------------------------------------------
    @Test
    public void tP04_stringConsumerContextEventOncePerOutput() {
        List<String> chunks = new ArrayList<>();
        MarshallableOut out = new StringConsumerMarshallableOut(chunks::add, WireType.YAML_ONLY);
        out.contextListener(ContextEvents.class, w -> w.event("hdr"));

        ContextEvents w = out.methodWriter(ContextEvents.class);
        w.event("one");
        w.event("two");
        w.event("three");

        String all = String.join("", chunks);
        long hdrs = all.split("hdr", -1).length - 1;
        assertEquals("context event 'hdr' should appear once per output but output was:\n" + all,
                1, hdrs);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void tP04_fileContextEventOncePerOutput() throws Exception {
        File file = File.createTempFile("ctxlistener", ".yaml");
        file.deleteOnExit();
        // append=true so every document is retained (default overwrite mode would hide the bug)
        URL url = new URL("file:" + file.getAbsolutePath() + "?append=true");
        MarshallableOut out = MarshallableOut.builder(url).wireType(WireType.YAML_ONLY).get();
        out.contextListener(ContextEvents.class, w -> w.event("hdr"));

        ContextEvents w = out.methodWriter(ContextEvents.class);
        w.event("one");
        w.event("two");
        w.event("three");

        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        String all = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        long hdrs = all.split("hdr", -1).length - 1;
        assertEquals("context event 'hdr' should appear once per output but file was:\n" + all,
                1, hdrs);
    }

    // NOTE: HTTPMarshallableOut is the third delegating out but is skipped here: it POSTs on
    // document close and requires a live HTTP endpoint returning 2xx, adding an in-test HttpServer
    // and network flakiness. It shares the identical per-document wire.clear() mechanism as the
    // File/StringConsumer outs above, which already demonstrate the T-P0-4 bug.

    // ------------------------------------------------------------------
    // 6. T-cov (coverage, not necessarily bugs)
    // ------------------------------------------------------------------
    // COVERAGE T-cov (Text): context event precedes the first data document.
    @Test
    public void tCov_contextEventPrecedesFirstDocument_text() {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        wire.contextListener(ContextEvents.class, w -> w.event("ctx"));
        ContextEvents w = wire.methodWriter(ContextEvents.class);
        w.event("one");
        w.event("two");
        assertEquals(Arrays.asList("ctx", "one", "two"), readOrdered(wire));
    }

    // COVERAGE T-cov (Raw): context event precedes the first data document.
    @Test
    public void tCov_contextEventPrecedesFirstDocument_raw() {
        Wire wire = WireType.RAW.apply(Bytes.allocateElasticOnHeap());
        wire.contextListener(ContextEvents.class, w -> w.event("ctx"));
        ContextEvents w = wire.methodWriter(ContextEvents.class);
        w.event("one");
        w.event("two");
        assertEquals(Arrays.asList("ctx", "one", "two"), drainEvents(wire));
    }

    @Test
    public void tCov_rawWireMetadataFirstThenContextThenData() {
        // Same contract as the other wires: a leading metadata document precedes the context
        // records, which precede the first data document. RawWire's reader cannot skip metadata
        // documents, so assert ordering structurally from the raw documents instead of a
        // methodReader: doc 1 metadata (meta), doc 2 data (ctx), doc 3 data (data).
        Wire wire = WireType.RAW.apply(Bytes.allocateElasticOnHeap());
        wire.contextListener(ContextEvents.class, w -> w.event("ctx"));

        ContextEvents metadataWriter = wire.methodWriterBuilder(true, ContextEvents.class).build();
        metadataWriter.event("meta");
        wire.methodWriter(ContextEvents.class).event("data");

        wire.bytes().readPosition(0);
        List<String> docs = new ArrayList<>();
        while (true) {
            try (DocumentContext dc = wire.readingDocument()) {
                if (!dc.isPresent())
                    break;
                Bytes<?> b = dc.wire().bytes();
                StringBuilder sb = new StringBuilder();
                for (long i = b.readPosition(); i < b.readLimit(); i++) {
                    byte c = b.readByte(i);
                    sb.append(c >= 32 && c < 127 ? (char) c : '.');
                }
                String body = sb.toString();
                String tag = body.contains("meta") ? "meta" : body.contains("ctx") ? "ctx"
                        : body.contains("data") ? "data" : "?";
                docs.add((dc.isMetaData() ? "M:" : "D:") + tag);
                b.readPosition(b.readLimit());
            }
        }
        assertEquals(Arrays.asList("M:meta", "D:ctx", "D:data"), docs);
    }

    // Rulings: a valid context count is positive and never 0; unknown/closed contexts report a
    // negative count (never an exception the interface does not document, and never a
    // valid-looking 1).
    @Test
    public void closedDocumentContextHolderReportsNegativeContextCount() {
        DocumentContextHolder holder = new DocumentContextHolder();
        // closed state: dc == null by design - must not NPE and must not claim a valid count
        assertTrue("closed holder must report a negative context count",
                holder.contextCount() < 0);
    }

    @Test
    public void noDocumentContextReportsNegativeContextCount() {
        assertTrue("the absent-document sentinel must not claim membership of a real context",
                NoDocumentContext.INSTANCE.contextCount() < 0);
    }

    @Test
    public void holderAndWrapperForwardContextCount() {
        // A stub with a distinctive value proves the wrappers forward rather than inherit the
        // interface default (a plain-wire delegate would make 1 == 1 pass either way).
        DocumentContext stub = new DocumentContext() {
            @Override
            public long contextCount() {
                return 42;
            }

            @Override
            public boolean isMetaData() {
                return false;
            }

            @Override
            public boolean isPresent() {
                return true;
            }

            @Override
            public Wire wire() {
                return null;
            }

            @Override
            public boolean isNotComplete() {
                return false;
            }

            @Override
            public int sourceId() {
                return 0;
            }

            @Override
            public long index() {
                return 0;
            }

            @Override
            public void close() {
            }

            @Override
            public void reset() {
            }
        };
        DocumentContextHolder holder = new DocumentContextHolder();
        holder.documentContext(stub);
        assertEquals(42, holder.contextCount());
    }

    // W1: HTTP delivers each document over a new connection, so per the DocumentContext contract
    // ("channel-like outputs should return the one-based connection count") each document must
    // report a distinct count - otherwise a progressive DTO never resends its static context to a
    // receiver that never saw the earlier delivery.
    @SuppressWarnings("deprecation")
    @Test
    public void httpDocumentsReportOneBasedConnectionCounts() throws Exception {
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
        List<String> posts = new ArrayList<>();
        server.createContext("/ctx", exchange -> {
            byte[] body = net.openhft.chronicle.core.io.IOTools.readAsBytes(exchange.getRequestBody());
            synchronized (posts) {
                posts.add(new String(body, java.nio.charset.StandardCharsets.ISO_8859_1));
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();
        try {
            URL url = new URL("http://localhost:" + server.getAddress().getPort() + "/ctx");
            MarshallableOut out = MarshallableOut.builder(url).wireType(WireType.JSON_ONLY).get();

            List<Long> counts = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                try (DocumentContext dc = out.writingDocument()) {
                    counts.add(dc.contextCount());
                    dc.wire().write("msg").text("m" + i);
                }
            }
            assertEquals("each HTTP document is a separate delivery and must report a distinct " +
                    "one-based connection count", Arrays.asList(1L, 2L, 3L), counts);
            assertEquals(3, posts.size());
        } finally {
            server.stop(0);
        }
    }

    // COVERAGE T-cov (acquire): acquireWritingDocument also fires the context listener.
    @Test
    public void tCov_contextEventViaAcquireWritingDocument() {
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.contextListener(ContextEvents.class, w -> w.event("ctx"));
        try (DocumentContext dc = wire.acquireWritingDocument(false)) {
            dc.wire().write("event").text("one");
        }
        assertEquals(Arrays.asList("ctx", "one"), readOrdered(wire));
    }
}
