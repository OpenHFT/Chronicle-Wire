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

public class TopOfBookHandler extends AbstractHandler<TopOfBookHandler> {
    private final ITopOfBookHandler nestedHandler;

    public TopOfBookHandler(ITopOfBookHandler nestedHandler) {
        this.nestedHandler = nestedHandler;
    }

    @Override
    public void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException {
        nestedHandler.out(channel.methodWriter(TopOfBookListener.class));
        channel.eventHandlerAsRunnable(nestedHandler).run();
    }

    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg) {
        throw new UnsupportedOperationException();
    }
}
