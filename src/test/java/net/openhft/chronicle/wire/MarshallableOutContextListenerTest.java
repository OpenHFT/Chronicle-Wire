/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import com.sun.net.httpserver.HttpServer;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.wire.internal.StringConsumerMarshallableOut;
import org.junit.Test;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class MarshallableOutContextListenerTest extends WireTestCommon {
    private static final String CONTEXT_AND_FIRST_EVENT = "" +
            "context: {\n" +
            "  name: schema,\n" +
            "  version: 7\n" +
            "}\n" +
            "...\n" +
            "event: {\n" +
            "  name: one,\n" +
            "  sequence: 1\n" +
            "}\n" +
            "...\n";
    private static final String SECOND_EVENT = "" +
            "event: {\n" +
            "  name: two,\n" +
            "  sequence: 2\n" +
            "}\n" +
            "...\n";
    private static final String THIRD_EVENT = "" +
            "event: {\n" +
            "  name: three,\n" +
            "  sequence: 3\n" +
            "}\n" +
            "...\n";

    @Test
    public void stringConsumerWritesContextOnceBeforeEvents() {
        List<String> chunks = new ArrayList<>();
        MarshallableOut out = new StringConsumerMarshallableOut(chunks::add, WireType.YAML_ONLY);
        ContextEvents writer = contextWriter(out);

        writer.event(new EventData("one", 1));
        writer.event(new EventData("two", 2));
        writer.event(new EventData("three", 3));

        assertEquals(Arrays.asList(CONTEXT_AND_FIRST_EVENT, SECOND_EVENT, THIRD_EVENT), chunks);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void appendFileWritesContextOnceBeforeEvents() throws Exception {
        File file = File.createTempFile("context-listener", ".yaml");
        try {
            URL url = new URL(file.toURI().toURL() + "?append=true");
            MarshallableOut out = MarshallableOut.builder(url)
                    .wireType(WireType.YAML_ONLY)
                    .get();
            ContextEvents writer = contextWriter(out);

            writer.event(new EventData("one", 1));
            writer.event(new EventData("two", 2));
            writer.event(new EventData("three", 3));

            assertEquals(CONTEXT_AND_FIRST_EVENT + SECOND_EVENT + THIRD_EVENT,
                    new String(Files.readAllBytes(file.toPath()), StandardCharsets.ISO_8859_1));
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void overwriteFileRejectsContextListener() throws Exception {
        File file = File.createTempFile("context-listener-overwrite", ".yaml");
        try {
            MarshallableOut out = MarshallableOut.builder(file.toURI().toURL())
                    .wireType(WireType.YAML_ONLY)
                    .get();

            UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                    () -> out.contextListener(ContextEvents.class,
                            writer -> writer.context(new ContextData("schema", 7))));
            assertEquals("contextListener requires append mode (add ?append=true to the file URL): " +
                            "in overwrite mode each document replaces the file, discarding the context records",
                    thrown.getMessage());
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void httpWritesContextIntoEveryPostAndIncreasesCountOnlyOnOpen() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        List<String> posts = Collections.synchronizedList(new ArrayList<>());
        server.createContext("/context", exchange -> {
            byte[] body = IOTools.readAsBytes(exchange.getRequestBody());
            posts.add(new String(body, StandardCharsets.ISO_8859_1));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            URL url = new URL("http://localhost:" + server.getAddress().getPort() + "/context");
            MarshallableOut out = MarshallableOut.builder(url)
                    .wireType(WireType.JSON_ONLY)
                    .get();
            out.contextListener(ContextEvents.class,
                    writer -> writer.context(new ContextData("schema", 7)));

            List<Integer> openContextCounts = new ArrayList<>();
            List<Integer> closedContextCounts = new ArrayList<>();
            String[] names = {"one", "two", "three"};
            for (int i = 0; i < names.length; i++) {
                DocumentContext dc = out.writingDocument();
                try {
                    openContextCounts.add(dc.contextCount());
                    dc.wire().write("event").marshallable(new EventData(names[i], i + 1));
                } finally {
                    dc.close();
                }
                closedContextCounts.add(dc.contextCount());
            }

            assertEquals(Arrays.asList(1, 2, 3), openContextCounts);
            assertEquals(openContextCounts, closedContextCounts);
            assertEquals(Arrays.asList(
                    "{\"context\":{\"name\":\"schema\",\"version\":7}}" +
                            "{\"event\":{\"name\":\"one\",\"sequence\":1}}\n",
                    "{\"context\":{\"name\":\"schema\",\"version\":7}}" +
                            "{\"event\":{\"name\":\"two\",\"sequence\":2}}\n",
                    "{\"context\":{\"name\":\"schema\",\"version\":7}}" +
                            "{\"event\":{\"name\":\"three\",\"sequence\":3}}\n"),
                    posts);
        } finally {
            server.stop(0);
        }
    }

    private static ContextEvents contextWriter(MarshallableOut out) {
        out.contextListener(ContextEvents.class,
                writer -> writer.context(new ContextData("schema", 7)));
        return out.methodWriter(ContextEvents.class);
    }

    interface ContextEvents {
        void context(ContextData context);

        void event(EventData event);
    }

    static final class ContextData extends SelfDescribingMarshallable {
        String name;
        int version;

        ContextData(String name, int version) {
            this.name = name;
            this.version = version;
        }
    }

    static final class EventData extends SelfDescribingMarshallable {
        String name;
        long sequence;

        EventData(String name, long sequence) {
            this.name = name;
            this.sequence = sequence;
        }
    }
}
