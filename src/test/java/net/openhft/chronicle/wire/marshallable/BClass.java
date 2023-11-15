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

// BClass extends BytesInBinaryMarshallable, which is an abstract class to assist in writing and reading objects to and from binary formats.
class BClass extends BytesInBinaryMarshallable {

    // Fields that the class contains.
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

    // Constructor to initialize the BClass with the given arguments.
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

    // Specifies the version of the current marshalling format. Useful for serialization versioning.
    private static final int MASHALLABLE_VERSION = 1;

    // Implementation of the writeMarshallable method to serialize the object to a binary format.
    @Override
    public void writeMarshallable(BytesOut<?> out) {
        out.writeStopBit(MASHALLABLE_VERSION); // Writes the version number first.
        // Following lines write each of the fields in their respective formats.
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

    // Implementation of the readMarshallable method to deserialize the object from a binary format.
    @Override
    public void readMarshallable(BytesIn<?> in) {
        int version = (int) in.readStopBit(); // Reads the version number first.
        if (version == MASHALLABLE_VERSION) { // Checks if the version is as expected.
            // Following lines read each of the fields in their respective formats.
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
            // Throws an exception if the read version number doesn't match the expected version. Useful for detecting data format changes.
            throw new IllegalStateException("Unknown version " + version);
        }
    }
}
