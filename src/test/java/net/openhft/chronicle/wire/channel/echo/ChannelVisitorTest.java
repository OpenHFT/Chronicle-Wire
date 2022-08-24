/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.channel.echo;

import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChannelVisitorTest {
    static <T> T visitOne(ChronicleChannel channel, ChannelVisiting visiting, ChannelVisitor<T> visitor) {
        StringBuilder reply = new StringBuilder();
        visiting.visitor(visitor);
        return (T) channel.readOne(reply, Object.class);
    }

    @Test
    public void testChannel() {
        try (ChronicleContext context = ChronicleContext.newContext("tcp://:0?sessionName=testId");
             final ChronicleChannel channel = context.newChannelSupplier(new ChannelVisitorHandler()).get()) {
            final ChannelVisiting visiting = channel.methodWriter(ChannelVisiting.class);
            int hostId = visitOne(channel, visiting, new ChannelHostId());
            assertEquals(0, hostId);

            String connectionId = visitOne(channel, visiting, new ChannelSessionName());
            assertEquals("testId", connectionId);
        }
    }

    static class ChannelSessionName extends ChannelVisitor<String> {
        @Override
        public String visit(ChronicleChannel channel) {
            return channel.headerIn().sessionName();
        }
    }


    static class ChannelHostId extends ChannelVisitor<Integer> {
        @Override
        public Integer visit(ChronicleChannel channel) {
            return channel.headerIn().systemContext().hostId();
        }
    }
}
