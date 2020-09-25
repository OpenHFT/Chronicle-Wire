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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

class AClass extends SelfDescribingMarshallable {
    int id;
    boolean flag;
    byte b;
    char ch;
    short s;
    int i;
    long l;
    float f;
    double d;
    String text;

    public AClass(int id, boolean flag, byte b, char ch, short s, int i, long l, float f, double d, String text) {
        this.id = id;
        this.flag = flag;
        this.b = b;
        this.ch = ch;
        this.s = s;
        this.i = i;
        this.l = l;
        this.f = f;
        this.d = d;
        this.text = text;
    }

    @Override
    public void writeMarshallable(@NotNull WireOut out) {
        out.write("id").writeInt(id);
        out.write("flag").writeBoolean(flag);
        out.write("b").writeByte(b);
        out.write("ch").writeChar(ch);
        out.write("s").writeShort(s);
        out.write("i").writeInt(i);
        out.write("l").writeLong(l);
        out.write("f").writeFloat(f);
        out.write("d").writeDouble(d);
        out.write("text").object(String.class, text);
    }

    @Override
    public void readMarshallable(@NotNull WireIn in) {
        id = in.read("id").readInt();
        flag = in.read("flag").readBoolean();
        b = in.read("b").readByte();
        ch = in.read("ch").readChar();
        s = in.read("s").readShort();
        i = in.read("i").readInt();
        l = in.read("l").readLong();
        f = in.read("f").readFloat();
        d = in.read("d").readDouble();
        text = in.read("text").object(text, String.class);
    }
}
