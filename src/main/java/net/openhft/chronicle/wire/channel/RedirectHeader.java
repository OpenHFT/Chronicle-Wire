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

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedirectHeader extends SelfDescribingMarshallable implements ChannelHeader {
    private final List<String> locations = new ArrayList<>();
    private SystemContext systemContext;
    private String connectionId;

    public RedirectHeader(List<String> locations) {
        this.locations.addAll(locations);
    }

    @Override
    public SystemContext systemContext() {
        return systemContext;
    }

    @Override
    public RedirectHeader systemContext(SystemContext systemContext) {
        this.systemContext = systemContext;
        return this;
    }

    @Override
    public String connectionId() {
        return connectionId;
    }

    @Override
    public ChannelHeader connectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    public List<String> locations() {
        return Collections.unmodifiableList(locations);
    }
}
