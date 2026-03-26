/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GenerateMethodWriter2CoverageTest extends WireTestCommon {

    @Test
    void generatesNestedWritersWithMethodIds() {
        String previous = System.getProperty("wire.generator.v2");
        System.setProperty("wire.generator.v2", "true");
        try {
            @NotNull Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
            wire.usePadding(true);

            Primary writer = wire.methodWriterBuilder(Primary.class).build();
            writer.say("hello");
            assertSame(writer, writer.reopen(), "non-terminating methods should return the same writer");

            Chain chain = writer.begin(17);
            chain.more(2).done();

            @NotNull RecordingPrimary recorder = new RecordingPrimary();
            wire.bytes().readPositionRemaining(0, wire.bytes().writePosition());
            MethodReader reader = wire.methodReader(recorder);
            while (reader.readOne()) {
                // drain
            }

            List<String> expected = new ArrayList<>();
            expected.add("say:hello");
            expected.add("reopen");
            expected.add("begin:17");
            expected.add("more:2");
            expected.add("done");
            assertEquals(expected, recorder.events, "Generated writer should emit method invocations in order");
            assertTrue(recorder.chainInvocations >= 2, "nested writer should be reused via thread-local state");
        } finally {
            if (previous != null)
                System.setProperty("wire.generator.v2", previous);
            else
                System.clearProperty("wire.generator.v2");
        }
    }

    interface Primary {
        @MethodId(7)
        void say(CharSequence text);

        Primary reopen();

        Chain begin(long id);
    }

    interface Chain {
        Chain more(long value);

        void done();
    }

    static final class RecordingPrimary implements Primary, Chain {
        final List<String> events = new ArrayList<>();
        int chainInvocations;

        @Override
        public void say(CharSequence text) {
            events.add("say:" + text);
        }

        @Override
        public Primary reopen() {
            events.add("reopen");
            return this;
        }

        @Override
        public Chain begin(long id) {
            events.add("begin:" + id);
            chainInvocations++;
            return this;
        }

        @Override
        public Chain more(long value) {
            events.add("more:" + value);
            chainInvocations++;
            return this;
        }

        @Override
        public void done() {
            events.add("done");
        }
    }
}
