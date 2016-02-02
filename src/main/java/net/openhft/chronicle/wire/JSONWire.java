/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import org.jetbrains.annotations.NotNull;

import static net.openhft.chronicle.bytes.Bytes.empty;
import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;

/**
 * JSON wire format
 * <p/>
 * At the moment, this is a cut down version of the YAML wire format.
 */
public class JSONWire extends TextWire {
    static final BytesStore COMMA = BytesStore.from(",");

    public JSONWire(Bytes bytes, boolean use8bit) {
        super(bytes, use8bit);
    }

    public JSONWire(Bytes bytes) {
        this(bytes, false);
    }

    @NotNull
    public static JSONWire from(@NotNull String text) {
        return new JSONWire(Bytes.from(text));
    }

    public static String asText(@NotNull Wire wire) {
        long pos = wire.bytes().readPosition();
        JSONWire tw = new JSONWire(nativeBytes());
        wire.copyTo(tw);
        wire.bytes().readPosition(pos);

        return tw.toString();
    }


    @NotNull
    @Override
    protected TextValueOut createValueOut() {
        return new JSONValueOut();
    }

    @NotNull
    @Override
    protected TextValueIn createValueIn() {
        return new JSONValueIn();
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        throw new UnsupportedOperationException();
    }

    void escape(@NotNull CharSequence s) {
        bytes.writeUnsignedByte('"');
        if (needsQuotes(s) == Quotes.NONE) {
            bytes.appendUtf8(s);
        } else {
            escape0(s, Quotes.DOUBLE);
        }
        bytes.writeUnsignedByte('"');
    }

    class JSONValueOut extends TextValueOut {
        public void elementSeparator() {
            sep = COMMA;
        }

        protected void popState() {
        }

        protected void pushState() {
            leaf = true;
        }

        @Override
        protected void addNewLine(long pos) {

        }

        @Override
        protected void newLine() {
            append(sep);
            sep = empty();
        }

        @Override
        protected void endField() {
            sep = COMMA;
        }

        protected void fieldValueSeperator() {
            bytes.writeUnsignedByte(':');
        }

        public void writeComment(@NotNull CharSequence s) {
        }
    }

    class JSONValueIn extends TextValueIn {
    }
}
