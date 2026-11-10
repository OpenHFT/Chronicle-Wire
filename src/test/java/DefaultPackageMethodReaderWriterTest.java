/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.AbstractGeneratedMethodReader;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.method.Funding;
import net.openhft.chronicle.wire.method.FundingListener;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Interfaces and handlers declared in the default package must use generated method writers and
 * readers. The generated class name used to start with '.', so code generation failed and both
 * fell back to proxies with a "falling back" warning, which {@link WireTestCommon} reports.
 */
public class DefaultPackageMethodReaderWriterTest extends WireTestCommon {

    private static final String EXPECTED = "writer=generated reader=generated events=[say:hello, count:3]";

    @Test
    public void namedHandlerInDefaultPackage() {
        Recorder recorder = new Recorder();
        assertEquals(EXPECTED, roundTrip(WireType.BINARY, Calls.class, recorder, recorder.events));
    }

    @Test
    public void anonymousHandlerInDefaultPackage() {
        List<String> events = new ArrayList<>();
        Calls handler = new Calls() {
            @Override
            public void say(String text) {
                events.add("say:" + text);
            }

            @Override
            public void count(int n) {
                events.add("count:" + n);
            }
        };
        assertEquals(EXPECTED, roundTrip(WireType.TEXT, Calls.class, handler, events));
    }

    /**
     * The incident's shape: a default-package probe reads a named-package interface whose
     * argument is a DTO, through an anonymous handler. Only the reader is in the default package.
     */
    @Test
    public void anonymousHandlerForNamedPackageInterface() {
        List<String> events = new ArrayList<>();
        FundingListener handler = new FundingListener() {
            @Override
            public void funding(Funding funding) {
                events.add("funding:" + funding.fr());
            }

            @Override
            public void fundingPrimitive(int num) {
                events.add("primitive:" + num);
            }

            @Override
            public void fundingNoArg() {
                events.add("noArg");
            }
        };
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);
        FundingListener writer = wire.methodWriter(FundingListener.class);
        writer.funding(new Funding().fr(0.25));
        writer.fundingPrimitive(7);

        MethodReader reader = wire.methodReader(handler);
        while (reader.readOne()) {
            // drain
        }
        assertEquals("reader=generated events=[funding:0.25, primitive:7]",
                "reader=" + (reader instanceof AbstractGeneratedMethodReader ? "generated" : "proxy")
                        + " events=" + events);
    }

    @Test
    public void generatorV2WriterInDefaultPackage() {
        String previous = System.getProperty("wire.generator.v2");
        System.setProperty("wire.generator.v2", "true");
        try {
            V2Recorder recorder = new V2Recorder();
            assertEquals(EXPECTED, roundTrip(WireType.BINARY, V2Calls.class, recorder, recorder.events));
        } finally {
            if (previous != null)
                System.setProperty("wire.generator.v2", previous);
            else
                System.clearProperty("wire.generator.v2");
        }
    }

    /**
     * Writes two calls and reads them back, describing which writer and reader kinds were used
     * so a failure reports both sides at once.
     */
    private static <T extends Calls> String roundTrip(WireType wireType, Class<T> type, T handler, List<String> events) {
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        T writer = wire.methodWriter(type);
        writer.say("hello");
        writer.count(3);

        MethodReader reader = wire.methodReader(handler);
        while (reader.readOne()) {
            // drain
        }
        return "writer=" + (Proxy.isProxyClass(writer.getClass()) ? "proxy" : "generated")
                + " reader=" + (reader instanceof AbstractGeneratedMethodReader ? "generated" : "proxy")
                + " events=" + events;
    }

    public interface Calls {
        void say(String text);

        void count(int n);
    }

    public interface V2Calls extends Calls {
    }

    static class Recorder implements Calls {
        final List<String> events = new ArrayList<>();

        @Override
        public void say(String text) {
            events.add("say:" + text);
        }

        @Override
        public void count(int n) {
            events.add("count:" + n);
        }
    }

    static final class V2Recorder extends Recorder implements V2Calls {
    }
}
