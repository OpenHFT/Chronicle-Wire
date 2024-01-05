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
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.channel.AbstractHandler;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelCfg;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import net.openhft.chronicle.wire.channel.echo.internal.EchoChannel;

public class EchoNHandler extends AbstractHandler<EchoNHandler> {
    // The number of times the message will be echoed back.
    int times;

    // Getter method for times variable.
    public int times() {
        return times;
    }

    // Setter method for times variable with a fluent API design pattern.
    public EchoNHandler times(int times) {
        this.times = times;
        return this;
    }

    // The main run method where the logic for echoing messages is implemented.
    @Override
    public void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException {
        // Create a pauser which manages pausing and resuming of a thread in a balanced manner.
        Pauser pauser = Pauser.balanced();

        // Continuously check the channel until it is closed.
        while (!channel.isClosed()) {
            // Try-with-resources block ensures that DocumentContext (dc) is closed properly after use.
            try (DocumentContext dc = channel.readingDocument()) {
                // Check if the document context is not present and pause if so.
                if (!dc.isPresent()) {
                    pauser.pause();
                    continue;
                }
                // Obtain the wire from the document context.
                final Wire wire = dc.wire();
                // Store the current read position of the byte stream.
                final long position = wire.bytes().readPosition();

                // Loop for a defined number of times to echo the message.
                for (int i = 0; i < times; i++) {
                    // Reset the read position for each iteration.
                    wire.bytes().readPosition(position);
                    // Try-with-resources block to ensure the DocumentContext (dc2) is closed properly.
                    try (DocumentContext dc2 = channel.writingDocument(dc.isMetaData())) {
                        // Copy the wire data to the new writing document context.
                        wire.copyTo(dc2.wire());
                    }
                }
                // Reset the pauser to avoid unnecessary CPU usage.
                pauser.reset();
            }
        }
    }

    // This method creates and returns an EchoChannel object with specific configurations.
    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg) {
        // Return a new instance of EchoChannel with the given configuration.
        return new EchoChannel(channelCfg);
    }
}
