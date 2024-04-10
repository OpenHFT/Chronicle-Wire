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

import net.openhft.chronicle.core.io.ClosedIORuntimeException;
import net.openhft.chronicle.wire.channel.AbstractHandler;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelCfg;
import net.openhft.chronicle.wire.channel.ChronicleContext;

/**
 * 'ChannelVisitorHandler' class extends 'AbstractHandler' and is responsible for
 * handling events in the context of channels by utilizing ChannelVisitors.
 * <p>
 * When 'run' is invoked, it sets up a mechanism to reply to received messages
 * by performing an operation defined by a ChannelVisitor on the provided channel.
 * Note that the actual visitor logic is abstract and must be defined elsewhere.
 * <p>
 * 'asInternalChannel' is not supported in this implementation.
 */
public class ChannelVisitorHandler extends AbstractHandler<ChannelVisitorHandler> {

    /**
     * Executes logic to handle events on the provided channel within the provided context.
     * <p>
     * The 'run' method sets up a 'Replies' instance and assigns to it a lambda function
     * that takes a 'ChannelVisitor', calls its 'visit' method with the channel as argument,
     * and sends the resulting message as a reply.
     *
     * @param context the ChronicleContext within which the operation is performed.
     * @param channel the ChronicleChannel on which the event is to be handled.
     * @throws ClosedIORuntimeException if an I/O error occurs.
     */
    @Override
    public void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException {
        // Acquire a method writer for the Replies interface
        Replies replies = channel.methodWriter(Replies.class);

        // Define the visiting logic using a lambda function that uses the visitor to generate replies
        ChannelVisiting visiting = visitor -> replies.reply(visitor.visit(channel));

        // Assign the visiting logic as the event handler for the channel and run it
        channel.eventHandlerAsRunnable(visiting).run();
    }

    /**
     * Throws UnsupportedOperationException as this functionality is not supported.
     * <p>
     * This method is intended to provide an internal channel based on provided
     * context and channel configuration. However, the functionality is not supported
     * in this implementation and will throw an exception if called.
     *
     * @param context the ChronicleContext within which the channel would be created.
     * @param channelCfg the configuration for the channel.
     * @throws UnsupportedOperationException always, as the operation is not supported.
     */
    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg<?> channelCfg) {
        throw new UnsupportedOperationException();
    }
}
