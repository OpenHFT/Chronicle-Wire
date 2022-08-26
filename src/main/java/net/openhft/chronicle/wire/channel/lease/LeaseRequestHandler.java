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

package net.openhft.chronicle.wire.channel.lease;

import net.openhft.chronicle.wire.ReplyingHandler;
import net.openhft.chronicle.wire.channel.*;

import java.util.List;

public class LeaseRequestHandler extends ReplyingHandler<LeaseRequestHandler> implements ChannelHandler {
    private String mac;
    private String type;
    private List<String> names;

    public String mac() {
        return mac;
    }

    public LeaseRequestHandler mac(String mac) {
        this.mac = mac;
        return this;
    }

    public String type() {
        return type;
    }

    public LeaseRequestHandler type(String type) {
        this.type = type;
        return this;
    }

    public List<String> names() {
        return names;
    }

    public LeaseRequestHandler names(List<String> names) {
        this.names = names;
        return this;
    }

    @Override
    public ChannelHeader responseHeader(ChronicleContext context) {
        // functionality has to be overridden by the server otherwise it can't issue a lease
        return new ErrorHeader().errorMsg("Lease allocation not supported, you need a lease server");
    }
}
