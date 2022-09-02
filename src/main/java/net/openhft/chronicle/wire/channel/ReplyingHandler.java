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

import net.openhft.chronicle.core.io.ClosedIORuntimeException;

/**
 * This Handler performs a single action and returns a response in the reply header.
 * <p>
 * This would typically be either an OkHeader, ReplyHeader, ErrorHandler
 *
 * @param <H> the subclass
 */
public abstract class ReplyingHandler<H extends ReplyingHandler<H>> extends AbstractHandler<H> {
    @Override
    public abstract ChannelHeader responseHeader(ChronicleContext context);

    @Override
    public void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException {
        // nothing to do
    }

    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg) {
        throw new UnsupportedOperationException();
    }
}
