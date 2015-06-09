/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.BytesMarshaller;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class MyTypes implements Marshallable {
    final StringBuilder text = new StringBuilder();
    boolean b;
    short s;
    double d;
    long l;
    int i;

    void b(boolean b) {
        this.b = b;
    }

    void s(short s) {
        this.s = s;
    }

    void d(double d) {
        this.d = d;
    }

    void l(long l) {
        this.l = l;
    }

    void i(int i) {
        this.i = i;
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(Fields.B_FLAG).bool(b)
                .write(Fields.S_NUM).int16(s)
                .write(Fields.D_NUM).float64(d)
                .write(Fields.L_NUM).int64(l)
                .write(Fields.I_NUM).int32(i)
                .write(Fields.TEXT).text(text);
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) {
        wire.read(Fields.B_FLAG).bool(x -> b = x)
                .read(Fields.S_NUM).int16(this::s)
                .read(Fields.D_NUM).float64(this::d)
                .read(Fields.L_NUM).int64(this::l)
                .read(Fields.I_NUM).int32(this::i)
                .read(Fields.TEXT).textTo(text)
        ;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MyTypes myTypes = (MyTypes) o;

        if (b != myTypes.b) return false;
        if (Double.compare(myTypes.d, d) != 0) return false;
        if (i != myTypes.i) return false;
        if (l != myTypes.l) return false;
        if (s != myTypes.s) return false;
        if (!StringUtils.isEqual(text, myTypes.text)) return false;

        return true;
    }

    @NotNull
    @Override
    public String toString() {
        return "MyTypes{" +
                "text=" + text +
                ", b=" + b +
                ", s=" + s +
                ", d=" + d +
                ", l=" + l +
                ", i=" + i +
                '}';
    }

    enum Fields implements WireKey {
        B_FLAG, S_NUM, D_NUM, L_NUM, I_NUM, TEXT;

        @Override
        public int code() {
            return ordinal();
        }
    }

    public static void main(String... ignored) {
        BytesMarshaller<MyTypes> bytesMarshaller = MarshallableBytesMarshaller.of(TextWire::new, MyTypes::new);
    }
}
