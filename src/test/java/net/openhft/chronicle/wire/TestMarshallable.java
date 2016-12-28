/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * Created by daniel on 08/04/15.
 */
public class TestMarshallable extends AbstractMarshallable {

    @NotNull
    private StringBuilder name = new StringBuilder();
    private int count;

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        wire.read(() -> "name").textTo(name);
        count = wire.read(() -> "count").int32();
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "name").text(name);
        wire.write(() -> "count").int32(count);

    }

    @NotNull
    public StringBuilder getName() {
        return name;
    }

    public void setName(CharSequence name) {
        this.name.setLength(0);
        this.name.append(name);
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
