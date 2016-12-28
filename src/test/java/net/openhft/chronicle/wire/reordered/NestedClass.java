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

package net.openhft.chronicle.wire.reordered;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

/**
 * Created by peter.lawrey on 01/02/2016.
 */
public class NestedClass implements Marshallable {
    String text;
    String text2;
    double number;
    double number2;
    double number4; // out of order

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        wire.read(() -> "text").text(this, (t, v) -> t.text = v)
                .read(() -> "text2").text(this, (t, v) -> t.text2 = v)
                .read(() -> "number").float64(this, (t, v) -> t.number = v)
                .read(() -> "number2").float64(this, (t, v) -> t.number2 = v)
                .read(() -> "number4").float64(this, (t, v) -> t.number4 = v);
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        // write version has 2 extra fields but is missing two fields
        wire.write(() -> "text").text(text)
                .write(() -> "text3").text("is text3")
                .write(() -> "number4").float64(number4)
                .write(() -> "number").float64(number)
                .write(() -> "number3").float64(333.3);
    }

    public void setTextNumber(String text, double number) {
        this.text = text;
        this.number = number;
        this.number4 = number * 4;
    }

    @NotNull
    @Override
    public String toString() {
        return "NestedClass{" +
                "text='" + text + '\'' +
                ", text2='" + text2 + '\'' +
                ", number=" + number +
                ", number2=" + number2 +
                ", number4=" + number4 +
                '}';
    }
}
