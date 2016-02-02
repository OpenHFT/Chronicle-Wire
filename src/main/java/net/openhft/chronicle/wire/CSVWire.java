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

import net.openhft.chronicle.bytes.AppendableUtil;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.StopCharTester;
import net.openhft.chronicle.bytes.StopCharTesters;
import net.openhft.chronicle.core.io.IOTools;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * YAML Based wire format
 */
public class CSVWire extends TextWire {

    static final ThreadLocal<StopCharTester> ESCAPED_END_OF_TEXT = ThreadLocal.withInitial(() -> StopCharTesters.COMMA_STOP.escaping());

    private final List<String> header = new ArrayList<>();

    public CSVWire(Bytes bytes, boolean use8bit) {
        super(bytes, use8bit);
        while (lineStart == 0)
            header.add(valueIn.text());
    }

    public CSVWire(Bytes bytes) {
        this(bytes, false);
    }

    public static CSVWire fromFile(String name) throws IOException {
        return new CSVWire(Bytes.wrapForRead(IOTools.readFile(name)), true);
    }

    @NotNull
    public static CSVWire from(@NotNull String text) {
        return new CSVWire(Bytes.from(text));
    }

    @NotNull
    @Override
    protected TextValueOut createValueOut() {
        return new CSVValueOut();
    }

    @NotNull
    @Override
    protected TextValueIn createValueIn() {
        return new CSVValueIn();
    }

    @NotNull
    public StringBuilder readField(@NotNull StringBuilder sb) {
        valueIn.text(sb);
        return sb;
    }

    @NotNull
    StopCharTester getEscapingCSVEndOfText() {
        StopCharTester escaping = ESCAPED_END_OF_TEXT.get();
        // reset it.
        escaping.isStopChar(' ');
        return escaping;
    }

    StopCharTester getEscapingSingleQuotes() {
        StopCharTester sct = ESCAPED_SINGLE_QUOTES.get();
        // reset it.
        sct.isStopChar(' ');
        return sct;
    }

    protected void consumeWhiteSpace() {
        for (; ; ) {
            int codePoint = peekCode();
            if (Character.isWhitespace(codePoint) || codePoint == ',') {
                if (codePoint == '\n' || codePoint == '\r')
                    this.lineStart = bytes.readPosition() + 1;
                bytes.readSkip(1);
            } else {
                break;
            }
        }
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        return valueIn;
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        consumeWhiteSpace();
        readField(name);
        return valueIn;
    }

    @NotNull
    @Override
    public Wire readComment(@NotNull StringBuilder s) {
        s.setLength(0);
        return this;
    }

    @Override
    public boolean hasMore() {
        consumeWhiteSpace();

        return bytes.readRemaining() > 0;
    }

    class CSVValueOut extends TextValueOut {

    }

    class CSVValueIn extends TextValueIn {

        @Nullable
        <ACS extends Appendable & CharSequence> ACS textTo0(@NotNull ACS a) {
            consumeWhiteSpace();
            int ch = peekCode();

            switch (ch) {
                case '"': {
                    bytes.readSkip(1);
                    if (use8bit)
                        bytes.parse8bit(a, getEscapingQuotes());
                    else
                        bytes.parseUtf8(a, getEscapingQuotes());
                    unescape(a);
                    int code = peekCode();
                    if (code == '"')
                        readCode();
                    break;

                }
                case '\'': {
                    bytes.readSkip(1);
                    if (use8bit)
                        bytes.parse8bit(a, getEscapingSingleQuotes());
                    else
                        bytes.parseUtf8(a, getEscapingSingleQuotes());
                    unescape(a);
                    int code = peekCode();
                    if (code == '\'')
                        readCode();
                    break;

                }
                default: {
                    if (bytes.readRemaining() > 0) {
                        if (a instanceof Bytes || use8bit)
                            bytes.parse8bit(a, getEscapingCSVEndOfText());
                        else
                            bytes.parseUtf8(a, getEscapingCSVEndOfText());

                    } else {
                        AppendableUtil.setLength(a, 0);
                    }
                    // trim trailing spaces.
                    while (a.length() > 0)
                        if (Character.isWhitespace(a.charAt(a.length() - 1)))
                            AppendableUtil.setLength(a, a.length() - 1);
                        else
                            break;
                    break;
                }
            }

            int prev = peekBack();
            if (prev == ':' || prev == '#' || prev == '}')
                bytes.readSkip(-1);
            return a;
        }

        protected long readLengthMarshallable() {
            long start = bytes.readPosition();
            try {
                consumeWhiteSpace();
                for (; ; ) {
                    int code = readCode();
                    switch (code) {
                        case '\r':
                        case '\n':
                        case 0:
                        case -1:
                            return bytes.readPosition() - start - 1;
                    }
                }
            } finally {
                bytes.readPosition(start);
            }
        }

        @Override
        public boolean hasNextSequenceItem() {
            consumeWhiteSpace();
            int ch = peekCode();
            if (ch == ',') {
                bytes.readSkip(1);
                return true;
            }
            return ch > 0 && ch != ']';
        }

        @NotNull
        @Override
        public WireIn marshallable(@NotNull ReadMarshallable object) {
            pushState();
            consumeWhiteSpace();
            final long len = readLengthMarshallable();

            final long limit = bytes.readLimit();
            final long position = bytes.readPosition();

            final long newLimit = position + len;
            try {
                // ensure that you can read past the end of this marshable object

                bytes.readLimit(newLimit);
                consumeWhiteSpace();
                object.readMarshallable(CSVWire.this);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(newLimit);
                popState();
            }

            consumeWhiteSpace();
            return CSVWire.this;
        }

    }
}
