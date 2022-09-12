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
import net.openhft.chronicle.wire.channel.ChronicleContext;
import net.openhft.chronicle.wire.channel.ChronicleGatewayMain;

import java.io.IOException;

public class ArithSvcMain {
    static final int PORT = Integer.getInteger("port", 6661);

    //private static final String URL = System.getProperty("url", "tcp://:" + PORT);

    public static void main (String[] args) throws IOException {
        System.setProperty("port", "" + PORT); // set if not set.
        ChronicleGatewayMain.main(args);
/*
        try (ChronicleContext context = ChronicleContext.newContext(URL)) {
            context.startNewGateway();
            Jvm.startup().on(ArithSvcMain.class, "Ready for messages");
            Jvm.park();
        }
*/
    }
}
