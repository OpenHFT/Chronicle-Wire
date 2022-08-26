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

package net.openhft.chronicle.wire.channel.lease;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.wire.ReplyHeader;
import net.openhft.chronicle.wire.ReplyingHandler;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.channel.ChannelHeader;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import net.openhft.chronicle.wire.channel.ChronicleGatewayMain;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

class LeaseCacheTest extends WireTestCommon {

    @Test
    void requestLeases() throws IOException {
        String path = OS.getTarget() + "/requestLeases-" + System.nanoTime();
        new File(path).mkdirs();
        final DummyLeaseProvider provider = new DummyLeaseProvider("2022-09-01T01:00:00");
        LeaseCache cache = new LeaseCache("mac", path, provider);
        final Lease lease1 = cache.requestLease("type", "one");
        assertEquals("" +
                "!net.openhft.chronicle.wire.channel.lease.Lease {\n" +
                "  mac: mac,\n" +
                "  type: type,\n" +
                "  name: one,\n" +
                "  notes: \"\",\n" +
                "  expiry: 2022-09-01T01:00:00,\n" +
                "  checkSum: A\n" +
                "}\n", lease1.toString());

        final List<Lease> leases = cache.requestLeases("mac", "type", Arrays.asList("two", "three"));
        assertEquals("" +
                "[!net.openhft.chronicle.wire.channel.lease.Lease {\n" +
                "  mac: mac,\n" +
                "  type: type,\n" +
                "  name: two,\n" +
                "  notes: \"\",\n" +
                "  expiry: 2022-09-01T01:00:00,\n" +
                "  checkSum: B\n" +
                "}\n" +
                ", !net.openhft.chronicle.wire.channel.lease.Lease {\n" +
                "  mac: mac,\n" +
                "  type: type,\n" +
                "  name: three,\n" +
                "  notes: \"\",\n" +
                "  expiry: 2022-09-01T01:00:00,\n" +
                "  checkSum: C\n" +
                "}\n" +
                "]", leases.toString());

        final DummyLeaseProvider provider2 = new DummyLeaseProvider("2022-09-02T02:00:00");
        LeaseCache cache2 = new LeaseCache("mac", path, provider2);
        final Lease lease2 = cache2.requestLease("type", "one");
        assertEquals("" +
                "!net.openhft.chronicle.wire.channel.lease.Lease {\n" +
                "  mac: mac,\n" +
                "  type: type,\n" +
                "  name: one,\n" +
                "  notes: \"\",\n" +
                "  expiry: 2022-09-01T01:00:00,\n" +
                "  checkSum: A\n" +
                "}\n", lease2.toString());

        final List<Lease> leases2 = cache2.requestLeases("mac", "type", Arrays.asList("two", "three", "four"));
        assertEquals("" +
                "[!net.openhft.chronicle.wire.channel.lease.Lease {\n" +
                "  mac: mac,\n" +
                "  type: type,\n" +
                "  name: two,\n" +
                "  notes: \"\",\n" +
                "  expiry: 2022-09-01T01:00:00,\n" +
                "  checkSum: B\n" +
                "}\n" +
                ", !net.openhft.chronicle.wire.channel.lease.Lease {\n" +
                "  mac: mac,\n" +
                "  type: type,\n" +
                "  name: three,\n" +
                "  notes: \"\",\n" +
                "  expiry: 2022-09-01T01:00:00,\n" +
                "  checkSum: C\n" +
                "}\n" +
                ", !net.openhft.chronicle.wire.channel.lease.Lease {\n" +
                "  mac: mac,\n" +
                "  type: type,\n" +
                "  name: four,\n" +
                "  notes: \"\",\n" +
                "  expiry: 2022-09-02T02:00:00,\n" +
                "  checkSum: A\n" +
                "}\n" +
                "]", leases2.toString());

        IOTools.deleteDirWithFiles(path);
    }

    @Test
    public void redirectedServer() throws IOException {
        final DummyLeaseProvider provider = new DummyLeaseProvider("2022-09-01T01:00:00");

        try (ChronicleGatewayMain gateway0 = new ChronicleGatewayMain("tcp://:0") {
            @Override
            protected ChannelHeader replaceInHeader(ChannelHeader channelHeader) {
                if (channelHeader instanceof LeaseRequestHandler) {
                    final LeaseRequestHandler lrh = (LeaseRequestHandler) channelHeader;
                    return new ReplyingHandler() {
                        @Override
                        public ChannelHeader responseHeader(ChronicleContext context) {
                            return new ReplyHeader<>().reply(provider.requestLeases(lrh.mac(), lrh.type(), lrh.names()));
                        }
                    };
                }
                return channelHeader;
            }
        }) {
            gateway0.start();
            // as we overrode the benahviour above it needs to be a separate context
            try (ChronicleContext context = ChronicleContext.newContext("tcp://localhost:" + gateway0.port())) {
                final ChronicleChannel channel = context.newChannelSupplier(new LeaseRequestHandler().mac("mac").type("type").names(Arrays.asList("one", "two", "three"))).get();
                assertEquals("!net.openhft.chronicle.wire.ReplyHeader {\n" +
                        "  systemContext: {\n" +
                        "    availableProcessors: 32,\n" +
                        "    hostId: 0,\n" +
                        "    hostName: dev-a,\n" +
                        "    upTime: 2022-08-26T16:33:26.27300615,\n" +
                        "    userCountry: GB,\n" +
                        "    userName: peter\n" +
                        "  },\n" +
                        "  sessionName: !!null \"\",\n" +
                        "  reply: [\n" +
                        "    !net.openhft.chronicle.wire.channel.lease.Lease { mac: mac, type: type, name: one, notes: \"\", expiry: 2022-09-01T01:00:00, checkSum: A },\n" +
                        "    !net.openhft.chronicle.wire.channel.lease.Lease { mac: mac, type: type, name: two, notes: \"\", expiry: 2022-09-01T01:00:00, checkSum: B },\n" +
                        "    !net.openhft.chronicle.wire.channel.lease.Lease { mac: mac, type: type, name: three, notes: \"\", expiry: 2022-09-01T01:00:00, checkSum: C }\n" +
                        "  ]\n" +
                        "}\n", channel.headerIn().toString());
            }
        }
    }
}