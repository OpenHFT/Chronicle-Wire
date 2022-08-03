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

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;

class Rung extends SelfDescribingMarshallable {
    double price, qty;
    boolean delta;
    String notSet;

    public static void main(String[] args) {
        Rung x = new Rung();
        x.price = 1.234;
        x.qty = 1e6;
        System.out.println(x);
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        Wires.writeMarshallable(this, wire, false);
    }
}
