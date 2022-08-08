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

package net.openhft.chronicle.wire.channel;

import java.util.function.Supplier;

public class ChronicleChannelSupplier extends ChronicleChannelCfg<ChronicleChannelSupplier> implements Supplier<ChronicleChannel> {
    private final ChronicleContext context;
    private final ChannelHandler handler;
    private String protocol;
    private String connectionId;

    public ChronicleChannelSupplier(ChronicleContext context, ChannelHandler handler) {
        this.context = context;
        this.handler = handler;
    }

    @Override
    public ChronicleChannel get() {
        handler.systemContext(context.systemContext());
        if (connectionId != null && handler.sessionName() == null)
            handler.sessionName(connectionId);
        final ChronicleChannel channel;
        switch (protocol) {
            case "tcp":
                channel = ChronicleChannel.newChannel(context.socketRegistry(), this, handler);
                break;
            case "internal":
                channel = handler.asInternalChannel(context, this);
                break;
            default:
                throw new IllegalArgumentException("Unsupported protocol " + protocol);
        }
        context.addCloseable(channel);
        return channel;
    }

    public String protocol() {
        return protocol;
    }

    public ChronicleChannelSupplier protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public String connectionId() {
        return connectionId;
    }

    public ChronicleChannelSupplier connectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }
}
