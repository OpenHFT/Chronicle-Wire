/*
 * Copyright 2016-2020 chronicle.software
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
package net.openhft.chronicle.wire.reordered;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

public class NestedReadSubset implements Marshallable {
    String text;
    String text2;
    double number;
    double number2;
    double number4;

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        String text = wire.read("text").text();
        double number = wire.read("number").float64();
        setTextNumber(text, number);
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        // write version has way more fields
        wire.write(() -> "text").text(text)
                .write(() -> "number").float64(number)
                .write(() -> "text3").text("is text3")
                .write(() -> "number4").float64(number4)
                .write(() -> "number3").float64(333.3);
    }

    public NestedReadSubset setTextNumber(String text, double number) {
        this.text = text;
        this.number = number;
        this.number4 = number * 4;
        return this;
    }

    @NotNull
    @Override
    public String toString() {
        return "NestedReadSubset{" +
                "text='" + text + '\'' +
                ", text2='" + text2 + '\'' +
                ", number=" + number +
                ", number2=" + number2 +
                ", number4=" + number4 +
                '}';
    }
}
