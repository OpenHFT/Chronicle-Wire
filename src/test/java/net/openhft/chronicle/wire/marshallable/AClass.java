/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

// AClass extends the functionality of SelfDescribingMarshallable, providing custom serialization and deserialization for its fields.
class AClass extends SelfDescribingMarshallable {
    // Member variables
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

    // Constructor to initialize the AClass with given arguments.
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

    // Custom serialization logic for AClass's fields.
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

    // Custom deserialization logic for AClass's fields.
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
