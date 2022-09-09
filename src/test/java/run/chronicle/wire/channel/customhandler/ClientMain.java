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

package run.chronicle.wire.channel.customhandler;

import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelSupplier;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientMain.class);
    private static final String url = "tcp://localhost:5556";

    public static void main(String[] args) {

        try (ChronicleContext context = ChronicleContext.newContext(url)) {
            BaseMessageHandler messageHandler = new BaseMessageHandler(new UCHandler());

            final ChronicleChannelSupplier supplier = context.newChannelSupplier(messageHandler);
            ChronicleChannel channel = supplier.get();
            LOGGER.info("Channel set up to: {}:{}", channel.channelCfg().hostname(), channel.channelCfg().port());

            final MessageListener echoing = channel.methodWriter(MessageListener.class);
            echoing.message("Hello, world");
            StringBuilder eventType = new StringBuilder();
            String text = channel.readOne(eventType, String.class);

            LOGGER.info(">>>> " + text);
        }
    }

}
