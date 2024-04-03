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
    int times;

    public int times() {
        return times;
    }

    public EchoNHandler times(int times) {
        this.times = times;
        return this;
    }

    @Override
    public void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException {
        Pauser pauser = Pauser.balanced();
        while (!channel.isClosed()) {
            try (DocumentContext dc = channel.readingDocument()) {
                if (!dc.isPresent()) {
                    pauser.pause();
                    continue;
                }
                final Wire wire = dc.wire();
                final long position = wire.bytes().readPosition();
                for (int i = 0; i < times; i++) {
                    wire.bytes().readPosition(position);
                    try (DocumentContext dc2 = channel.writingDocument(dc.isMetaData())) {
                        wire.copyTo(dc2.wire());
                    }
                }
                pauser.reset();
            }
        }
    }

    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg<?>channelCfg) {
        return new EchoChannel(channelCfg);
    }
}
