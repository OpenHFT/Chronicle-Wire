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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class ThreeSequence extends SelfDescribingMarshallable {
    @NotNull
    transient List<Rung> aBuffer = new ArrayList<>();
    @NotNull
    transient List<Rung> bBuffer = new ArrayList<>();
    @NotNull
    transient List<Rung> cBuffer = new ArrayList<>();
    @NotNull
    List<Rung> a = new ArrayList<>();
    @NotNull
    List<Rung> b = new ArrayList<>();
    @NotNull
    List<Rung> c = new ArrayList<>();
    @Nullable
    String text;

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        wire.read("b").sequence(b, bBuffer, Rung::new);
        wire.read("a").sequence(a, aBuffer, Rung::new);
        wire.read("c").sequence(c, cBuffer, Rung::new);
        text = wire.read("text").text();
    }
}

