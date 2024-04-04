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

import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.core.io.ClosedIORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.channel.AbstractHandler;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelCfg;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import net.openhft.chronicle.wire.channel.echo.internal.EchoChannel;

/**
 * An implementation of a channel handler that primarily echoes back any incoming data.
 * The handler extends {@link AbstractHandler}, and implements the `run` and `asInternalChannel` methods
 * for specific behavior.
 * <p>
 * This handler acquires an {@link AffinityLock} before running, and releases it after
 * the {@link ChronicleChannel} is closed. While the channel is open, it continuously reads incoming data,
 * echoing it back to the sender.
 * <p>
 * When there is no data available, it invokes the {@link Pauser} to pause execution,
 * reducing the CPU usage when idle. If data is available, it resets the pauser to wake up immediately
 * the next time it checks for data.
 */
public class EchoHandler extends AbstractHandler<EchoHandler> {

    /**
     * Executes the main logic of this EchoHandler, which continuously reads incoming data from the
     * provided channel and echoes it back to the sender.
     *
     * @param context the ChronicleContext in which this handler operates
     * @param channel the ChronicleChannel from which to read the data
     * @throws ClosedIORuntimeException     if the channel is closed unexpectedly
     * @throws InvalidMarshallableException if there's an issue while processing the data
     */
    @SuppressWarnings("try")
    @Override
    public void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException, InvalidMarshallableException {
        try (AffinityLock lock = context.affinityLock()) {
            Pauser pauser = Pauser.balanced();
            while (!channel.isClosed()) {
                try (DocumentContext dc = channel.readingDocument()) {
                    if (!dc.isPresent()) {
                        pauser.pause();
                        continue;
                    }
                    try (DocumentContext dc2 = channel.writingDocument(dc.isMetaData())) {
                        dc.wire().copyTo(dc2.wire());
                    }
                    pauser.reset();
                }
            }
        }
    }

    /**
     * Returns an {@link EchoChannel} as the internal channel for this handler.
     *
     * @param context    the ChronicleContext in which this handler operates
     * @param channelCfg the ChronicleChannelCfg that configures the channel
     * @return a new EchoChannel instance
     */
    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg<?> channelCfg) {
        return new EchoChannel(channelCfg);
    }
}
