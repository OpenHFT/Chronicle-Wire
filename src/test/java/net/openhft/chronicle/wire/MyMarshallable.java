/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
 * Created by Peter Lawrey on 11/05/16.
 */
class MyMarshallable extends AbstractMarshallable {

    @Nullable
    String someData;

    MyMarshallable(String someData) {
        this.someData = someData;
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "MyField").text(someData);
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        someData = wire.read(() -> "MyField").text();
    }
}
