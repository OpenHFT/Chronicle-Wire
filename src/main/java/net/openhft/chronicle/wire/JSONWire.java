/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;

/**
 * JSON wire format
 * <p>
 * At the moment, this is a cut down version of the YAML wire format.
 */
public class JSONWire extends TextWire {
    @SuppressWarnings("rawtypes")
    static final BytesStore COMMA = BytesStore.from(",");

    @SuppressWarnings("rawtypes")
    public JSONWire(@NotNull Bytes bytes, boolean use8bit) {
        super(bytes, use8bit);
        // TODO Make false in x.22 c.f. https://github.com/OpenHFT/Chronicle-Wire/issues/286
        trimFirstCurly(true);
    }

    @SuppressWarnings("rawtypes")
    public JSONWire(@NotNull Bytes bytes) {
        this(bytes, false);
    }

    @NotNull
    public static JSONWire from(@NotNull String text) {
        return new JSONWire(Bytes.from(text));
    }

    public static String asText(@NotNull Wire wire) {
        long pos = wire.bytes().readPosition();
        @NotNull JSONWire tw = new JSONWire(nativeBytes());
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
        return new JSONValueIn() {

            @Override
            public double float64() {
                consumePadding();
                valueIn.skipType();
                switch (peekCode()) {
                    case '[':
                    case '{':
                        Jvm.warn().on(getClass(), "Unable to read " + valueIn.object() + " as a double.");
                        return 0;
                }

                boolean isNull;

                long l = bytes.readLimit();
                try {
                    bytes.readLimit(bytes.readPosition() + 4);
                    isNull = "null".contentEquals(bytes);
                } finally {
                    bytes.readLimit(l);
                }

                if (isNull) {
                    bytes.readSkip("null".length());
                    consumePadding();
                }

                final double v = isNull ? Double.NaN : bytes.parseDouble();
                checkRewind();
                return v;
            }

            @Override
            public void checkRewind() {
                int ch = bytes.readUnsignedByte(bytes.readPosition() - 1);
                if (ch == ':' || ch == '}' || ch == ']')
                    bytes.readSkip(-1);

                    // !='l' to handle 'null' in JSON wire
                else if (ch != 'l' && (ch > 'F' && (ch < 'a' || ch > 'f'))) {
                    throw new IllegalArgumentException("Unexpected character in number '" + (char) ch + '\'');
                }
            }
        };
    }

    @Override
    public ValueOut writeEvent(Class expectedType, Object eventKey) {
        if (eventKey instanceof Number) {
            return writeEventName(eventKey.toString());
        }
        return super.writeEvent(expectedType, eventKey);
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    protected Quotes needsQuotes(@NotNull CharSequence s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"' || ch < ' ')
                return Quotes.DOUBLE;
        }
        return Quotes.NONE;
    }

    @Override
    void escape(@NotNull CharSequence s) {
        bytes.writeUnsignedByte('"');
        if (needsQuotes(s) == Quotes.NONE) {
            bytes.appendUtf8(s);
        } else {
            escape0(s, Quotes.DOUBLE);
        }
        bytes.writeUnsignedByte('"');
    }

    @NotNull
    @Override
    protected StringBuilder readField(@NotNull StringBuilder sb) {
        consumePadding();
        int code = peekCode();
        if (code == '}') {
            sb.setLength(0);
            return sb;
        }
        if (code == '{') {
            if (valueIn.stack.level > 0)
                throw new IORuntimeException("Expected field name, but got { at " + bytes.toDebugString(64));
            valueIn.pushState();
            bytes.readSkip(1);
        }
        return super.readField(sb);
    }

    @Override
    @NotNull
    protected TextStopCharsTesters strictEndOfText() {
        return TextStopCharsTesters.STRICT_END_OF_TEXT_JSON;
    }

    class JSONValueOut extends TextValueOut {
        @NotNull
        @Override
        public String nullOut() {
            return "null";
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@Nullable CharSequence type) {
            return text(type);
        }

        @NotNull
        @Override
        public ValueOut typePrefix(@NotNull CharSequence typeName) {
            return this;
        }

        @Override
        public void elementSeparator() {
            sep = COMMA;
        }

        @Override
        protected void popState() {
        }

        @Override
        protected void pushState() {
            leaf = true;
        }

        @Override
        protected void afterOpen() {
            sep = EMPTY;
        }

        @Override
        protected void afterClose() {

        }

        @Override
        protected void addNewLine(long pos) {
        }

        @Override
        protected void newLine() {
        }

        @Override
        protected void endField() {
            sep = COMMA;
        }

        @Override
        protected void fieldValueSeperator() {
            bytes.writeUnsignedByte(':');
        }

        @Override
        public void writeComment(@NotNull CharSequence s) {
        }

        @Override
        protected String doubleToString(double d) {
            return Double.isNaN(d) ? "null" : super.doubleToString(d);
        }

        @Override
        protected String floatToString(float f) {
            return Float.isNaN(f) ? "null" : super.floatToString(f);
        }

        @NotNull
        @Override
        public WireOut rawText(CharSequence value) {
            bytes.writeByte((byte) '\"');
            WireOut wireOut = super.rawText(value);
            bytes.writeByte((byte) '\"');
            return wireOut;
        }

        @Override
        public @NotNull WireOut date(LocalDate localDate) {
            return text(localDate.toString());
        }

        @Override
        public @NotNull WireOut dateTime(LocalDateTime localDateTime) {
            return text(localDateTime.toString());
        }
    }

    class JSONValueIn extends TextValueIn {
        @Override
        public String text() {
            @Nullable String text = super.text();
            return text == null || text.equals("null") ? null : text;
        }

        @Override
        protected boolean isASeparator(int nextChar) {
            return true;
        }
    }
}
