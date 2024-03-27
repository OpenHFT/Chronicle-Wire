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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;

import java.io.ObjectOutput;
import java.util.List;
import java.util.Map;

/**
 * This is the WireObjectOutput class that implements the ObjectOutput interface.
 * It is designed to write objects and data to a WireOut instance. The class provides
 * methods to write an object, bytes, and other basic data types, ensuring that the data is
 * written correctly to the underlying wire instance.
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
        // Do nothing
    }

    @Override
    public void close() {
        // Do nothing
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
    public void writeBytes(@NotNull String s) {
        wire.getValueOut().text(s);
    }

    @Override
    public void writeChars(@NotNull String s) {
        wire.getValueOut().text(s);
    }

    @Override
    public void writeUTF(@NotNull String s) {
        wire.getValueOut().text(s);
    }
}
