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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;

import java.io.ObjectOutput;
import java.util.List;
import java.util.Map;

/*
 * Created by Peter Lawrey on 09/05/16.
 */
class WireObjectOutput implements ObjectOutput {
    private final WireOut wire;

    WireObjectOutput(WireOut wire) {
        this.wire = wire;
    }

    @Override
    public void writeObject(Object obj) {
        @NotNull final ValueOut valueOut = wire.getValueOut();
        if (obj instanceof Map)
            valueOut.typePrefix(Map.class);
        else if (obj instanceof List)
            valueOut.typePrefix(List.class);
        valueOut.object(obj);
    }

    @Override
    public void write(int b) {
        wire.getValueOut().uint8(b);
    }

    @Override
    public void write(byte[] b) {
        wire.getValueOut().bytes(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (off == 0 && len == b.length)
            write(b);
        else
            wire.getValueOut().bytes(Bytes.wrapForRead(b).readPositionRemaining(off, len));
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    @Override
    public void writeBoolean(boolean v) {
        wire.getValueOut().bool(v);
    }

    @Override
    public void writeByte(int v) {
        wire.getValueOut().int8(v);
    }

    @Override
    public void writeShort(int v) {
        wire.getValueOut().int16(v);
    }

    @Override
    public void writeChar(int v) {
        wire.getValueOut().uint16(v);
    }

    @Override
    public void writeInt(int v) {
        wire.getValueOut().uint16(v);
    }

    @Override
    public void writeLong(long v) {
        wire.getValueOut().int64(v);
    }

    @Override
    public void writeFloat(float v) {
        wire.getValueOut().float32(v);
    }

    @Override
    public void writeDouble(double v) {
        wire.getValueOut().float64(v);
    }

    @Override
    public void writeBytes(String s) {
        wire.getValueOut().text(s);
    }

    @Override
    public void writeChars(String s) {
        wire.getValueOut().text(s);
    }

    @Override
    public void writeUTF(String s) {
        wire.getValueOut().text(s);
    }
}
