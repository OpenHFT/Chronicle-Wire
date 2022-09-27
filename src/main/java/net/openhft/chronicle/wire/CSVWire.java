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

import net.openhft.chronicle.bytes.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * YAML Based wire format
 */
public class CSVWire extends TextWire {

    private static final ThreadLocal<StopCharTester> ESCAPED_END_OF_TEXT = ThreadLocal.withInitial(
            StopCharTesters.COMMA_STOP::escaping);

    private final List<String> header = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    public CSVWire(@NotNull Bytes<?> bytes, boolean use8bit) {
        super(bytes, use8bit);
        while (lineStart == 0) {
            long start = bytes.readPosition();
            header.add(valueIn.text());
            if (bytes.readPosition() == start)
                break;
        }
    }

    @SuppressWarnings("rawtypes")
    public CSVWire(@NotNull Bytes<?> bytes) {
        this(bytes, false);
    }

    @NotNull
    public static CSVWire fromFile(String name) throws IOException {
        return new CSVWire(BytesUtil.readFile(name), true);
    }

    @NotNull
    public static CSVWire from(@NotNull String text) {
        return new CSVWire(Bytes.from(text));
    }

    @NotNull
    static StopCharTester getEscapingCSVEndOfText() {
        StopCharTester escaping = ESCAPED_END_OF_TEXT.get();
        // reset it.
        escaping.isStopChar(' ');
        return escaping;
    }

    @NotNull
    @Override
    protected CSVValueOut createValueOut() {
        return new CSVValueOut();
    }

    @NotNull
    @Override
    protected TextValueIn createValueIn() {
        return new CSVValueIn();
    }

    @Override
    @NotNull
    public StringBuilder readField(@NotNull StringBuilder sb) {
        valueIn.text(sb);
        return sb;
    }

    public void consumePaddingStart() {
        for (; ; ) {
            int codePoint = peekCode();
            if (codePoint == '#') {
                while (readCode() >= ' ') ;
                continue;
            }
            if (Character.isWhitespace(codePoint)) {
                if (codePoint == '\n' || codePoint == '\r')
                    this.lineStart = bytes.readPosition() + 1;
                bytes.readSkip(1);
            } else {
                break;
            }
        }
    }

    @Override
    public void consumePadding() {
        for (; ; ) {
            int codePoint = peekCode();
            if (Character.isWhitespace(codePoint) && codePoint >= ' ') {
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
        consumePadding();
        readField(name);
        return valueIn;
    }

    @NotNull
    @Override
    public Wire readComment(@NotNull StringBuilder s) {
        s.setLength(0);
        return this;
    }

    class CSVValueOut extends YamlValueOut {
        @NotNull
        @Override
        public CSVWire typeLiteral(@Nullable CharSequence type) {
            if (type == null)
                return (CSVWire) nu11();
            throw new UnsupportedOperationException("Type literals not supported in CSV, cannot write " + type);
        }

        @NotNull
        @Override
        public CSVWire marshallable(@NotNull Serializable object) {
            throw new UnsupportedOperationException("Serializable objects not supported in CSV, cannot write " + object);
        }
    }

    class CSVValueIn extends TextValueIn {

        @Override
        public boolean hasNext() {
            consumePaddingStart();
            return bytes.readRemaining() > 0;
        }

        @Override
        @Nullable <T extends Appendable & CharSequence> T textTo0(@NotNull T a) {
            consumePadding();
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
                    code = peekCode();
                    if (code == ',')
                        readCode();
                    break;

                }
                case '\'': {
                    bytes.readSkip(1);
                    if (use8bit)
                        bytes.parse8bit(a, TextWire.getEscapingSingleQuotes());
                    else
                        bytes.parseUtf8(a, TextWire.getEscapingSingleQuotes());
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
            if (END_CHARS.get(prev))
                bytes.readSkip(-1);
            return a;
        }

        @Override
        protected long readLengthMarshallable() {
            long start = bytes.readPosition();
            try {
                consumePadding();
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
            consumePadding();
            int ch = peekCode();
            if (ch == ',') {
                bytes.readSkip(1);
                return true;
            }
            return ch > 0 && ch != ']';
        }

        @Override
        public boolean marshallable(@NotNull ReadMarshallable object) {
            if (isNull())
                return false;
            pushState();
            final long len = readLengthMarshallable();

            final long limit = bytes.readLimit();
            final long position = bytes.readPosition();

            final long newLimit = position + len;
            try {
                // ensure that you can read past the end of this marshable object

                bytes.readLimit(newLimit);
                consumePadding();
                object.readMarshallable(CSVWire.this);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(newLimit);
                popState();
            }

            consumePadding();
            return true;
        }
    }
}
