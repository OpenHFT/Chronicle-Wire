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
import net.openhft.chronicle.bytes.ref.*;
import net.openhft.chronicle.bytes.util.Compression;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * YAML Based wire format
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class YamlWire extends YamlWireOut<YamlWire> {
    static final String SEQ_MAP = "!seqmap";
    static final String BINARY_TAG = "!binary";
    static final String DATA_TAG = "!data";

    //for (char ch : "?%&*@`0123456789+- ',#:{}[]|>!\\".toCharArray())
    private final TextValueIn valueIn = createValueIn();
    private final YamlTokeniser yt;
    private final Map<String, Object> anchorValues = new HashMap<>();
    private DefaultValueIn defaultValueIn;
    private WriteDocumentContext writeContext;
    private ReadDocumentContext readContext;

    public YamlWire(@NotNull Bytes<?> bytes, boolean use8bit) {
        super(bytes, use8bit);
        yt = new YamlTokeniser(bytes);
        defaultValueIn = new DefaultValueIn(this);
    }

    public YamlWire(@NotNull Bytes<?> bytes) {
        this(bytes, false);
    }

    @NotNull
    public static YamlWire fromFile(String name) throws IOException {
        return new YamlWire(BytesUtil.readFile(name), true);
    }

    @NotNull
    public static YamlWire from(@NotNull String text) {
        return new YamlWire(Bytes.from(text));
    }

    public static String asText(@NotNull Wire wire) {
        long pos = wire.bytes().readPosition();
        @NotNull Wire tw = Wire.newYamlWireOnHeap();
        wire.copyTo(tw);
        wire.bytes().readPosition(pos);
        return tw.toString();
    }

    // https://yaml.org/spec/1.2.2/#escaped-characters
    private static <ACS extends Appendable & CharSequence> void unescape(@NotNull ACS sb,
                                                                         char blockQuote) {
        int end = 0;
        int length = sb.length();
        boolean skip = false;
        for (int i = 0; i < length; i++) {
            if (skip) {
                skip = false;
                continue;
            }

            char ch = sb.charAt(i);
            if (blockQuote == '\"' && ch == '\\' && i < length - 1) {
                char ch3 = sb.charAt(++i);
                switch (ch3) {
                    case '0':
                        ch = 0;
                        break;
                    case 'a':
                        ch = 7;
                        break;
                    case 'b':
                        ch = '\b';
                        break;
                    case 't':
                        ch = '\t';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'v':
                        ch = 0xB;
                        break;
                    case 'f':
                        ch = 0xC;
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 'e':
                        ch = 0x1B;
                        break;
                    case 'N':
                        ch = 0x85;
                        break;
                    case '_':
                        ch = 0xA0;
                        break;
                    case 'L':
                        ch = 0x2028;
                        break;
                    case 'P':
                        ch = 0x2029;
                        break;
                    case 'x':
                        ch = (char)
                                (Character.getNumericValue(sb.charAt(++i)) * 16 +
                                        Character.getNumericValue(sb.charAt(++i)));
                        break;
                    case 'u':
                        ch = (char)
                                (Character.getNumericValue(sb.charAt(++i)) * 4096 +
                                        Character.getNumericValue(sb.charAt(++i)) * 256 +
                                        Character.getNumericValue(sb.charAt(++i)) * 16 +
                                        Character.getNumericValue(sb.charAt(++i)));
                        break;
                    default:
                        ch = ch3;
                }
            }

            if (blockQuote == '\'' && ch == '\'' && i < length - 1) {
                char ch2 = sb.charAt(i + 1);
                if (ch2 == ch) {
                    skip = true;
                }
            }

            AppendableUtil.setCharAt(sb, end++, ch);
        }
        if (length != sb.length())
            throw new IllegalStateException("Length changed from " + length + " to " + sb.length() + " for " + sb);
        AppendableUtil.setLength(sb, end);
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public boolean hintReadInputOrder() {
        // TODO Fix YamlTextWireTest for false.
        return true;
    }

    @Override
    @NotNull
    public <T> T methodWriter(@NotNull Class<T> tClass, Class... additional) {
        VanillaMethodWriterBuilder<T> builder = new VanillaMethodWriterBuilder<>(tClass,
                WireType.YAML,
                () -> newTextMethodWriterInvocationHandler(tClass));
        for (Class aClass : additional)
            builder.addInterface(aClass);
        builder.marshallableOut(this);
        return builder.build();
    }

    @NotNull
    TextMethodWriterInvocationHandler newTextMethodWriterInvocationHandler(Class... interfaces) {
        for (Class<?> anInterface : interfaces) {
            Comment c = anInterface.getAnnotation(Comment.class);
            if (c != null)
                writeComment(c.value());
        }
        return new TextMethodWriterInvocationHandler(interfaces[0], this);
    }

    @Override
    @NotNull
    public <T> MethodWriterBuilder<T> methodWriterBuilder(@NotNull Class<T> tClass) {
        VanillaMethodWriterBuilder<T> builder = new VanillaMethodWriterBuilder<>(tClass,
                WireType.YAML,
                () -> newTextMethodWriterInvocationHandler(tClass));
        builder.marshallableOut(this);
        return builder;
    }

    @Override
    public @NotNull VanillaMethodReaderBuilder methodReaderBuilder() {
        return super.methodReaderBuilder().wireType(WireType.YAML);
    }

    @NotNull
    @Override
    public DocumentContext writingDocument(boolean metaData) {
        if (writeContext == null)
            useBinaryDocuments();
        writeContext.start(metaData);
        return writeContext;
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) {
        if (writeContext != null && writeContext.isOpen())
            return writeContext;
        return writingDocument(metaData);
    }

    @NotNull
    @Override
    public DocumentContext readingDocument() {
        initReadContext();
        return readContext;
    }

    protected void initReadContext() {
        if (readContext == null)
            useBinaryDocuments();
        readContext.start();
    }

    @NotNull
    public YamlWire useBinaryDocuments() {
        readContext = new BinaryReadDocumentContext(this, false);
        writeContext = new BinaryWriteDocumentContext(this);
        return this;
    }

    @NotNull
    public YamlWire useTextDocuments() {
        readContext = new TextReadDocumentContext(this);
        writeContext = new TextWriteDocumentContext(this);
        return this;
    }

    @NotNull
    @Override
    public DocumentContext readingDocument(long readLocation) {
        final long readPosition = bytes().readPosition();
        final long readLimit = bytes().readLimit();
        bytes().readPosition(readLocation);
        initReadContext();
        readContext.closeReadLimit(readLimit);
        readContext.closeReadPosition(readPosition);
        return readContext;
    }

    @NotNull
    protected TextValueIn createValueIn() {
        return new TextValueIn();
    }

    public String toString() {
        if (bytes.readRemaining() > (1024 * 1024)) {
            final long l = bytes.readLimit();
            try {
                bytes.readLimit(bytes.readPosition() + (1024 * 1024));
                return bytes + "..";
            } finally {
                bytes.readLimit(l);
            }
        } else
            return bytes.toString();
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        if (wire instanceof TextWire || wire instanceof YamlWire) {
            final Bytes<?> bytes0 = bytes();
            wire.bytes().write(this.bytes, yt.blockStart(), bytes0.readLimit() - yt.blockStart);
            this.bytes.readPosition(this.bytes.readLimit());
        } else {
            while (!isEmpty()) {
                copyOne(wire, true);
                yt.next();
            }
        }
    }

    private void copyOne(WireOut wire, boolean nested) {
        switch (yt.current()) {
            case NONE:
                break;
            case COMMENT:
                wire.writeComment(yt.text());
                break;
            case TAG:
                wire.getValueOut().typePrefix(yt.text());
                yt.next();
                copyOne(wire, true);
                yt.next();
                break;
            case DIRECTIVE:
                break;
            case DOCUMENT_END:
                break;
            case DIRECTIVES_END:
                yt.next();
                while (!isEmpty()) {
                    copyOne(wire, false);
                    yt.next();
                }
                break;
            case MAPPING_KEY:
                copyMappingKey(wire, nested);
                break;
            case MAPPING_END:
                return;
            case MAPPING_START: {
                if (nested) {
                    yt.next();
                    wire.getValueOut().marshallable(w -> {
                        while (yt.current() == YamlToken.MAPPING_KEY) {
                            copyMappingKey(wire, true);
                            yt.next();
                        }
                    });
                }
                break;
            }
            case SEQUENCE_END:
                break;
            case SEQUENCE_ENTRY:
                break;
            case SEQUENCE_START: {
                yt.next();
                YamlWire yw = this;
                wire.getValueOut().sequence(w -> {
                    while (yt.current() != YamlToken.SEQUENCE_END) {
                        yw.copyOne(w.wireOut(), true);
                        yw.yt.next();
                    }
                });
                break;
            }
            case TEXT:
                wire.getValueOut().text(yt.text());
                break;
            case LITERAL:
                wire.getValueOut().text(yt.text());
                break;
            case ANCHOR:
                break;
            case ALIAS:
                break;
            case RESERVED:
                break;
            case STREAM_END:
                break;
            case STREAM_START:
                break;
        }
    }

    private void copyMappingKey(WireOut wire, boolean nested) {
        yt.next();
        if (yt.current() == YamlToken.MAPPING_KEY)
            yt.next();
        if (yt.current() == YamlToken.TEXT) {
            if (nested) {
                wire.write(yt.text());
            } else {
                wire.writeEvent(String.class, yt.text());
            }
            yt.next();
        } else {
            throw new UnsupportedOperationException("Unable to copy key " + yt);
        }
        copyOne(wire, true);
    }

    @Override
    public long readEventNumber() {
        final StringBuilder stringBuilder = acquireStringBuilder();
        readField(stringBuilder);
        try {
            return StringUtils.parseInt(stringBuilder, 10);
        } catch (NumberFormatException ignored) {
            return Long.MIN_VALUE;
        }
    }

    @NotNull
    @Override
    public ValueIn read() {
        readField(acquireStringBuilder());
        switch (yt.current()) {
            case NONE:
            case MAPPING_END:
                return defaultValueIn;
            default:
                return valueIn;
        }
    }

    @NotNull
    private StringBuilder acquireStringBuilder() {
        StringUtils.setCount(sb, 0);
        return sb;
    }

    @NotNull
    protected StringBuilder readField(@NotNull StringBuilder sb) {
        startEventIfTop();
        if (yt.current() == YamlToken.MAPPING_KEY) {
            yt.next();
            if (yt.current() == YamlToken.TEXT) {
                String text = yt.text(); // May use sb so we need to reset it after
                sb.setLength(0);
                sb.append(text);
                unescape(sb, yt.blockQuote());
                yt.next();
            } else {
                throw new IllegalStateException(yt.toString());
            }
        } else {
            sb.setLength(0);
        }
        return sb;
    }

    @Nullable
    @Override
    public <K> K readEvent(@NotNull Class<K> expectedClass) {
        startEventIfTop();
        switch (yt.current()) {
            case MAPPING_KEY:
                YamlToken next = yt.next();
                if (next == YamlToken.MAPPING_KEY) {
                    return readEvent(expectedClass);
                }

                return valueIn.object(expectedClass);
            case NONE:
                return null;
        }
        throw new UnsupportedOperationException(yt.toString());
    }

    @Override
    public boolean isNotEmptyAfterPadding() {
        consumePadding();
        switch (yt.current()) {
            case MAPPING_END:
            case DOCUMENT_END:
            case SEQUENCE_END:
            case NONE:
                return false;
            default:
                return true;
        }
    }

    @Nullable
    private <K> K toExpected(Class<K> expectedClass, StringBuilder sb) {
        return ObjectUtils.convertTo(expectedClass, WireInternal.INTERNER.intern(sb));
    }

    @Override
    public void consumePadding() {
        while (true) {
            switch (yt.current()) {
                case COMMENT:
                    commentListener.accept(yt.text());
                    // fall through
                case DIRECTIVE:
                case DIRECTIVES_END:
                    yt.next();
                    break;
                default:
                    return;
            }
        }
    }

    @Override
    @NotNull
    public String readingPeekYaml() {
        return "todo";
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        return read(key.name().toString());
    }

    @NotNull
    @Override
    public ValueIn read(String keyName) {
        startEventIfTop();

        // check the keys we have already seen first.
        YamlKeys keys = yt.keys();
        int count = keys.count();
        if (count > 0) {
            long pos = yt.lastKeyPosition();
            long[] offsets = keys.offsets();
            int contextSize = yt.contextSize();
            for (int i = 0; i < count; i++) {
                yt.revertToContext(contextSize);
                YamlToken next = yt.rereadAndNext(offsets[i]);
                assert next == YamlToken.MAPPING_KEY;
                if (checkForMatch(keyName)) {
                    keys.removeIndex(i);
                    return valueIn;
                }
            }
            yt.revertToContext(contextSize);
            bytes.readPosition(pos);
            yt.next();
        }

        int minIndent = yt.topContext().indent;
        // go through remaining keys
        while (yt.current() == YamlToken.MAPPING_KEY) {
            long lastKeyPosition = yt.lastKeyPosition();
            if (checkForMatch(keyName))
                return valueIn;

            keys.push(lastKeyPosition);
            valueIn.consumeAny(minIndent);
        }

        return defaultValueIn;
    }

    public String dumpContext() {
        Wire yw = Wire.newYamlWireOnHeap();
        yw.getValueOut().list(yt.contexts, YamlTokeniser.YTContext.class);
        return yw.toString();
    }

    private boolean checkForMatch(@NotNull String keyName) {
        YamlToken next = yt.next();

        if (next == YamlToken.TEXT) {
            sb.setLength(0);
            sb.append(yt.text());
            unescape(sb, yt.blockQuote());
            yt.next();
        } else {
            throw new IllegalStateException(next.toString());
        }

        return (sb.length() == 0 || StringUtils.isEqual(sb, keyName));
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        startEventIfTop();
        readField(name);
        return valueIn;
    }

    @NotNull
    @Override
    public ValueIn getValueIn() {
        return valueIn;
    }

    @NotNull
    @Override
    public Wire readComment(@NotNull StringBuilder s) {
        sb.setLength(0);
        if (yt.current() == YamlToken.COMMENT) {
            // Skip the initial '#'
            YamlToken next = yt.next();
            sb.append(yt.text());
        }
        return this;
    }

    @Override
    public void clear() {
        reset();
    }

    protected void consumeDocumentStart() {
        if (bytes.readRemaining() > 4) {
            long pos = bytes.readPosition();
            if (bytes.readByte(pos) == '-' && bytes.readByte(pos + 1) == '-' && bytes.readByte(pos + 2) == '-')
                bytes.readSkip(3);

            pos = bytes.readPosition();
            @NotNull String word = bytes.parseUtf8(StopCharTesters.SPACE_STOP);
            switch (word) {
                case "!!data":
                case "!!data-not-ready":
                case "!!meta-data":
                case "!!meta-data-not-ready":
                    break;
                default:
                    bytes.readPosition(pos);
            }
        }

        if (yt.current() == YamlToken.NONE)
            yt.next();
    }

    @NotNull
    @Override
    public LongValue newLongReference() {
        return new TextLongReference();
    }

    @NotNull
    @Override
    public BooleanValue newBooleanReference() {
        return new TextBooleanReference();
    }

    @Override
    public boolean useSelfDescribingMessage(@NotNull CommonMarshallable object) {
        return true;
    }

    @NotNull
    @Override
    public IntValue newIntReference() {
        return new TextIntReference();
    }

    @NotNull
    @Override
    public LongArrayValues newLongArrayReference() {
        return new TextLongArrayReference();
    }

    @Override
    public @NotNull IntArrayValues newIntArrayReference() {
        return new TextIntArrayReference();
    }

    @NotNull
    private Map readMap(Class valueType) {
        Map map = new LinkedHashMap();
        if (yt.current() == YamlToken.MAPPING_START) {
            while (yt.next() == YamlToken.MAPPING_KEY) {
                if (yt.next() == YamlToken.TEXT) {
                    String key = yt.text();
                    Object o;
                    if (yt.next() == YamlToken.TEXT) {
                        o = ObjectUtils.convertTo(valueType, yt.text());
                    } else {
                        throw new UnsupportedOperationException(yt.toString());
                    }
                    map.put(key, o);
                } else {
                    throw new UnsupportedOperationException(yt.toString());
                }
            }
        } else {
            throw new UnsupportedOperationException(yt.toString());
        }
        return map;
    }

    @Override
    public void startEvent() {
        consumePadding();
        switch (yt.current()) {
            case MAPPING_START:
                yt.next(Integer.MAX_VALUE);
                return;
            case NONE:
                return;
        }
        throw new UnsupportedOperationException(yt.toString());
    }

    void startEventIfTop() {
        consumePadding();
        if (yt.contextSize() == 3)
            if (yt.current() == YamlToken.MAPPING_START)
                yt.next();
    }

    @Override
    public boolean isEndEvent() {
        consumePadding();
        YamlToken current = yt.current();
        return current == YamlToken.MAPPING_END
                || current == YamlToken.NONE;
    }

    @Override
    public void endEvent() {
        int minIndent = yt.topContext().indent;

        switch (yt.current()) {
            case MAPPING_END:
            case DOCUMENT_END:
            case NONE:
                break;
            default:
                valueIn.consumeAny(minIndent);
                break;
        }
        if (yt.current() == YamlToken.NONE) {
            yt.next(Integer.MIN_VALUE);
        } else {
            while (yt.current() == YamlToken.MAPPING_KEY) {
                yt.next();
                valueIn.consumeAny(minIndent);
            }
        }
        if (yt.current() == YamlToken.MAPPING_END || yt.current() == YamlToken.DOCUMENT_END || yt.current() == YamlToken.NONE) {
            yt.next(Integer.MIN_VALUE);
            return;
        }
        throw new UnsupportedOperationException(yt.toString());
    }

    public void reset() {
        if (readContext != null)
            readContext.reset();
        if (writeContext != null)
            writeContext.reset();
        bytes.clear();
        sb.setLength(0);
        yt.reset();
        valueIn.resetState();
        valueOut.resetState();
        anchorValues.clear();
    }

    @Override
    public boolean hasMetaDataPrefix() {
        if (yt.current() == YamlToken.TAG
                && yt.isText("!meta-data")) {
            yt.next();
            return true;
        }
        return false;
    }

    @Override
    public boolean readDocument(@Nullable ReadMarshallable metaDataConsumer, @Nullable ReadMarshallable dataConsumer) {
        valueIn.resetState();
        return super.readDocument(metaDataConsumer, dataConsumer);
    }

    @Override
    public boolean readDocument(long position, @Nullable ReadMarshallable metaDataConsumer, @Nullable ReadMarshallable dataConsumer) {
        valueIn.resetState();
        return super.readDocument(position, metaDataConsumer, dataConsumer);
    }

    class TextValueIn implements ValueIn {
        @Override
        public ClassLookup classLookup() {
            return YamlWire.this.classLookup();
        }

        @Override
        public void resetState() {
            yt.reset();
            anchorValues.clear();
        }

        @Nullable
        @Override
        public String text() {
            @Nullable CharSequence cs = textTo0(acquireStringBuilder());
            return cs == null ? null : WireInternal.INTERNER.intern(cs);
        }

        @Nullable
        @Override
        public StringBuilder textTo(@NotNull StringBuilder sb) {
            sb.setLength(0);
            @Nullable CharSequence cs = textTo0(sb);
            if (cs == null)
                return null;
            if (cs != sb) {
                sb.setLength(0);
                sb.append(cs);
            }
            return sb;
        }

        @Nullable
        @Override
        public Bytes<?> textTo(@NotNull Bytes<?> bytes) {
            bytes.clear();
            if (yt.current() == YamlToken.TEXT) {
                bytes.clear();
                bytes.append(yt.text());
                yt.next();
            } else {
                throw new UnsupportedOperationException(yt.toString());
            }
            return bytes;
        }

        @Override
        public BracketType getBracketType() {
            switch (yt.current()) {
                default:
                    throw new UnsupportedOperationException(yt.toString());
                case DIRECTIVES_END:
                case TAG:
                case COMMENT:
                    yt.next();
                    return getBracketType();
                case MAPPING_START:
                    return BracketType.MAP;
                case SEQUENCE_START:
                    return BracketType.SEQ;
                case NONE:
                case MAPPING_KEY:
                case SEQUENCE_ENTRY:
                case STREAM_START:
                case TEXT:
                case LITERAL:
                    return BracketType.NONE;
            }
        }

        @Nullable
        Bytes<?> textTo0(@NotNull Bytes<?> a) {
            consumePadding();
            if (yt.current() == YamlToken.TEXT) {
                a.append(yt.text());
            } else {
                throw new UnsupportedOperationException(yt.toString());
            }
            return a;
        }

        @Nullable
        StringBuilder textTo0(@NotNull StringBuilder a) {
            consumePadding();
            if (yt.current() == YamlToken.SEQUENCE_ENTRY)
                yt.next();
            if (yt.current() == YamlToken.TEXT || yt.current() == YamlToken.LITERAL) {
                a.append(yt.text());
                if (yt.current() == YamlToken.TEXT)
                    unescape(a, yt.blockQuote());
                yt.next();
            } else if (yt.current() == YamlToken.TAG) {
                if (yt.isText("!null")) {
                    yt.next();
                    yt.next();
                    return null;
                }

                if (yt.isText(BINARY_TAG)) {
                    yt.next();
                    final byte[] arr = (byte[]) decodeBinary(byte[].class);
                    for (byte b : arr) {
                        a.append((char) b);
                    }
                    return a;
                }

                throw new UnsupportedOperationException(yt.toString());
            }
            return a;
        }

        @NotNull
        @Override
        public WireIn bytesMatch(@NotNull BytesStore compareBytes, BooleanConsumer consumer) {
            throw new UnsupportedOperationException(yt.toString());
        }

        @NotNull
        @Override
        public WireIn bytes(@NotNull BytesOut<?> toBytes) {
            toBytes.clear();
            return bytes(b -> toBytes.write((BytesStore) b));
        }

        @Nullable
        @Override
        public WireIn bytesSet(@NotNull PointerBytesStore toBytes) {
            return bytes(bytes -> {
                long capacity = bytes.readRemaining();
                Bytes<Void> bytes2 = Bytes.allocateDirect(capacity);
                bytes2.write((BytesStore) bytes);
                toBytes.set(bytes2.addressForRead(bytes2.start()), capacity);
            });
        }

        @Override
        @NotNull
        public WireIn bytes(@NotNull ReadBytesMarshallable bytesConsumer) {
            consumePadding();
            // TODO needs to be made much more efficient.
            @NotNull StringBuilder sb = acquireStringBuilder();
            if (yt.current() == YamlToken.TAG) {
                bytes.readSkip(1);
                yt.text(sb);
                yt.next();
                if (yt.current() != YamlToken.TEXT)
                    throw new UnsupportedOperationException(yt.toString());
                @Nullable byte[] uncompressed = Compression.uncompress(sb, yt, t -> {
                    @NotNull StringBuilder sb2 = acquireStringBuilder();
                    t.text(sb2);
                    return Base64.getDecoder().decode(sb2.toString());
                });
                if (uncompressed != null) {
                    Bytes<byte[]> bytes = Bytes.wrapForRead(uncompressed);
                    bytesConsumer.readMarshallable(bytes);
                    bytes.releaseLast();

                } else if (StringUtils.isEqual(sb, "!null")) {
                    bytesConsumer.readMarshallable(null);
                    yt.next();

                } else {
                    throw new IORuntimeException("Unsupported type=" + sb);
                }
            } else {
                textTo(sb);
                Bytes<byte[]> bytes = Bytes.wrapForRead(sb.toString().getBytes(ISO_8859_1));
                bytesConsumer.readMarshallable(bytes);
                bytes.releaseLast();
            }
            return YamlWire.this;
        }

        @Override
        public byte @Nullable [] bytes(byte[] using) {
            return (byte[]) objectWithInferredType(using, SerializationStrategies.ANY_OBJECT, byte[].class);

        }

        @NotNull
        @Override
        public WireIn wireIn() {
            return YamlWire.this;
        }

        @Override
        public long readLength() {
            return readLengthMarshallable();
        }

        @NotNull
        @Override
        public WireIn skipValue() {
            consumeAny(yt.topContext().indent);
            return YamlWire.this;
        }

        protected long readLengthMarshallable() {
            long start = bytes.readPosition();
            try {
                consumeAny(yt.topContext().indent);
                return bytes.readPosition() - start;
            } finally {
                bytes.readPosition(start);
            }
        }

        protected void consumeAny(int minIndent) {
            consumePadding();
            int indent2 = Math.max(yt.topContext().indent, minIndent);
            switch (yt.current()) {
                case SEQUENCE_ENTRY:
                case TAG:
                    yt.next(minIndent);
                    consumeAny(minIndent);
                    break;
                case MAPPING_START:
                    consumeMap(indent2);
                    break;
                case SEQUENCE_START:
                    consumeSeq(indent2);
                    break;
                case MAPPING_KEY:
                    yt.next(minIndent);
                    consumeAny(minIndent);
                    if (yt.current() != YamlToken.MAPPING_KEY && yt.current() != YamlToken.MAPPING_END)
                        consumeAny(minIndent);
                    break;
                case SEQUENCE_END:
                    yt.next(minIndent);
                    break;
                case TEXT:
                    yt.next(minIndent);
                    break;
                case MAPPING_END:
                case STREAM_START:
                case DOCUMENT_END:
                case NONE:
                    break;
                default:
                    throw new UnsupportedOperationException(yt.toString());
            }
        }

        private void consumeSeq(int minIndent) {
            assert yt.current() == YamlToken.SEQUENCE_START;
            yt.next(minIndent);
            while (true) {
                switch (yt.current()) {
                    case SEQUENCE_ENTRY:
                        yt.next(minIndent);
                        consumeAny(minIndent);
                        break;

                    case SEQUENCE_END:
                        yt.next(minIndent);
                        return;

                    default:
                        throw new IllegalStateException(yt.toString());
                }
            }
        }

        private void consumeMap(int minIndent) {
            // consume MAPPING_START
            yt.next(minIndent);
            while (yt.current() == YamlToken.MAPPING_KEY) {
                yt.next(minIndent); // consume KEY
                consumeAny(minIndent); // consume the key
                consumeAny(minIndent); // consume the value
            }
            if (yt.current() == YamlToken.NONE)
                yt.next(Integer.MIN_VALUE);
            if (yt.current() == YamlToken.MAPPING_END)
                yt.next(minIndent);
        }

        @NotNull
        @Override
        public <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> tFlag) {
            consumePadding();

            final StringBuilder stringBuilder = acquireStringBuilder();
            if (textTo(stringBuilder) == null) {
                tFlag.accept(t, null);
                return YamlWire.this;
            }

            Boolean flag = stringBuilder.length() == 0 ? null : StringUtils.isEqual(stringBuilder, "true");
            tFlag.accept(t, flag);
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb) {
            consumePadding();
            tb.accept(t, (byte) getALong());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (short) getALong());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (short) getALong());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (int) getALong());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (int) getALong());
            return YamlWire.this;
        }

        long getALong() {
            if (yt.current() == YamlToken.TEXT) {
                long l = yt.parseLong();
                yt.next();
                return l;
            }
            throw new UnsupportedOperationException(yt.toString());
        }

        @NotNull
        @Override
        public <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            consumePadding();
            tl.accept(t, getALong());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            consumePadding();
            tl.accept(t, getALong());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf) {
            consumePadding();
            tf.accept(t, (float) getADouble());
            return YamlWire.this;
        }

        public double getADouble() {
            if (yt.current() == YamlToken.TEXT) {
                double v = yt.parseDouble();
                yt.next();
                return v;
            } else {
                throw new UnsupportedOperationException("yt:" + yt.current());
            }
        }

        @NotNull
        @Override
        public <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td) {
            consumePadding();
            td.accept(t, getADouble());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime) {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            textTo(stringBuilder);
            setLocalTime.accept(t, LocalTime.parse(WireInternal.INTERNER.intern(stringBuilder)));
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime) {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            textTo(stringBuilder);
            tZonedDateTime.accept(t, ZonedDateTime.parse(WireInternal.INTERNER.intern(stringBuilder)));
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate) {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            textTo(stringBuilder);
            tLocalDate.accept(t, LocalDate.parse(WireInternal.INTERNER.intern(stringBuilder)));
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid) {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            textTo(stringBuilder);
            tuuid.accept(t, UUID.fromString(WireInternal.INTERNER.intern(stringBuilder)));
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter) {
            consumePadding();
            if (!(values instanceof TextLongArrayReference)) {
                values = new TextLongArrayReference();
            }
            @NotNull Byteable b = (Byteable) values;
            long length = TextLongArrayReference.peakLength(bytes, bytes.readPosition());
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            setter.accept(t, values);
            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(@NotNull LongValue value) {
            throw new UnsupportedOperationException(yt.toString());
        }

        @NotNull
        @Override
        public WireIn int32(@NotNull IntValue value) {
            throw new UnsupportedOperationException(yt.toString());
        }

        @Override
        public WireIn bool(@NotNull final BooleanValue value) {
            throw new UnsupportedOperationException(yt.toString());
        }

        @NotNull
        @Override
        public <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter) {
            if (!(value instanceof TextLongReference)) {
                setter.accept(t, value = new TextLongReference());
            }
            return int64(value);
        }

        @NotNull
        @Override
        public <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter) {
            throw new UnsupportedOperationException(yt.toString());
        }

        @Override
        public <T> boolean sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader) {
            consumePadding();
            if (isNull()) {
                return false;
            }
            if (yt.current() == YamlToken.SEQUENCE_START) {
                int minIndent = yt.secondTopContext().indent;
                yt.next(Integer.MAX_VALUE);
                tReader.accept(t, YamlWire.this.valueIn);
                if (yt.current() == YamlToken.NONE)
                    yt.next(minIndent);
                if (yt.current() == YamlToken.SEQUENCE_END)
                    yt.next(minIndent);

            } else if (yt.current() == YamlToken.TEXT) {
                tReader.accept(t, YamlWire.this.valueIn);

            } else {
                throw new UnsupportedOperationException(yt.toString());
            }
            return true;
        }

        public <T> boolean sequence(List<T> list, @NotNull List<T> buffer, Supplier<T> bufferAdd, Reader reader0) {
            return sequence(list, buffer, bufferAdd);
        }

        @Override
        public <T> boolean sequence(@NotNull List<T> list, @NotNull List<T> buffer, @NotNull Supplier<T> bufferAdd) {
            consumePadding();
            if (isNull()) {
                return false;
            }
            list.clear();
            if (yt.current() == YamlToken.SEQUENCE_START) {
                int minIndent = yt.secondTopContext().indent;
                yt.next(Integer.MAX_VALUE);

                while(hasNextSequenceItem()) {
                    if (buffer.size() <= list.size())
                        buffer.add(bufferAdd.get());
                    Object using = buffer.get(list.size());
                    list.add((T) valueIn.object(using, using.getClass()));
                }

                if (yt.current() == YamlToken.NONE)
                    yt.next(minIndent);
                if (yt.current() == YamlToken.SEQUENCE_END)
                    yt.next(minIndent);

            } else {
                throw new UnsupportedOperationException(yt.toString());
            }
            return true;
        }

        @NotNull
        @Override
        public <T, K> WireIn sequence(@NotNull T t, K kls, @NotNull TriConsumer<T, K, ValueIn> tReader) {
            throw new UnsupportedOperationException(yt.toString());
        }

        @Override
        public boolean hasNext() {
            if (yt.current() == YamlToken.DOCUMENT_END)
                yt.next(Integer.MIN_VALUE);

            consumePadding();

            switch (yt.current()) {
                case SEQUENCE_END:
                case STREAM_END:
                case DOCUMENT_END:
                case MAPPING_END:
                case NONE:
                    return false;
                default:
                    return true;
            }
        }

        @Override
        public boolean hasNextSequenceItem() {
            consumePadding();
            switch (yt.current()) {
                case SEQUENCE_START:
                case TEXT:
                case SEQUENCE_ENTRY:
                    return true;
            }
            return false;
        }

        @Override
        public <T> T applyToMarshallable(@NotNull Function<WireIn, T> marshallableReader) {
            throw new UnsupportedOperationException(yt.toString());
        }

        @NotNull
        @Override
        public <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts) {
            consumePadding();
            if (yt.current() == YamlToken.TAG) {
                ts.accept(t, yt.text());
                yt.next();

            } else {
                ts.accept(t, "java.lang.Object");
            }
            return this;
        }

        @Override
        public Class typePrefix() {
            if (yt.current() != YamlToken.TAG)
                return null;
            final StringBuilder stringBuilder = acquireStringBuilder();
            yt.text(stringBuilder);

            // Do not handle !!binary, do not resolve to BytesStore, do not consume tag
            if (BINARY_TAG.contentEquals(stringBuilder) || DATA_TAG.contains(stringBuilder))
                return null;

            try {
                yt.next();
                return classLookup().forName(stringBuilder);
            } catch (ClassNotFoundRuntimeException e) {
                Jvm.warn().on(getClass(), "Unable to find " + stringBuilder + " " + e);
                return null;
            }
        }

        @Override
        public Object typePrefixOrObject(Class tClass) {
            consumePadding();
            switch (yt.current()) {
                case TAG: {
                    Class<?> type = typePrefix();
                    return type;
                }
                default:
                    return null;
/*

                case MAPPING_START:
                    if (tClass == null || tClass == Object.class || tClass == Map.class) {
                        return readMap();
                    }
                    return marshallable(ObjectUtils.newInstance(tClass), SerializationStrategies.MARSHALLABLE);

                case SEQUENCE_START:
                    if (tClass == null || tClass == Object.class || tClass == List.class)
                        return readList(Object.class);
                    if (tClass == Set.class)
                        return readSet(tClass);
                    break;
                case TEXT:
                    return text();
*/
            }
        }

        @Override
        public boolean isTyped() {
            consumePadding();
            int code = bytes.peekUnsignedByte();
            return code == '!';
        }

        @NotNull
        String stringForCode(int code) {
            return code < 0 ? "Unexpected end of input" : "'" + (char) code + "'";
        }

        @NotNull
        @Override
        public <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer)
                throws IORuntimeException, BufferUnderflowException {
            throw new UnsupportedOperationException(yt.toString());
        }

        @Override
        public Type typeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler) {
            consumePadding();
            if (yt.current() == YamlToken.TAG) {
                if (yt.text().equals("type")) {
                    if (yt.next() == YamlToken.TEXT) {
                        Class aClass = classLookup().forName(yt.text());
                        yt.next();
                        return aClass;
                    }
                }
            }
            throw new UnsupportedOperationException(yt.toString());
        }

        @Nullable
        @Override
        public Object marshallable(@NotNull Object object, @NotNull SerializationStrategy strategy)
                throws BufferUnderflowException, IORuntimeException {
            if (isNull()) {
                consumeAny(yt.topContext().indent);
                return null;
            }

            consumePadding();
            if (yt.current() == YamlToken.SEQUENCE_ENTRY) {
                yt.next();
                consumePadding();
            }
            switch (yt.current()) {
                case TAG:
                    Class clazz = typePrefix();
                    if (clazz != object.getClass())
                        object = ObjectUtils.newInstance(clazz);
                    return marshallable(object, strategy);

                case SEQUENCE_START:
                    Jvm.warn().on(getClass(), "Expected a {} but was blank for type " + object.getClass());
                    consumeAny(yt.secondTopContext().indent);
                    return object;

                case MAPPING_START:
                    wireIn().startEvent();

                    object = strategy.readUsing(null, object, this, BracketType.MAP);

                    try {
                        wireIn().endEvent();
                    } catch (UnsupportedOperationException uoe) {
                        throw new IORuntimeException("Unterminated { while reading marshallable " +
                                object + ",code='" + yt.current() + "', bytes=" + Bytes.toString(bytes, 1024)
                        );
                    }
                    return object;

                default:
                    break;
            }
            throw new UnsupportedOperationException(yt.toString());
        }

        @NotNull
        public Demarshallable demarshallable(@NotNull Class clazz) {
            consumePadding();
            switch (yt.current()) {
                case TAG:
                    yt.next();
                    break;
                case MAPPING_START:
                    break;
                default:
                    throw new IORuntimeException("Unsupported type " + yt.current());
            }

            final long len = readLengthMarshallable();

            final long limit = bytes.readLimit();
            final long position = bytes.readPosition();

            final long newLimit = position - 1 + len;
            Demarshallable object;
            try {
                // ensure that you can read past the end of this marshable object

                bytes.readLimit(newLimit);
                bytes.readSkip(1); // skip the {
                consumePadding();

                object = Demarshallable.newInstance(clazz, YamlWire.this);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(newLimit);
            }

            consumePadding();
            if (yt.current() != YamlToken.MAPPING_END)
                throw new IORuntimeException("Unterminated { while reading marshallable " +
                        object + ",code='" + yt.current() + "', bytes=" + Bytes.toString(bytes, 1024)
                );
            yt.next();
            return object;
        }

        @Override
        @Nullable
        public <T> T typedMarshallable() {
            return (T) objectWithInferredType(null, SerializationStrategies.ANY_NESTED, null);
        }

        @Nullable
        private <K, V> Map<K, V> map(@NotNull final Class<K> kClass,
                                     @NotNull final Class<V> vClass,
                                     @Nullable Map<K, V> usingMap) {
            consumePadding();
            if (usingMap == null)
                usingMap = new LinkedHashMap<>();
            else
                usingMap.clear();

            @NotNull StringBuilder sb = acquireStringBuilder();
            switch (yt.current()) {
                case TAG:
                    return typedMap(kClass, vClass, usingMap, sb);
                case MAPPING_START:
                    return marshallableAsMap(kClass, vClass, usingMap);
                case SEQUENCE_START:
                    return readAllAsMap(kClass, vClass, usingMap);
                default:
                    throw new IORuntimeException("Unexpected code " + yt.current());
            }
        }

        @Nullable
        private <K, V> Map<K, V> typedMap(@NotNull Class<K> kClazz, @NotNull Class<V> vClass, @NotNull Map<K, V> usingMap, @NotNull StringBuilder sb) {
            yt.text(sb);
            yt.next();
            if (("!null").contentEquals(sb)) {
                text();
                return null;

            } else if (SEQ_MAP.contentEquals(sb)) {
                consumePadding();
                if (yt.current() != YamlToken.SEQUENCE_START)
                    throw new IORuntimeException("Unsupported start of sequence : " + yt.current());
                do {
                    marshallable(r -> {
                        @Nullable final K k = r.read(() -> "key")
                                .object(kClazz);
                        @Nullable final V v = r.read(() -> "value")
                                .object(vClass);
                        usingMap.put(k, v);
                    });
                } while (hasNextSequenceItem());
                return usingMap;

            } else {
                throw new IORuntimeException("Unsupported type :" + sb);
            }
        }

        @Override
        public boolean bool() {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            if (textTo(stringBuilder) == null)
                throw new NullPointerException("value is null");

            if (ObjectUtils.isTrue(stringBuilder))
                return true;
            if (ObjectUtils.isFalse(stringBuilder))
                return false;
            Jvm.debug().on(getClass(), "Unable to parse '" + stringBuilder + "' as a boolean flag, assuming false");
            return false;
        }

        @Override
        public byte int8() {
            long l = int64();
            if (l > Byte.MAX_VALUE || l < Byte.MIN_VALUE)
                throw new IllegalStateException("value=" + l + ", is greater or less than Byte.MAX_VALUE/MIN_VALUE");
            return (byte) l;
        }

        @Override
        public short int16() {
            long l = int64();
            if (l > Short.MAX_VALUE || l < Short.MIN_VALUE)
                throw new IllegalStateException("value=" + l + ", is greater or less than Short.MAX_VALUE/MIN_VALUE");
            return (short) l;
        }

        @Override
        public int int32() {
            long l = int64();
            if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE)
                throw new IllegalStateException("value=" + l + ", is greater or less than Integer.MAX_VALUE/MIN_VALUE");
            return (int) l;
        }

        @Override
        public int uint16() {
            long l = int64();
            if (l > Integer.MAX_VALUE || l < 0)
                throw new IllegalStateException("value=" + l + ", is greater or less than Integer" +
                        ".MAX_VALUE/ZERO");
            return (int) l;
        }

        @Override
        public long int64() {
            consumePadding();
            valueIn.skipType();
            if (yt.current() != YamlToken.TEXT) {
                Jvm.warn().on(getClass(), "Unable to read " + valueIn.object() + " as a long.");
                return 0;
            }

            return getALong();
        }

        @Override
        public double float64() {
            consumePadding();
            valueIn.skipType();
            if (yt.current() != YamlToken.TEXT) {
                Jvm.warn().on(getClass(), "Unable to read " + valueIn.object() + " as a long.");
                return 0;
            }
            return getADouble();
        }

        void skipType() {
            consumePadding();
            if (yt.current() == YamlToken.TAG) {
                yt.next();
                consumePadding();
            }
        }

        @Override
        public float float32() {

            double d = float64();
            if ((double) (((float) d)) != d)
                throw new IllegalStateException("value=" + d + " can not be represented as a float");

            return (float) d;
        }

        /**
         * @return true if !!null "", if {@code true} reads the !!null "" up to the next STOP, if
         * {@code false} no  data is read  ( data is only peaked if {@code false} )
         */
        @Override
        public boolean isNull() {
            consumePadding();

            if (yt.current() == YamlToken.TAG && yt.isText("!null")) {
                consumeAny(0);
                return true;
            }

            return false;
        }

        @Override
        public Object objectWithInferredType(Object using, @NotNull SerializationStrategy strategy, Class type) {
            consumePadding();
            if (yt.current() == YamlToken.SEQUENCE_ENTRY)
                yt.next();
            @Nullable Object o = objectWithInferredType0(using, strategy, type);
            consumePadding();
            return o;
        }

        @Nullable
        Object objectWithInferredType0(Object using, @NotNull SerializationStrategy strategy, Class type) {
            boolean bestEffort = type != null;
            if (yt.current() == YamlToken.TAG) {
                Class aClass = typePrefix();
                if (type == null || type == Object.class || type.isInterface())
                    type = aClass;
            }

            switch (yt.current()) {
                case MAPPING_START:
                    if (type != null) {
                        if (type == SortedMap.class && !(using instanceof SortedMap))
                            using = new TreeMap();
                        if (type == Object.class || Map.class.isAssignableFrom(type) || using instanceof Map)
                            return map(Object.class, Object.class, (Map) using);
                    }
                    return valueIn.object(using, type, bestEffort);

                case SEQUENCE_START:
                    return readSequence(type);

                case TEXT:
                case LITERAL:
                    Object o = valueIn.readNumberOrText();
                    return ObjectUtils.convertTo(type, o);

                case ANCHOR:
                    String alias = yt.text();
                    yt.next();
                    o = valueIn.object(using, type);
                    // Overwriting of anchor values is permitted
                    anchorValues.put(alias, o);
                    return o;

                case ALIAS:
                    alias = yt.text();
                    o = anchorValues.get(yt.text());
                    if (o == null)
                        throw new IllegalStateException("Unknown alias " + alias + " with no corresponding anchor");

                    yt.next();
                    return o;

                case NONE:
                    return null;

                case TAG:
                    final StringBuilder stringBuilder = acquireStringBuilder();
                    yt.text(stringBuilder);

                    if (BINARY_TAG.contentEquals(stringBuilder)) {
                        yt.next();
                        return decodeBinary(type);
                    }

                    // Intentional fall-through

                default:
                    throw new UnsupportedOperationException("Cannot determine what to do with " + yt.current());
            }
        }

        @Nullable
        protected Object readNumberOrText() {
            char bq = yt.blockQuote();
            @Nullable String s = text();
            if (yt.current() == YamlToken.LITERAL)
                return s;
            if (s == null
                    || bq != 0
                    || s.length() < 1
                    || s.length() > 40
                    || "0123456789.+-".indexOf(s.charAt(0)) < 0)
                return s;

            String ss = s;
            if (s.indexOf('_') >= 0)
                ss = ss.replace("_", "");

            // YAML octal notation
            if (s.startsWith("0o"))
                ss = "0" + s.substring(2);

            try {
                return Long.decode(ss);
            } catch (NumberFormatException fallback) {
                // fallback
            }
            try {
                return Double.parseDouble(ss);
            } catch (NumberFormatException fallback) {
                // fallback
            }
            try {
                if (s.length() == 7 && s.charAt(1) == ':')
                    return LocalTime.parse('0' + s);
                if (s.length() == 8 && s.charAt(2) == ':')
                    return LocalTime.parse(s);
            } catch (DateTimeParseException fallback) {
                // fallback
            }
            try {
                if (s.length() == 10)
                    return LocalDate.parse(s);
            } catch (DateTimeParseException fallback) {
                // fallback
            }
            try {
                if (s.length() >= 22)
                    return ZonedDateTime.parse(s);
            } catch (DateTimeParseException fallback) {
                // fallback
            }
            return s;
        }

        @NotNull
        private Object readSequence(Class clazz) {
            @NotNull Collection coll =
                    clazz == SortedSet.class ? new TreeSet<>() :
                            clazz == Set.class ? new LinkedHashSet<>() :
                                    new ArrayList<>();
            @Nullable Class componentType = (clazz != null && clazz.isArray() && clazz.getComponentType().isPrimitive())
                    ? clazz.getComponentType() : null;

            readCollection(componentType, coll);
            if (clazz != null && clazz.isArray()) {
                Object o = Array.newInstance(clazz.getComponentType(), coll.size());
                if (clazz.getComponentType().isPrimitive()) {
                    Iterator iter = coll.iterator();
                    for (int i = 0; i < coll.size(); i++)
                        Array.set(o, i, iter.next());
                    return o;
                }
                return coll.toArray((Object[]) o);
            }
            return coll;
        }

        private void readCollection(@Nullable Class clazz, @NotNull Collection list) {
            sequence(list, (l, v) -> {
                while (v.hasNextSequenceItem()) {
                    l.add(v.object(clazz));
                }
            });
        }

        private Object decodeBinary(Class type) {
            Object o = objectWithInferredType(null, SerializationStrategies.ANY_SCALAR, String.class);
            byte[] decoded = Base64.getDecoder().decode(o == null ? "" : o.toString().replaceAll("\\s", ""));

            // Does this logic belong here?
            if (type == null || BytesStore.class.isAssignableFrom(type))
                return BytesStore.wrap(decoded);

            if (type.isArray() && type.getComponentType().equals(Byte.TYPE))
                return decoded;

            // For BitSet, other types may be supported in the future
            try {
                Method valueOf = type.getDeclaredMethod("valueOf", byte[].class);
                Jvm.setAccessible(valueOf);
                return valueOf.invoke(null, decoded);
            } catch (NoSuchMethodException e) {
                // ignored
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }

            throw new UnsupportedOperationException("Cannot determine how to deserialize " + type + " from binary data");
        }

        @Override
        public String toString() {
            return YamlWire.this.toString();
        }
    }

    @Override
    public boolean writingIsComplete() {
        return !writeContext.isNotComplete();
    }
}
