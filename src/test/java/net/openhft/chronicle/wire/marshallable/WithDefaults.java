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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("rawtypes")
public class WithDefaults extends SelfDescribingMarshallable {
    Bytes<?> bytes = Bytes.from("Hello");
    String text = "Hello";
    boolean flag = true;
    int num = Integer.MIN_VALUE;
    Long num2 = Long.MIN_VALUE;
    double qty = Double.NaN;

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        Wires.writeMarshallable(this, wire, false);
    }
}
