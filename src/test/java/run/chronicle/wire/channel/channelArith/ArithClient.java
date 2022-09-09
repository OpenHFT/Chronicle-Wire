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

package run.chronicle.wire.channel.channelArith;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleContext;

public class ArithClient {
    private static final String URL = System.getProperty("url", "tcp://localhost:" + ArithSvcMain.PORT);

    public static void main(String[] args) {

        try (ChronicleContext context = ChronicleContext.newContext(URL)) {
            ArithHandler arithHandler = new ArithHandler(new Calculator());

            ChronicleChannel channel = context.newChannelSupplier(arithHandler).get();
            Jvm.startup().on(ArithClient.class, "Channel set up to: " + channel.channelCfg());

            final ArithListener outgoing = channel.methodWriter(ArithListener.class);

            outgoing.plus(3, 4);
            outgoing.minus(3, 4);
            outgoing.times(3, 4);

            StringBuilder evtType = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                double response = channel.readOne(evtType, double.class);
                Jvm.startup().on(ArithClient.class, " >>> " + response);
            }
        }
    }
}
