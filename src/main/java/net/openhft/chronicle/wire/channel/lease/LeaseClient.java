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

import net.openhft.chronicle.wire.ReplyHeader;
import net.openhft.chronicle.wire.channel.ChannelHeader;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import net.openhft.chronicle.wire.channel.ErrorHeader;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LeaseClient implements LeaseProvider {
    private final String url;

    public LeaseClient(String url) {
        this.url = url;
    }

    @Override
    public List<Lease> requestLeases(String mac, String type, List<String> names) throws IllegalStateException {
        final LeaseRequestHandler lrh = new LeaseRequestHandler()
                .mac(mac)
                .type(type)
                .names(names);

        try (ChronicleContext context = ChronicleContext.newContext(url);
             ChronicleChannel channel = context.newChannelSupplier(lrh).get()) {
            final ChannelHeader header = channel.headerIn();
            if (header instanceof ReplyHeader) {
                Object reply = ((ReplyHeader) header).reply();
                if (reply instanceof Collection) {
                    final Object collect = ((Collection) reply).stream()
                            .map(e -> (Lease) e)
                            .collect(Collectors.toList());
                    return (List<Lease>) collect;
                }
                throw new IllegalStateException("Replied " + reply);
            }
            if (header instanceof ErrorHeader)
                throw new IllegalStateException(((ErrorHeader) header).errorMsg());
            throw new IllegalStateException("Unexpected header " + header);
        }
    }
}
