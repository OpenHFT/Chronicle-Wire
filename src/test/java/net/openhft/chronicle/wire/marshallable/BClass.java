/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;

class BClass extends BytesInBinaryMarshallable {
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

    public BClass(int id, boolean flag, byte b, char ch, short s, int i, long l, float f, double d, String text) {
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

    // from generated code
    private static final int MASHALLABLE_VERSION = 1;

    @Override
    public void writeMarshallable(BytesOut<?> out) {
        out.writeStopBit(MASHALLABLE_VERSION);
        out.writeInt(id);
        out.writeBoolean(flag);
        out.writeByte(b);
        out.writeChar(ch);
        out.writeShort(s);
        out.writeInt(i);
        out.writeLong(l);
        out.writeFloat(f);
        out.writeDouble(d);
        out.writeObject(String.class, text);
    }

    @Override
    public void readMarshallable(BytesIn<?> in) {
        int version = (int) in.readStopBit();
        if (version == MASHALLABLE_VERSION) {
            id = in.readInt();
            flag = in.readBoolean();
            b = in.readByte();
            ch = in.readChar();
            s = in.readShort();
            i = in.readInt();
            l = in.readLong();
            f = in.readFloat();
            d = in.readDouble();
            text = (String) in.readObject(String.class);
        } else {
            throw new IllegalStateException("Unknown version " + version);
        }
    }
}
