/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GenerateMethodWriter2CoverageTest extends WireTestCommon {

    @Test
    public void generatesNestedWritersWithMethodIds() {
        String previous = System.getProperty("wire.generator.v2");
        System.setProperty("wire.generator.v2", "true");
        try {
            @NotNull Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
            wire.usePadding(true);

            Primary writer = wire.methodWriterBuilder(Primary.class).build();
            writer.say("hello");
            assertSame("non-terminating methods should return the same writer", writer, writer.reopen());

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
            assertEquals("Generated writer should emit method invocations in order",
                    expected, recorder.events);
            assertTrue("nested writer should be reused via thread-local state",
                    recorder.chainInvocations >= 2);
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
