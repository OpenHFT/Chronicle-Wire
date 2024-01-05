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

package net.openhft.chronicle.wire.channel.book;

import net.openhft.chronicle.core.io.ClosedIORuntimeException;
import net.openhft.chronicle.wire.channel.AbstractHandler;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelCfg;
import net.openhft.chronicle.wire.channel.ChronicleContext;

/**
 * Handler for managing the top of the book in a financial market, extending the abstract handler.
 */
public class TopOfBookHandler extends AbstractHandler<TopOfBookHandler> {

    // Nested handler for interfacing with the top of the book.
    private final ITopOfBookHandler nestedHandler;

    /**
     * Constructor initializing the handler with a given ITopOfBookHandler instance.
     *
     * @param nestedHandler An instance of a class implementing ITopOfBookHandler.
     */
    public TopOfBookHandler(ITopOfBookHandler nestedHandler) {
        this.nestedHandler = nestedHandler;
    }

    /**
     * Execute the run method for channel event handling and writing methods
     * related to the top of the book through the provided nested handler.
     *
     * @param context The context under which the chronicle is running.
     * @param channel The channel through which the chronicle communicates.
     * @throws ClosedIORuntimeException If an I/O error occurs.
     */
    @Override
    public void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException {
        // Direct method writing for TopOfBookListener class through the channel
        nestedHandler.out(channel.methodWriter(TopOfBookListener.class));

        // Execute the runnable event handler within the nested handler.
        channel.eventHandlerAsRunnable(nestedHandler).run();
    }

    /**
     * Method to convert the handler into an internal channel, however, is unsupported
     * and throws an exception in this implementation.
     *
     * @param context The context under which the chronicle is running.
     * @param channelCfg The configuration for the Chronicle channel.
     * @return Nothing.
     * @throws UnsupportedOperationException as this operation is not supported.
     */
    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg) {
        throw new UnsupportedOperationException();
    }
}
