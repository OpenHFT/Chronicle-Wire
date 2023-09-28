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

/**
 * The ChronicleChannelSupplier class is a specialized version of ChronicleChannelCfg,
 * also implementing the Supplier interface to provide ChronicleChannel instances
 * based on a specified protocol. The protocols currently supported are 'tcp' and 'internal'.
 *
 * @see java.util.function.Supplier
 */
public class ChronicleChannelSupplier extends ChronicleChannelCfg<ChronicleChannelSupplier> implements Supplier<ChronicleChannel> {
    private final transient ChronicleContext context;
    private final ChannelHandler handler;
    private String protocol;
    private String connectionId;

    /**
     * Constructs a new ChronicleChannelSupplier with the provided context and handler.
     *
     * @param context the ChronicleContext that serves as the system context for this supplier
     * @param handler the ChannelHandler responsible for handling events on the channel
     */
    public ChronicleChannelSupplier(ChronicleContext context, ChannelHandler handler) {
        this.context = context;
        this.handler = handler;
    }

    /**
     * Depending on the specified protocol, it instantiates a new ChronicleChannel,
     * registers it with the context, and returns the newly created ChronicleChannel instance.
     *
     * @return a new instance of ChronicleChannel. The type of ChronicleChannel (TCP or internal)
     * depends on the protocol set on this supplier.
     * @throws IllegalArgumentException if the protocol is unsupported
     */
    @Override
    public ChronicleChannel get() {
        // Set the system context for the handler.
        handler.systemContext(context.systemContext());

        // If a connectionId is set and the handler has no sessionName,
        // set the sessionName of the handler to the connectionId.
        if (connectionId != null && handler.sessionName() == null)
            handler.sessionName(connectionId);

        // The new ChronicleChannel instance.
        final ChronicleChannel channel;
        switch (protocol) {
            case "tcp":
                // Create a new TCP ChronicleChannel.
                channel = ChronicleChannel.newChannel(context.socketRegistry(), this, handler);
                break;
            case "internal":
                // Create a new internal ChronicleChannel.
                channel = handler.asInternalChannel(context, this);
                break;
            default:
                // If the protocol is not supported, throw an exception.
                throw new IllegalArgumentException("Unsupported protocol " + protocol);
        }

        // Add the new ChronicleChannel to the context.
        context.addCloseable(channel);

        // Return the new ChronicleChannel.
        return channel;
    }

    /**
     * Retrieves the protocol used by this supplier for creating ChronicleChannel instances.
     *
     * @return the protocol as a string
     */
    public String protocol() {
        return protocol;
    }

    /**
     * Sets the protocol used by this supplier for creating ChronicleChannel instances.
     *
     * @param protocol the protocol as a string
     * @return this ChronicleChannelSupplier instance, to allow method chaining
     */
    public ChronicleChannelSupplier protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    /**
     * Retrieves the connection ID associated with this supplier.
     *
     * @return the connection ID as a string
     */
    public String connectionId() {
        return connectionId;
    }

    /**
     * Sets the connection ID associated with this supplier.
     *
     * @param connectionId the connection ID as a string
     * @return this ChronicleChannelSupplier instance, to allow method chaining
     */
    public ChronicleChannelSupplier connectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }
}
