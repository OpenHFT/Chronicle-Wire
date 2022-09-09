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

package run.chronicle.wire.channel.demo2;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import net.openhft.chronicle.wire.channel.echo.EchoHandler;
import net.openhft.chronicle.wire.channel.echo.Says;

public class ChannelClient {

    private static final String URL = System.getProperty("url", "tcp://localhost:" + ChannelService.PORT);

    public static void main(String[] args) {

        try (ChronicleContext context = ChronicleContext.newContext(URL).name("ChannelClient");
             ChronicleChannel channel = context.newChannelSupplier(new EchoHandler()).get()) {

            Jvm.startup().on(ChannelClient.class, "Channel set up on port: " + channel.channelCfg().port());
            Says says = channel.methodWriter(Says.class);
            says.say("Well hello there");

            StringBuilder eventType = new StringBuilder();
            String text = channel.readOne(eventType, String.class);

            Jvm.startup().on(ChannelClient.class, ">>>> " + eventType + ": " + text);
        }
    }
}
