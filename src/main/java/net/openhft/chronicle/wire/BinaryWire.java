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
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import net.openhft.chronicle.bytes.ref.*;
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import net.openhft.chronicle.bytes.util.Bit8StringInterner;
import net.openhft.chronicle.bytes.util.Compression;
import net.openhft.chronicle.bytes.util.UTF8StringInterner;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.ValidatableUtil;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.scoped.ScopedResourcePool;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

import static net.openhft.chronicle.core.util.ReadResolvable.readResolve;
import static net.openhft.chronicle.wire.BinaryWire.AnyCodeMatch.ANY_CODE_MATCH;
import static net.openhft.chronicle.wire.BinaryWireCode.*;
import static net.openhft.chronicle.wire.Wires.GENERATE_TUPLES;

/**
 * Represents a binary translation of TextWire, which is a subset of YAML.
 * This class provides functionalities for reading from and writing to binary wire formats. It encapsulates
 * various configurations such as field representation, delta support, and compression settings.
 * Extends the `AbstractWire` and implements the `Wire` interface to ensure compatibility and a common API
 * with other wire formats.
 */
@SuppressWarnings({"rawtypes", "unchecked", "this-escape", "deprecation"})
public class BinaryWire extends AbstractWire implements Wire {

    static final ScopedResourcePool<StringBuilder> SBP = StringBuilderPool.createThreadLocal();

    // UTF-8 string interner for memory-efficient string operations
    private static final UTF8StringInterner UTF8 = new UTF8StringInterner(4096);

    // 8-bit string interner for memory-efficient string operations
    private static final Bit8StringInterner BIT8 = new Bit8StringInterner(1024);

    // Class value mapping to determine whether an object uses self-describing messages
    private static final ClassValue<Boolean> USES_SELF_DESCRIBING = ClassLocal.withInitial(k -> {
        Object m = ObjectUtils.newInstance(k);
        if (m instanceof Marshallable)
            return ((Marshallable) m).usesSelfDescribingMessage();
        return true;
    });

    // Flag to control warnings related to missing classes
    private static final AtomicBoolean FIRST_WARN_MISSING_CLASS = new AtomicBoolean();
    private static final ThreadLocal<VanillaMessageHistory> VANILLA_MESSAGE_HISTORY_TL = ThreadLocal.withInitial(VanillaMessageHistory::new);

    // Output handler for fixed binary values
    private final FixedBinaryValueOut fixedValueOut = new FixedBinaryValueOut();

    // Output handler for binary values
    @NotNull
    private final FixedBinaryValueOut valueOut;

    // Input handler for binary values
    @NotNull
    protected final BinaryValueIn valueIn;

    // Indicates whether fields are represented numerically
    private final boolean numericFields;

    // Indicates whether fields are absent
    private final boolean fieldLess;

    // Threshold size for compressed outputs
    private final int compressedSize;

    // Context for writing to the wire
    private final WriteDocumentContext writeContext = new BinaryWriteDocumentContext(this);

    // Context for reading from the wire
    @NotNull
    private final BinaryReadDocumentContext readContext;

    // String builder for various internal operations
    private final StringBuilder stringBuilder = new StringBuilder();

    // Default input handler
    private DefaultValueIn defaultValueIn;
    private final String compression;
    private Boolean overrideSelfDescribing = null;

    /**
     * Constructs a BinaryWire with default settings.
     *
     * @param bytes The bytes to be processed by this wire
     */
    public BinaryWire(@NotNull Bytes<?> bytes) {
        this(bytes, false, false, false, Integer.MAX_VALUE, "binary", false);
    }

    /**
     * Constructs a BinaryWire with specified configurations.
     *
     * @param bytes The bytes to be processed by this wire
     * @param fixed Indicates whether the value output is fixed
     * @param numericFields Indicates if fields are represented numerically
     * @param fieldLess Indicates if fields are absent
     * @param compressedSize Threshold size for compression
     * @param compression Type of compression (e.g., "binary")
     */
    public BinaryWire(@NotNull Bytes<?> bytes, boolean fixed, boolean numericFields, boolean fieldLess, int compressedSize, String compression) {
        super(bytes, false);
        this.numericFields = numericFields;
        this.fieldLess = fieldLess;
        this.compressedSize = compressedSize;
        valueOut = getFixedBinaryValueOut(fixed);
        this.compression = compression;
        valueIn = new BinaryValueIn();
        readContext = new BinaryReadDocumentContext(this);
    }

    /**
     * Constructs a BinaryWire with specified configurations.
     *
     * @param bytes The bytes to be processed by this wire
     * @param fixed Indicates whether the value output is fixed
     * @param numericFields Indicates if fields are represented numerically
     * @param fieldLess Indicates if fields are absent
     * @param compressedSize Threshold size for compression
     * @param compression Type of compression (e.g., "binary")
     * @param supportDelta must be false
     */
    @Deprecated(/* to be removed in x.29 */)
    public BinaryWire(@NotNull Bytes<?> bytes, boolean fixed, boolean numericFields, boolean fieldLess, int compressedSize, String compression, boolean supportDelta) {
        this(bytes, fixed, numericFields, fieldLess, compressedSize, compression);
        assert !supportDelta;
    }

    /**
     * Creates and returns a new instance of BinaryWire with the delta support disabled.
     *
     * @param bytes The bytes to be processed by this wire
     * @return A new instance of BinaryWire
     */
    @NotNull
    public static BinaryWire binaryOnly(@NotNull Bytes<?> bytes) {
        return new BinaryWire(bytes, false, false, false, Integer.MAX_VALUE, "binary", false);
    }

    /**
     * Determines if the provided BytesStore can be treated as textual.
     * This method checks each byte of the BytesStore to ensure it's a printable character or a newline.
     *
     * @param bytes The BytesStore to check
     * @return true if the BytesStore can be treated as text, false otherwise
     */
    static boolean textable(BytesStore<?, ?> bytes) {
        if (bytes == null)
            return false;
        for (long pos = bytes.readPosition(); pos < bytes.readLimit(); pos++) {
            byte b = bytes.readByte(pos);
            if (b < ' ' && b != '\n')
                return false;
        }
        return true;
    }

    /**
     * Determines if the provided CharSequence can be treated as textual.
     * This method checks each character of the CharSequence to ensure it's a printable character.
     *
     * @param cs The CharSequence to check
     * @return true if the CharSequence can be treated as text, false otherwise
     */
    static boolean textable(CharSequence cs) {
        if (cs == null)
            return false;
        for (int pos = 0; pos < cs.length(); pos++) {
            char b = cs.charAt(pos);
            if (b < ' ')
                return false;
        }
        return true;
    }

    /**
     * Checks if the provided character is a digit (0-9).
     *
     * @param c The character to check
     * @return true if the character is a digit, false otherwise
     */
    static boolean isDigit(char c) {
        // use underflow to make digits below '0' large.
        c -= '0';
        return c <= 9;
    }

    @Override
    public void reset() {
        writeContext.reset();
        readContext.reset();
        valueIn.resetState();
        valueOut.resetState();
        bytes.clear();
    }

    @Override
    public void rollbackIfNotComplete() {
        writeContext.rollbackIfNotComplete();
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    /**
     * Retrieves the current override setting for the self-describing nature of this BinaryWire.
     *
     * @return null if there's no override, true if it always uses self-describing messages,
     *         false if it never uses self-describing messages.
     */
    public Boolean getOverrideSelfDescribing() {
        return overrideSelfDescribing;
    }

    /**
     * Sets an override for the self-describing nature of this BinaryWire.
     *
     * @param overrideSelfDescribing null if there's no override, true if it should always use self-describing messages,
     *                               false if it should never use self-describing messages.
     * @return The current instance of the BinaryWire class (following the builder pattern).
     */
    public BinaryWire setOverrideSelfDescribing(Boolean overrideSelfDescribing) {
        this.overrideSelfDescribing = overrideSelfDescribing;
        return this;
    }

    /**
     * Acquires and clears the internal StringBuilder for use. This method is used to avoid frequent
     * instantiation of new StringBuilder objects, improving performance.
     *
     * @return A cleared instance of the internal StringBuilder.
     */
    @NotNull
    protected StringBuilder acquireStringBuilder() {
        // Reset the StringBuilder to its default state.
        stringBuilder.setLength(0);
        return stringBuilder;
    }

    /**
     * Provides a FixedBinaryValueOut instance based on the given boolean parameter. If 'fixed' is true,
     * the method returns a fixed instance; otherwise, it creates and returns a new BinaryValueOut instance.
     *
     * @param fixed Determines which type of FixedBinaryValueOut to return.
     * @return An instance of FixedBinaryValueOut.
     */
    @NotNull
    protected FixedBinaryValueOut getFixedBinaryValueOut(boolean fixed) {
        return fixed ? fixedValueOut : new BinaryValueOut();
    }

    @Override
    public void clear() {
        bytes.clear();
        valueIn.resetState();
        valueOut.resetState();
    }

    /**
     * Checks and returns if this BinaryWire instance is field-less.
     *
     * @return true if the BinaryWire is field-less, false otherwise.
     */
    public boolean fieldLess() {
        return fieldLess;
    }

    @NotNull
    @Override
    public DocumentContext writingDocument(boolean metaData) {
        writeContext.start(metaData);
        return writeContext;
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) {
        if (writeContext.isOpen() && writeContext.chainedElement())
            return writeContext;
        return writingDocument(metaData);
    }

    @NotNull
    @Override
    public DocumentContext readingDocument() {
        readContext.start();
        return readContext;
    }

    @NotNull
    @Override
    public DocumentContext readingDocument(long readLocation) {
        @NotNull Bytes<?> bytes = bytes();
        final long readPosition = bytes.readPosition();
        final long readLimit = bytes.readLimit();
        bytes.readPositionUnlimited(readLocation);

        readContext.start();
        readContext.closeReadLimit(readLimit);
        readContext.closeReadPosition(readPosition);
        return readContext;
    }

    /**
     * Typicality used for debugging, this method does not progress the read position and should
     * only be used when inside a reading document.
     *
     * @return when readReading a document returns the current document as a YAML String, if you are
     * not currently reading a document, and empty string will be return.
     */
    @Override
    @NotNull
    public String readingPeekYaml() {
        long start = readContext.start;
        if (start == -1)
            return "";
        return Wires.fromSizePrefixedBlobs(bytes, start, usePadding());
    }

    @Override
    public void copyTo(@NotNull WireOut wire) throws InvalidMarshallableException {
        if (wire.getClass() == getClass()) {
            final Bytes<?> bytes2 = wire.bytes();
            if (bytes2.retainedHexDumpDescription())
                bytes2.writeHexDumpDescription("passed-through");
            bytes2.write(this.bytes);
            this.bytes.readPosition(this.bytes.readLimit());
            return;
        }

        boolean first = true;
        copyTo(wire, first);
    }

    /**
     * Copies the content of this BinaryWire instance to the provided WireOut instance.
     * It does this by copying each data point sequentially.
     *
     * @param wire The destination WireOut instance.
     * @param first A flag indicating if the data point being copied is the first one.
     */
    void copyTo(@NotNull WireOut wire, boolean first) {
        while (bytes.readRemaining() > 0) {
            copyOne(wire, first);
            first = false; // Subsequent data points aren't the first ones.
        }
    }

    /**
     * Copies one unit of data from this BinaryWire to the provided WireOut instance.
     *
     * @param wire The destination WireOut instance.
     * @throws InvalidMarshallableException if the operation encounters an error during marshalling.
     */
    public void copyOne(@NotNull WireOut wire) throws InvalidMarshallableException {
        copyOne(wire, true);
    }

    /**
     * Copies one unit of data from this BinaryWire to the provided WireOut instance,
     * considering the provided 'first' flag.
     *
     * @param wire The destination WireOut instance.
     * @param first A flag indicating if the data point being copied is the first one.
     * @throws InvalidMarshallableException if the operation encounters an error during marshalling.
     */
    private void copyOne(@NotNull WireOut wire, boolean first) throws InvalidMarshallableException {
        int peekCode = peekCode();
        outerSwitch:
        switch (peekCode >> 4) {
            // For numeric codes, validate and copy accordingly.
            case BinaryWireHighCode.NUM0:
            case BinaryWireHighCode.NUM1:
            case BinaryWireHighCode.NUM2:
            case BinaryWireHighCode.NUM3:
            case BinaryWireHighCode.NUM4:
            case BinaryWireHighCode.NUM5:
            case BinaryWireHighCode.NUM6:
            case BinaryWireHighCode.NUM7:
                if (first)
                    throw new IllegalArgumentException();
                bytes.uncheckedReadSkipOne();
                wire.getValueOut().uint8checked(peekCode);
                break;

            // For control codes, handle padding and lengths.
            case BinaryWireHighCode.CONTROL:
                switch (peekCode) {
                    case PADDING:
                        // Handle padding and skip reading.
                        bytes.uncheckedReadSkipOne();
                        break outerSwitch;
                    case PADDING32:
                        // Handle 32-bit padding and skip reading.
                        bytes.uncheckedReadSkipOne();
                        bytes.readSkip(bytes.readUnsignedInt());
                        break outerSwitch;

                    // Handle byte lengths and read accordingly.
                    case BYTES_LENGTH8: {
                        bytes.uncheckedReadSkipOne();
                        int len = bytes.readUnsignedByte();
                        readWithLength(wire, len);
                        break outerSwitch;
                    }

                    case BYTES_LENGTH16: {
                        bytes.uncheckedReadSkipOne();
                        int len = bytes.readUnsignedShort();
                        readWithLength(wire, len);
                        break outerSwitch;
                    }

                    case BYTES_LENGTH32: {
                        // Handle 32-bit length bytes and read accordingly.
                        bytes.uncheckedReadSkipOne();
                        int len = bytes.readInt();
                        readWithLength(wire, len);
                        break outerSwitch;
                    }

                    case I64_ARRAY:
                        // Handle array of 64-bit integers.
                        bytes.uncheckedReadSkipOne();
                        long len2 = bytes.readLong();
                        long used = bytes.readLong();
                        if (len2 == used && len2 <= 2)
                            wire.getValueOut().sequence(o -> {
                                for (long i = 0; i < len2; i++) {
                                    long v = bytes.readLong();
                                    o.int64(v);
                                }
                            });
                        else
                            wire.getValueOut().sequence(o -> {
                                // Write comments indicating length and usage.
                                wire.writeComment("length: " + len2 + ", used: " + used);
                                for (long i = 0; i < len2; i++) {
                                    long v = bytes.readLong();
                                    if (i == used) {
                                        o.swapLeaf(true);
                                    }
                                    o.int64(v);
                                }
                                o.swapLeaf(false);
                            });

                        break outerSwitch;

                    case FIELD_ANCHOR: { // Process field anchors.
                        fieldAnchor(wire);
                        break outerSwitch;
                    }

                    case ANCHOR:
                    case UPDATED_ALIAS: { // Process anchors and updated aliases.
                        anchor(wire);
                        break outerSwitch;
                    }

                    case HISTORY_MESSAGE: {
                        bytes.uncheckedReadSkipOne();
                        copyHistoryMessage(bytes(), wire);
                        break outerSwitch;
                    }
                    case U8_ARRAY:
                        unexpectedCode();
                }
                unknownCode(wire);
                break;

            case BinaryWireHighCode.FLOAT:
                // Handle floating-point values.
                bytes.uncheckedReadSkipOne();
                try {
                    Number d = readFloat0(peekCode);
                    wire.getValueOut().object(d);
                } catch (Exception e) {
                    unknownCode(wire);
                }
                break;

            case BinaryWireHighCode.INT:
                // Handle integer values.
                bytes.uncheckedReadSkipOne();
                try {
                    if (peekCode == INT64_0x) {
                        wire.getValueOut().int64_0x(bytes.readLong());
                    } else {
                        Number l = readInt0object(peekCode);
                        if (l instanceof Integer)
                            wire.getValueOut().int32(l.intValue());
                        else
                            wire.getValueOut().object(l);
                    }
                } catch (Exception e) {
                    unknownCode(wire);
                }
                break;

            case BinaryWireHighCode.SPECIAL:
                // Process special codes.
                copySpecial(wire, peekCode);
                break;

            case BinaryWireHighCode.FIELD0:
            case BinaryWireHighCode.FIELD1:
                // Process field values.
                @Nullable StringBuilder fsb = readField(peekCode, ANY_CODE_MATCH.name(), ANY_CODE_MATCH.code(), acquireStringBuilder(), false);
                if (!textable(fsb))
                    throw new IllegalArgumentException();
                wire.write(fsb);
                break;

            case BinaryWireHighCode.STR0:
            case BinaryWireHighCode.STR1:
                // Process string values.
                bytes.uncheckedReadSkipOne();
                @Nullable StringBuilder sb = readText(peekCode, acquireStringBuilder());
                wire.getValueOut().text(sb);
                break;
        }
    }

    private static void copyHistoryMessage(Bytes<?> bytes, @NotNull WireOut wire) {
        VanillaMessageHistory vmh = VANILLA_MESSAGE_HISTORY_TL.get();
        vmh.useBytesMarshallable(true);
        vmh.addSourceDetails(false);
        vmh.readMarshallable(bytes);
        wire.getValueOut().object(VanillaMessageHistory.class, vmh);
    }

    /**
     * Throws an exception indicating an unexpected code was encountered.
     *
     * @throws IORuntimeException Always thrown with a specific message.
     */
    protected static void unexpectedCode() {
        throw new IORuntimeException("Unexpected code in this context");
    }

    /**
     * Placeholder or handler for anchor processing in the WireOut stream.
     * This implementation throws an exception indicating it's unexpected in this context.
     *
     * @param wire The wire output stream.
     */
    protected void anchor(@NotNull WireOut wire) {
        unexpectedCode();
    }

    /**
     * Placeholder or handler for field anchor processing in the WireOut stream.
     * This implementation throws an exception indicating it's unexpected in this context.
     *
     * @param wire The wire output stream.
     */
    protected void fieldAnchor(@NotNull WireOut wire) {
        unexpectedCode();
    }

    /**
     * Reads data of a specified length from the bytes stream and writes to the WireOut stream
     * while interpreting the type of data (Map, Sequence, or Object).
     *
     * @param wire The wire output stream to write data to.
     * @param len  The length of data to be read.
     * @throws InvalidMarshallableException If there's an issue during marshalling.
     */
    @SuppressWarnings("incomplete-switch")
    public void readWithLength(@NotNull WireOut wire, int len) throws InvalidMarshallableException {
        long limit = bytes.readLimit();
        long newLimit = bytes.readPosition() + len;
        if (newLimit > limit)
            throw new IORuntimeException("Can't extend the limit");
        try {
            bytes.readLimit(newLimit);
            @NotNull final ValueOut wireValueOut = wire.getValueOut();
            BracketType bracketType = getBracketTypeNext();
            switch (bracketType) {
                case HISTORY_MESSAGE:
                    bytes.uncheckedReadSkipOne();
                    copyHistoryMessage(bytes(), wire);
                    return;
                case MAP:
                    // For MAP type data, use marshallable to process.
                    wireValueOut.marshallable(this::copyTo);
                    break;
                case SEQ:
                    // For SEQ type data, use sequence for processing.
                    wireValueOut.sequence(v -> copyTo(v.wireOut(), false));
                    break;
                case NONE:
                    // For simple or NONE type, just read and process the object.
                    @Nullable Object object = this.getValueIn().object();
                    if (object instanceof BytesStore) {
                        @Nullable BytesStore<?, ?> bytes = (BytesStore) object;
                        if (textable(bytes)) {
                            wireValueOut.text(bytes);
                            bytes.releaseLast();
                            break;
                        }
                    }
                    wireValueOut.object(object);
                    break;
            }
        } finally {
            bytes.readLimit(limit);  // Reset the read limit to its original value.
        }
    }

    /**
     * Throws an exception indicating an unknown code was encountered.
     *
     * @param wire The wire output stream.
     * @throws IllegalArgumentException with the corresponding message for the unknown code.
     */
    protected void unknownCode(@NotNull WireOut wire) {
        throw new IllegalArgumentException(stringForCode(bytes.readUnsignedByte()));
    }

    /**
     * Peeks the next code from the stream and determines its corresponding bracket type.
     *
     * @return BracketType which could be MAP, SEQ, or NONE based on the peeked code.
     */
    @NotNull
    private BracketType getBracketTypeNext() {
        int peekCode = peekCode();
        return getBracketTypeFor(peekCode);
    }

    /**
     * Determines the bracket type for a given code.
     *
     * @param peekCode The code to determine the bracket type for.
     * @return BracketType corresponding to the provided code.
     */
    @NotNull
    BracketType getBracketTypeFor(int peekCode) {
        // If the code indicates a field name, return MAP
        if (peekCode >= FIELD_NAME0 && peekCode <= FIELD_NAME31)
            return BracketType.MAP;
        switch (peekCode) {
            case HISTORY_MESSAGE:
                return BracketType.HISTORY_MESSAGE;
            case FIELD_NUMBER:
            case FIELD_NAME_ANY:
            case EVENT_NAME:
            case EVENT_OBJECT:
                return BracketType.MAP;  // If the code corresponds to field numbers, events or any field name, return MAP

            case U8_ARRAY:
            case I64_ARRAY:
                return BracketType.NONE;  // For certain array types, return NONE

            default:
                return BracketType.SEQ;   // For all other codes, default to SEQ
        }
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull String fieldName) {
        return read(fieldName, fieldName.hashCode(), null, Function.identity());
    }

    @NotNull
    @Override
    public ValueIn read() {
        readField(acquireStringBuilder(), null, ANY_CODE_MATCH.code());
        return bytes.readRemaining() <= 0
                ? acquireDefaultValueIn()
                : valueIn;
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        return read(key.name(), key.code(), key, WireKey::defaultValue);
    }

    /**
     * Attempts to read a field based on a given keyName and keyCode. If the field isn't immediately found, the method
     * will default to the provided source and lookup function. This method manages the parsing position, and may resort
     * to older fields if necessary.
     *
     * @param keyName       The name of the key to be searched for.
     * @param keyCode       The code corresponding to the key.
     * @param defaultSource The source to revert to if the key isn't found.
     * @param defaultLookup The function used to derive a default value based on the defaultSource.
     * @param <T>           The type of the default source.
     * @return Returns a ValueIn object which represents the found or default value.
     */
    private <T> ValueIn read(CharSequence keyName, int keyCode, T defaultSource, @NotNull Function<T, Object> defaultLookup) {
        ValueInState curr = valueIn.curr();
        @NotNull StringBuilder sb = acquireStringBuilder();
        // did we save the position last time
        // so we could go back and parseOne an older field?
        if (curr.savedPosition() > 0) {
            bytes.readPosition(curr.savedPosition() - 1);
            curr.savedPosition(0L);
        }

        // Iterate through remaining bytes to find the field.
        while (bytes.readRemaining() > 0) {
            long position = bytes.readPosition();
            // at the current position look for the field.
            readField(sb, keyName, keyCode);
            if (sb.length() == 0 || StringUtils.isEqual(sb, keyName))
                return valueIn;

            // if no old field nor current field matches, set to default values.
            // we may come back and set the field later if we find it.
            curr.addUnexpected(position);

            ValidatableUtil.startValidateDisabled();
            try {
                valueIn.consumeNext();
            } catch (InvalidMarshallableException e) {
                throw new AssertionError(e);
            } finally {
                ValidatableUtil.endValidateDisabled();
            }
            consumePadding();
        }

        return read2(keyName, keyCode, defaultSource, defaultLookup, curr, sb, keyName);
    }

    /**
     * A secondary method to continue the field search process, specifically to handle cases where the field may
     * have been missed in a previous pass or if the field still hasn't been found. It will revert to the default
     * value if the field cannot be located.
     *
     * @param keyName       The name of the key to be searched for.
     * @param keyCode       The code corresponding to the key.
     * @param defaultSource The source to revert to if the key isn't found.
     * @param defaultLookup The function used to derive a default value based on the defaultSource.
     * @param curr          The current state of ValueIn.
     * @param sb            The StringBuilder used for string manipulations during search.
     * @param name          The actual name of the field being sought.
     * @param <T>           The type of the default source.
     * @return Returns a ValueIn object which represents the found or default value.
     */
    protected <T> ValueIn read2(CharSequence keyName,
                                int keyCode,
                                T defaultSource,
                                @NotNull Function<T, Object> defaultLookup,
                                @NotNull ValueInState curr,
                                @NotNull StringBuilder sb,
                                CharSequence name) {
        long position2 = bytes.readLimit();

        // if not a match go back and look at old fields.
        for (int i = 0; i < curr.unexpectedSize(); i++) {
            bytes.readPosition(curr.unexpected(i));
            readField(sb, keyName, keyCode);
            if (sb.length() == 0 || StringUtils.isEqual(sb, name)) {
                // if an old field matches, remove it, save the current position
                curr.removeUnexpected(i);
                curr.savedPosition(position2 + 1);
                return valueIn;
            }
        }
        bytes.readPosition(position2);

        acquireDefaultValueIn();
        defaultValueIn.defaultValue = defaultLookup.apply(defaultSource);
        return defaultValueIn;
    }

    /**
     * Acquires an instance of DefaultValueIn. If one doesn't exist, a new instance is created.
     * Resets the default value to null each time it's acquired.
     *
     * @return The acquired or newly created DefaultValueIn instance.
     */
    private DefaultValueIn acquireDefaultValueIn() {
        if (defaultValueIn == null)
            defaultValueIn = new DefaultValueIn(this);
        // Reset the default value to null.
        defaultValueIn.defaultValue = null;
        return defaultValueIn;
    }

    @Override
    public long readEventNumber() {
        int peekCode = peekCodeAfterPadding();
        if (peekCode == BinaryWireCode.FIELD_NUMBER) {
            bytes.uncheckedReadSkipOne();
            int peekCode2 = bytes.peekUnsignedByte();
            if (0 <= peekCode2 && peekCode2 < 128) {
                bytes.uncheckedReadSkipOne();
                return peekCode2;
            }
            return bytes.readStopBit();
        }
        return Long.MIN_VALUE;
    }

    @NotNull
    @Override
    public ValueIn readEventName(@NotNull StringBuilder name) {
        return readField(name, null, ANY_CODE_MATCH.code()) == null ? acquireDefaultValueIn() : valueIn;
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        return readField(name, null, ANY_CODE_MATCH.code()) == null ? acquireDefaultValueIn() : valueIn;
    }

    @NotNull
    @Override
    public ValueIn getValueIn() {
        return valueIn;
    }

    @NotNull
    @Override
    public Wire readComment(@NotNull StringBuilder s) {
        if (peekCode() == COMMENT) {
            bytes.uncheckedReadSkipOne();
            bytes.readUtf8(s);
        } else {
            s.setLength(0);
        }
        return this;
    }

    /**
     * Reads the field value into a StringBuilder. It uses the provided key name and key code for the operation.
     *
     * @param name     The StringBuilder where the field name will be stored.
     * @param keyName  The name of the key to read.
     * @param keyCode  The code corresponding to the key.
     * @return The updated StringBuilder containing the field name.
     */
    @Nullable
    private StringBuilder readField(@NotNull StringBuilder name, CharSequence keyName, int keyCode) {
        // Peek at the next code after any padding or comment bytes.
        int peekCode = peekCodeAfterPadding();

        // Continue reading the field.
        return readField(peekCode, keyName, keyCode, name, true);
    }

    /**
     * Peeks at the next code after any padding or comments. If padding or comments are encountered,
     * they are consumed and the method looks again for the next meaningful code.
     *
     * @return The code that appears after any padding or comments.
     */
    private int peekCodeAfterPadding() {
        // Peek at the next available code.
        int peekCode = peekCode();

        // If the code corresponds to padding or a comment, consume it and peek again.
        if (peekCode == PADDING || peekCode == PADDING32 || peekCode == COMMENT) {
            consumePadding();
            peekCode = peekCode();
        }
        return peekCode;
    }

    @Nullable
    @Override
    public <K> K readEvent(@NotNull Class<K> expectedClass) throws InvalidMarshallableException {
        int peekCode = peekCodeAfterPadding();
        switch (peekCode >> 4) {
            case BinaryWireHighCode.END_OF_STREAM:
                return null;

            case BinaryWireHighCode.CONTROL:
            case BinaryWireHighCode.SPECIAL:
                return readSpecialField(peekCode, expectedClass);

            case BinaryWireHighCode.FIELD0:
            case BinaryWireHighCode.FIELD1:
                return readSmallField(peekCode, expectedClass);

            default:
                return null;
        }
    }

    /**
     * Reads a small field based on the given peek code and converts it to the expected class type.
     * This method is optimized for fields with a length up to 31 characters.
     *
     * @param peekCode      The peek code used to determine the length and type of the field.
     * @param expectedClass The class type to which the read value should be converted.
     * @param <K>           The type of the returned value.
     * @return The read value converted to the expected class type.
     */
    @NotNull
    private <K> K readSmallField(int peekCode, Class<K> expectedClass) {
        // Skip the peek code as it's already read.
        bytes.uncheckedReadSkipOne();
        // Extract the length from the lower 5 bits of the peek code.
        final int length = peekCode & 0x1F;
        // Read and intern the field string.
        final String s = BIT8.intern(bytes, length);
        // Move the read position after the field.
        bytes.readSkip(length);
        if (expectedClass == String.class)
            // Return the interned string if the expected class is String.
            return (K) WireInternal.INTERNER.intern(s);
        // Otherwise, convert the string to the expected class type.
        return ObjectUtils.convertTo(expectedClass, s);
    }

    /**
     * Reads a special field based on the given peek code and converts it to the expected class type.
     * This method handles special cases like field numbers, field names, events, and anchors.
     *
     * @param peekCode      The peek code representing the type of the field.
     * @param expectedClass The class type to which the read value should be converted.
     * @param <K>           The type of the returned value.
     * @return The read value converted to the expected class type or null if the peek code doesn't match any known type.
     * @throws InvalidMarshallableException If there's an error during the marshalling process.
     */
    @Nullable
    private <K> K readSpecialField(int peekCode, @NotNull Class<K> expectedClass) throws InvalidMarshallableException {
        switch (peekCode) {
            case FIELD_NUMBER:
                // Skip the peek code and read the field number.
                bytes.uncheckedReadSkipOne();
                long fieldId = bytes.readStopBit();
                return ObjectUtils.convertTo(expectedClass, fieldId);

            case FIELD_NAME_ANY:
            case EVENT_NAME: {
                // Skip the peek code and read the field or event name.
                bytes.uncheckedReadSkipOne();
                @Nullable StringBuilder sb = read8bit();
                return ObjectUtils.convertTo(expectedClass, WireInternal.INTERNER.intern(sb));
            }

            case EVENT_OBJECT:
                // Skip the peek code and read the event object.
                bytes.uncheckedReadSkipOne();
                return valueIn.object(expectedClass);
        }

        // If the peek code doesn't match any known type, return null.
        return null;
    }

    /**
     * Reads a field with an 8-bit character set encoding into a StringBuilder.
     *
     * @return A StringBuilder containing the read field or null if the read operation failed.
     */
    @Nullable
    StringBuilder read8bit() {
        @NotNull StringBuilder sb = acquireStringBuilder();
        // Try to read the field and return the StringBuilder if successful.
        return bytes.read8bit(sb) ? sb : null;
    }

    @Override
    public void consumePadding() {
        while (true) {
            int code = peekCode();
            switch (code) {
                case PADDING:
                    bytes.uncheckedReadSkipOne();
                    break;

                case PADDING32:
                    bytes.uncheckedReadSkipOne();
                    bytes.readSkip(bytes.readUnsignedInt());
                    break;

                case COMMENT: {
                    bytes.uncheckedReadSkipOne();
                    commentListener.accept(readUtf8());
                    break;
                }

                default:
                    return;
            }
        }
    }

    /**
     * Peeks the code from the current position without advancing the read pointer.
     *
     * @return The unsigned byte at the current read position.
     */
    protected int peekCode() {
        return bytes.peekUnsignedByte();
    }

    /**
     * Reads a field from the wire based on the provided peek code.
     * This method is designed to handle different types of fields including control, special, small fields, etc.
     *
     * @param peekCode   The peek code indicating the type of the field to be read.
     * @param keyName    The key name of the field to be matched against.
     * @param keyCode    The key code of the field to be matched against.
     * @param sb         The StringBuilder to be populated with the read field.
     * @param missingOk  Indicates if missing fields are acceptable.
     * @return The populated StringBuilder with the read field or null if no field matched the given keyName and keyCode.
     */
    private StringBuilder readField(int peekCode, CharSequence keyName, int keyCode, @NotNull StringBuilder sb, boolean missingOk) {
        sb.setLength(0);
        switch (peekCode >> 4) {
            case BinaryWireHighCode.END_OF_STREAM:
                break;

            case BinaryWireHighCode.CONTROL:
            case BinaryWireHighCode.SPECIAL:
                return readSpecialField(peekCode, keyName, keyCode, sb);

            case BinaryWireHighCode.FIELD0:
            case BinaryWireHighCode.FIELD1:
                return readSmallField(peekCode, sb);
            default:
                if (missingOk)
                    // Allow cases where a field might not have been written.
                    break;
                throw new UnsupportedOperationException("Unknown code " + stringForCode(peekCode));
        }

        // In field-less mode, accept any field in order.
        if (fieldLess) {
            return sb;
        }

        return null;
    }

    /**
     * Reads a small field from the wire based on the provided peek code.
     * This method is optimized for reading fields with a length up to 31 characters.
     *
     * @param peekCode   The peek code indicating the type and length of the field to be read.
     * @param sb         The StringBuilder to be populated with the read field.
     * @return The populated StringBuilder with the read field.
     */
    @NotNull
    private StringBuilder readSmallField(int peekCode, @NotNull StringBuilder sb) {
        // Skip the peek code as it's already read.
        bytes.uncheckedReadSkipOne();
        if (bytes.isDirectMemory() && bytes.bytesStore() instanceof NativeBytesStore) {
            // Optimized parsing for direct memory access.
            AppendableUtil.parse8bit_SB1(bytes, sb, peekCode & 0x1f);
        } else {
            try {
                // General parsing for non-direct memory.
                AppendableUtil.parse8bit(bytes, sb, peekCode & 0x1f);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }
        return sb;
    }

    /**
     * Reads a field from the wire based on the provided peek code, specifically for special types of fields.
     * The special fields include field number, field name, event name, field anchor, and event object.
     *
     * @param peekCode   The peek code indicating the type of the special field to be read.
     * @param keyName    The key name of the field to be matched against.
     * @param keyCode    The key code of the field to be matched against.
     * @param sb         The StringBuilder to be populated with the read field.
     * @return The populated StringBuilder with the read special field or null if the peek code does not match any known special field.
     */
    @Nullable
    private StringBuilder readSpecialField(int peekCode, CharSequence keyName, int keyCode, @NotNull StringBuilder sb) {
        switch (peekCode) {
            case FIELD_NUMBER:
                // Skip the peek code as it's already read.
                bytes.uncheckedReadSkipOne();
                long fieldId = bytes.readStopBit();
                // special handling for MethodReader.HISTORY messages as this has a more compact form as it can be very common
                if (fieldId == MethodReader.MESSAGE_HISTORY_METHOD_ID) {
                    sb.setLength(0);
                    sb.append(MethodReader.HISTORY);
                    return sb;
                }
                return readFieldNumber(keyName, keyCode, sb, fieldId);
            case FIELD_NAME_ANY:
            case EVENT_NAME:
                // Read and return the field or event name.
                bytes.uncheckedReadSkipOne();
                bytes.read8bit(sb);
                return sb;

            case EVENT_OBJECT:
                // Get the textual representation of the event object.
                valueIn.text(sb);
                return sb;
        }

        // Return null for unknown special fields.
        return null;
    }


    /**
     * Reads a field number from the wire and populates the provided StringBuilder.
     *
     * @param keyName   The key name of the field to be matched against.
     * @param keyCode   The key code of the field to be matched against.
     * @param sb        The StringBuilder to be populated with the read field number.
     * @param fieldId   The ID of the field read from the wire.
     * @return The populated StringBuilder with the read field number.
     */
    @NotNull
    protected StringBuilder readFieldNumber(CharSequence keyName, int keyCode, @NotNull StringBuilder sb, long fieldId) {
        // Check if the keyCode matches a predefined "ANY" match code.
        if (keyCode == ANY_CODE_MATCH.code()) {
            sb.append(fieldId);  // Append field ID to the StringBuilder.
            return sb;
        }

        // If fieldId doesn't match the expected keyCode, reset the StringBuilder.
        if (fieldId != keyCode)
            return sb;

        // If the fieldId matches the keyCode, append the keyName to the StringBuilder.
        sb.append(keyName);
        return sb;
    }

    /**
     * Gets the string representation of the field data based on the provided code.
     *
     * @param code The code indicating the type of the field data.
     * @param sb   The Appendable and CharSequence object to be populated with the field data.
     * @return The populated Appendable and CharSequence object with the field data.
     */
    @NotNull
    <T extends Appendable & CharSequence> T getStringBuilder(int code, @NotNull T sb) {
        // Parse the UTF-8 encoded data from bytes based on the provided code and populate the StringBuilder.
        bytes.parseUtf8(sb, true, code & 0x1f);
        return sb;
    }

    /**
     * Copies special types of fields from the input wire to the output wire based on the provided peek code.
     *
     * @param wire     The output wire to which the field should be written.
     * @param peekCode The peek code indicating the type of the special field to be copied.
     * @throws InvalidMarshallableException If there's an error during the copy operation.
     */
    private void copySpecial(@NotNull WireOut wire, int peekCode) throws InvalidMarshallableException {
        // Switch based on the type of the field indicated by peekCode.
        switch (peekCode) {
            case COMMENT: {
                // Skip reading one byte and then read the comment.
                bytes.uncheckedReadSkipOne();
                @Nullable StringBuilder sb = readUtf8();
                wire.writeComment(sb);  // Write the comment to the output wire.
                break;
            }

            // Handling timestamp related fields.
            case TIME:
                wire.getValueOut().time(getValueIn().time());  // Get the time from the input and write to the output.
                break;
            case DATE:
                wire.getValueOut().date(getValueIn().date());  // Similarly handle for date...
                break;
            case DATE_TIME:
                wire.getValueOut().dateTime(getValueIn().dateTime());  // ...date-time...
                break;
            case ZONED_DATE_TIME:
                wire.getValueOut().zonedDateTime(getValueIn().zonedDateTime());
                break;

            case TYPE_PREFIX: {
                // Store the current read position.
                long readPosition = bytes.readPosition();
                bytes.uncheckedReadSkipOne();  // Skip one byte.
                @Nullable StringBuilder sb = readUtf8();
                // Check if the prefix indicates specific encoding/compression.
                if (StringUtils.isEqual("gzip", sb) || StringUtils.isEqual("lzw", sb)) {
                    bytes.readPosition(readPosition);  // Reset the read position.
                    wire.writeComment(sb);  // Write the prefix as a comment.
                    wire.getValueOut().text(valueIn.text());  // Write the actual value.

                } else {
                    // Set the type prefix for the output.
                    wire.getValueOut().typePrefix(sb);

                    try {
                        // Attempt to find the class for the name found in the type prefix.
                        Class<?> aClass = classLookup.forName(sb);

                        // Special handling based on the class type.
                        if (aClass == byte[].class) {
                            wire.getValueOut().text(BytesStore.wrap(valueIn.bytes()));
                            break;
                        }

                        if (aClass.isEnum()) {
                            wire.getValueOut().object(aClass, valueIn.object(aClass));
                            break;
                        }
                        if (aClass.isInterface() || usesSelfDescribing(aClass))
                            break;
                        Marshallable m = (Marshallable) ObjectUtils.newInstance(aClass);
                        valueIn.marshallable(m);
                        wire.getValueOut().marshallable(m);
                    } catch (ClassNotFoundRuntimeException ex) {
                        // Log a warning if the class is not found.
                        if (FIRST_WARN_MISSING_CLASS.compareAndSet(false, true))
                            Jvm.warn().on(BinaryWire.class, "Unable to copy object safely, message will not be repeated: " + ex);
                        copyOne(wire, false);
                    } catch (Exception e) {
                        // Log a warning for any other exceptions.
                        Jvm.warn().on(getClass(), "Unable to copy " + sb + " safely will try anyway " + e);
                    }
                    wire.getValueOut().endTypePrefix();
                }
                break;
            }

            // Handle literals in the wire.
            case TYPE_LITERAL: {
                bytes.uncheckedReadSkipOne();
                @Nullable StringBuilder sb = readUtf8();
                wire.getValueOut().typeLiteral(sb);
                break;
            }

            // Handle event or field names.
            case EVENT_NAME:
            case FIELD_NAME_ANY:
                @Nullable StringBuilder fsb = readField(peekCode, null, ANY_CODE_MATCH.code(), acquireStringBuilder(), true);
                wire.write(fsb);
                break;

            // Start an event object.
            case EVENT_OBJECT:
                bytes.uncheckedReadSkipOne();
                wire.writeStartEvent();
                boolean wasLeaf = wire.getValueOut().swapLeaf(true);
                if (peekCode() == TYPE_PREFIX)
                    copyOne(wire);
                copyOne(wire);
                wire.getValueOut().swapLeaf(wasLeaf);
                wire.writeEndEvent();
                break;

            // Handle strings.
            case STRING_ANY: {
                bytes.uncheckedReadSkipOne();
                @Nullable StringBuilder sb1 = readUtf8();
                wire.getValueOut().text(sb1);
                break;
            }

            // Handle field numbers.
            case FIELD_NUMBER: {
                bytes.uncheckedReadSkipOne();
                long code2 = bytes.readStopBit();
                if (code2 == MethodReader.MESSAGE_HISTORY_METHOD_ID && !wire.isBinary()) {
                    wire.write(MethodReader.HISTORY);
                    break;
                }
                wire.write(new WireKey() {
                    @NotNull
                    @Override
                    public String name() {
                        return Long.toString(code2);
                    }

                    @Override
                    public int code() {
                        return (int) code2;
                    }
                });
                break;
            }

            // Handle boolean values and null.
            case NULL:
                bytes.uncheckedReadSkipOne();
                wire.getValueOut().bool(null);
                break;

            case FALSE:
                bytes.uncheckedReadSkipOne();
                wire.getValueOut().bool(false);
                break;

            case TRUE:
                bytes.uncheckedReadSkipOne();
                wire.getValueOut().bool(true);
                break;
            default:
                unknownCode(wire);
        }
    }

    /**
     * Checks if a given class is self-describing.
     *
     * @param aClass The class to check.
     * @return True if the class is self-describing, false otherwise.
     */
    private boolean usesSelfDescribing(Class<?> aClass) {
        Boolean selfDesc = overrideSelfDescribing == null ? USES_SELF_DESCRIBING.get(aClass) : overrideSelfDescribing;

        // Return true if the class is marked as self-describing.
        return Boolean.TRUE.equals(selfDesc);
    }

    /**
     * Decodes an integer from its binary representation based on a code.
     *
     * @param code The code that indicates the encoding.
     * @return The decoded integer.
     */
    long readInt(int code) {
        // Direct value for codes less than 128.
        if (code < 128)
            return code;
        switch (code >> 4) {
            case BinaryWireHighCode.SPECIAL:
                switch (code) {
                    // Handle special cases for boolean false and true.
                    case FALSE:
                        return 0;
                    case TRUE:
                        return 1;
                }
                break;

            case BinaryWireHighCode.FLOAT:
                // For float codes, read the float and cast to long.
                double d = readFloat0(code);
                return (long) d;

            case BinaryWireHighCode.INT:
                // For integer codes, decode the integer.
                return readInt0(code);
        }

        // If we can't decode, throw an exception.
        throw new UnsupportedOperationException(stringForCode(code));
    }

    /**
     * Decodes a floating point number from its binary representation based on a code.
     *
     * @param code The code that indicates the encoding.
     * @return The decoded floating point number.
     */
    double readFloat0(int code) {
        // Check if the high bit is set (indicating a special encoding).
        // This might be redundant in some call sites, so optimization is needed.
        if ((code & 0x80) == 0) {
            return code;
        }

        switch (code) {
            case FLOAT32:
                // 32-bit floating point representation.
                return bytes.readFloat();
            case FLOAT_STOP_2:
                // Decode and adjust by factor of 1e2.
                return bytes.readStopBit() / 1e2;
            case FLOAT_STOP_4:
                // Decode and adjust by factor of 1e4.
                return bytes.readStopBit() / 1e4;
            case FLOAT_STOP_6:
                // Decode and adjust by factor of 1e6.
                return bytes.readStopBit() / 1e6;
            case FLOAT64:
                // 64-bit floating point representation.
                return bytes.readDouble();
        }
        throw new UnsupportedOperationException(stringForCode(code));
    }

    /**
     * Decodes a floating point number from its binary representation and boxes it as a Number.
     * NOTE: This method boxes primitives, which might produce garbage.
     *
     * @param code The code indicating the encoding.
     * @return The decoded floating point number as a Number.
     */
    // TODO: boxes and creates garbage
    private Number readFloat0bject(int code) {
        // TODO: in some places we have already called this before invoking the function,
        // so we should review them and optimize the calls to do the check only once
        // Handle small positive integers without special encoding.
        if (code < 128 && code >= 0) {
            return code;
        }

        switch (code) {
            case FLOAT32:
                // 32-bit floating point representation.
                return bytes.readFloat();
            case FLOAT_STOP_2:
                // Decode and adjust by a factor of 1e2.
                return bytes.readStopBit() / 1e2;
            case FLOAT_STOP_4:
                // Decode and adjust by a factor of 1e4.
                return bytes.readStopBit() / 1e4;
            case FLOAT_STOP_6:
                // Decode and adjust by a factor of 1e6.
                return bytes.readStopBit() / 1e6;
            case FLOAT64:
                // 64-bit floating point representation.
                return bytes.readDouble();
        }

        // If we can't decode, throw an exception.
        throw new UnsupportedOperationException(stringForCode(code));
    }

    /**
     * Decodes an integer from its binary representation based on a code.
     *
     * @param code The code that indicates the encoding.
     * @return The decoded integer.
     */
    long readInt0(int code) {
        // Handle small positive integers without special encoding.
        if (isSmallInt(code))
            return code;

        switch (code) {
            case INT8:
                // 8-bit signed integer.
                return bytes.readByte();
            case UINT8:
            case SET_LOW_INT8:
                // 8-bit unsigned integer.
                return bytes.readUnsignedByte();
            case INT16:
                // 16-bit signed integer.
                return bytes.readShort();
            case UINT16:
            case SET_LOW_INT16:
                // 16-bit unsigned integer.
                return bytes.readUnsignedShort();
            case INT32:
                // 32-bit signed integer.
                return bytes.readInt();
            case UINT32:
                // 32-bit unsigned integer.
                return bytes.readUnsignedInt();
            case INT64:
            case INT64_0x:
                // 64-bit signed integer.
                return bytes.readLong();
        }

        // If we can't decode, throw an exception.
        throw new UnsupportedOperationException(stringForCode(code));
    }

    /**
     * Decodes an integer value from its binary representation and boxes it as a Number.
     * NOTE: This method boxes primitive numbers, which might introduce garbage in a garbage-collected environment.
     *
     * @param code The code that indicates the encoding.
     * @return The decoded integer value as a Number.
     */
    // TODO: boxes and creates garbage
    Number readInt0object(int code) {
        // Check if the number is a small integer (no encoding needed).
        if (isSmallInt(code))
            return code;

        switch (code) {
            case INT8:
                // 8-bit signed integer.
                return bytes.readByte();
            case UINT8:
            case SET_LOW_INT8:
                // 8-bit unsigned integer.
                return bytes.readUnsignedByte();
            case INT16:
                // 16-bit signed integer.
                return bytes.readShort();
            case SET_LOW_INT16:
            // 16-bit unsigned integer.
            case UINT16:
                return bytes.readUnsignedShort();
            case INT32:
                // 32-bit signed integer.
                return bytes.readInt();
            case UINT32:
                // 32-bit unsigned integer.
                return bytes.readUnsignedInt();
            case INT64:
            case INT64_0x:
                // 64-bit signed integer.
                return bytes.readLong();
        }

        // If the encoding is unrecognized, throw an exception.
        throw new UnsupportedOperationException(stringForCode(code));
    }

    /**
     * Determines if the given code corresponds to a small integer (7-bits).
     *
     * @param code The integer code.
     * @return True if it's a small integer; false otherwise.
     */
    private boolean isSmallInt(int code) {
        return (code & 128) == 0;
    }

    /**
     * Decodes a floating point number from its binary representation.
     *
     * @param code The code indicating the encoding type.
     * @return The decoded floating point value.
     */
    double readFloat(int code) {
        // If the number is a small integer, return it directly as a double.
        if (code < 128)
            return code;
        switch (code >> 4) {
            case BinaryWireHighCode.FLOAT:
                // Decode the floating point number.
                return readFloat0(code);

            case BinaryWireHighCode.INT:
                // Convert the integer to double.
                return readInt0(code);
        }

        // If the encoding is unrecognized, throw an exception.
        throw new UnsupportedOperationException(stringForCode(code));
    }

    @NotNull
    @Override
    public ValueOut write() {
        if (!fieldLess) {
            writeField("");
        }
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut writeEventName(@NotNull WireKey key) {
        return writeEventName(key.name());
    }

    @NotNull
    @Override
    public ValueOut writeEventName(@NotNull CharSequence name) {
        if (bytes.retainedHexDumpDescription())
            bytes.writeHexDumpDescription(name + ": (event)");
        writeCode(EVENT_NAME).write8bit(name);
        return valueOut;
    }

    @Override
    public ValueOut writeEventId(int methodId) {
        writeCode(FIELD_NUMBER).writeStopBit(methodId);
        return valueOut;
    }

    @Override
    public ValueOut writeEventId(String name, int methodId) {
        if (bytes.retainedHexDumpDescription()) {
            writeEventIdDescription(name, methodId);
        }
        writeCode(FIELD_NUMBER).writeStopBit(methodId);
        return valueOut;
    }

    /**
     * Writes a description of an event ID which consists of the event name and its corresponding method ID.
     *
     * @param name     The name of the event.
     * @param methodId The ID of the method associated with the event.
     */
    private void writeEventIdDescription(String name, int methodId) {
        try (ScopedResource<StringBuilder> sbTl = SBP.get()) {
            final StringBuilder sb = sbTl.get();
            sb.append(name).append(" (");

            // Check if the methodId falls within the printable ASCII character range.
            if (' ' < methodId && methodId <= '~')
                sb.append('\'').append((char) methodId).append('\''); // Represent methodId as a character.
            else
                sb.append(methodId); // Use the integer representation.

            sb.append(')');

            // Write the description to bytes in hexadecimal format.
            bytes.writeHexDumpDescription(sb);
        }
    }

    @Override
    public void writeStartEvent() {
        writeCode(EVENT_OBJECT);
    }

    @Override
    public void writeEndEvent() {
        // Do nothing
    }

    @NotNull
    @Override
    public ValueOut write(@NotNull WireKey key) {
        if (!fieldLess) {
            if (numericFields)
                writeField(key.code());
            else
                writeField(key.name());
        }
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut write(@NotNull CharSequence key) {
        if (!fieldLess) {
            if (numericFields)
                writeField(WireKey.toCode(key));
            else
                writeField(key);
        }
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut getValueOut() {
        return valueOut;
    }

    @NotNull
    @Override
    public Wire writeComment(CharSequence s) {
        writeCode(COMMENT);
        bytes.writeUtf8(s);
        return BinaryWire.this;
    }

    @NotNull
    @Override
    public WireOut addPadding(int paddingToAdd) {
        if (paddingToAdd < 0)
            throw new IllegalStateException("Cannot add " + paddingToAdd + " bytes of padding");
        if (paddingToAdd >= 5) {
            writeCode(PADDING32)
                    .writeUnsignedInt(paddingToAdd - 5L)
                    .writeSkip(paddingToAdd - 5L);

        } else {
            for (int i = 0; i < paddingToAdd; i++)
                writeCode(PADDING);
        }
        return this;
    }

    /**
     * Writes a field to the byte buffer with its name.
     *
     * @param name The name of the field.
     */
    private void writeField(@NotNull CharSequence name) {
        // If hex dump retention is enabled, write the field's name as a hex dump description.
        if (bytes.retainedHexDumpDescription())
            bytes.writeHexDumpDescription(name + ":");
        int len = name.length();

        // Decide on encoding strategy based on length of name.
        if (len < 0x20) {
            writeField0(name, len);

        } else {
            writeCode(FIELD_NAME_ANY).write8bit(name);
        }
    }

    /**
     * Writes a field to the byte buffer with the given name and length.
     * If the name starts with a digit, tries to parse it as an integer.
     *
     * @param name The name of the field.
     * @param len  The length of the field name.
     */
    private void writeField0(@NotNull CharSequence name, int len) {
        // If name starts with a digit, attempt to parse it as an integer.
        if (len > 0 && isDigit(name.charAt(0))) {
            try {
                writeField(StringUtils.parseInt(name, 10));
                return;
            } catch (NumberFormatException ignored) {
            }
        }

        // Write the length-prefixed name.
        bytes.writeByte((byte) (FIELD_NAME0 + len));
        bytes.append8bit(name);
    }

    /**
     * Writes a field to the byte buffer using a numeric code representation.
     *
     * @param code The numeric code of the field.
     */
    private void writeField(int code) {
        // If hex dump retention is enabled, write the code as a hex dump description.
        if (bytes.retainedHexDumpDescription())
            bytes.writeHexDumpDescription(Integer.toString(code));

        // Write the code for the field and then its numeric representation.
        writeCode(FIELD_NUMBER);
        bytes.writeStopBit(code);
    }

    /**
     * Writes a byte code to the byte buffer.
     *
     * @param code The byte code to write.
     * @return Returns the byte buffer after writing the code.
     */
    protected Bytes<?> writeCode(int code) {
        return bytes.writeByte((byte) code);
    }

    /**
     * Reads and decodes binary data based on a given code into a textual form.
     *
     * @param code The code indicating the type of data to be read.
     * @param sb   An appendable and char sequence target where the decoded text will be appended.
     * @param <T>  A type that extends both Appendable and CharSequence.
     * @return Returns the passed-in appendable populated with decoded text or null.
     */
    @Nullable <T extends Appendable & CharSequence> T readText(int code, @NotNull T sb) {

        // If the code is an ASCII value, simply append it to the StringBuilder.
        if (code <= 127) {
            AppendableUtil.append(sb, code);
            return sb;
        }

        // Split the parsing based on the high bits of the code.
        switch (code >> 4) {
            case BinaryWireHighCode.CONTROL:
                // Handle various control codes.
                switch (code) {
                    // Handle byte-length based data reading.
                    case BYTES_LENGTH8:
                    case BYTES_LENGTH16:
                    case BYTES_LENGTH32:
                        if (sb instanceof StringBuilder) {
                            bytes.uncheckedReadSkipBackOne();
                            valueIn.bytesStore((StringBuilder) sb);
                        } else if (sb instanceof Bytes) {
                            bytes.uncheckedReadSkipBackOne();
                            valueIn.bytesStore((Bytes) sb);
                        } else {
                            throw new IllegalArgumentException("Expected a StringBuilder or Bytes");
                        }
                        return sb;

                    // Handle padding.
                    case PADDING:
                        return readText(bytes.readUnsignedByte(), sb);
                    case PADDING32:
                        bytes.readSkip(bytes.readUnsignedInt());
                        return readText(bytes.readUnsignedByte(), sb);
                }
                throw unknownCode(code);

            case BinaryWireHighCode.SPECIAL:
                // Handle special codes.
                switch (code) {
                    case NULL:
                        AppendableUtil.append(sb, "null");
                        return sb;
                    case TRUE:
                        AppendableUtil.append(sb, "true");
                        return sb;
                    case FALSE:
                        // Handle common special values.
                        AppendableUtil.append(sb, "false");
                        return sb;
                    case TIME:
                    case DATE:
                    case DATE_TIME:
                    case ZONED_DATE_TIME:
                    case TYPE_LITERAL:
                    case STRING_ANY:
                        // Handle various string/textual representations.
                        if (bytes.readUtf8(sb))
                            return sb;
                        return null;
                    case EVENT_OBJECT:
                        valueIn.text((StringBuilder) sb);
                        return sb;
                }
                throw unknownCode(code);

            case BinaryWireHighCode.FLOAT:
                // Handle float values.
                AppendableUtil.append(sb, readFloat(code));
                return sb;
            case BinaryWireHighCode.INT:
                // Handle integer values.
                AppendableUtil.append(sb, readInt(code));
                return sb;
            case BinaryWireHighCode.STR0:
            case BinaryWireHighCode.STR1:
                // Handle string values.
                return getStringBuilder(code, sb);

            case BinaryWireHighCode.FIELD0:
            case BinaryWireHighCode.FIELD1:
                try (final ScopedResource<StringBuilder> sbTl = SBP.get()) {
                    readField(sbTl.get(), "", code);
                }
                AppendableUtil.setLength(sb, 0);
                return readText(peekCode(), sb);
            default:
                throw unknownCode(code);
        }
    }

    /**
     * Generates an UnsupportedOperationException when an unknown code is encountered.
     * <p>
     * The function formats the provided code to a hexadecimal string and
     * includes it in the exception message.
     *
     * @param code The code that was not recognized or understood.
     * @return A new UnsupportedOperationException initialized with a message detailing the unknown code.
     */
    @NotNull
    private UnsupportedOperationException unknownCode(int code) {
        // Format the code as a hexadecimal string and return the exception.
        return new UnsupportedOperationException("code=0x" + String.format("%02X ", code).trim());
    }

    /**
     * Reads a code from the byte storage without checks.
     * <p>
     * This method retrieves a code that indicates the format or type of the next data
     * to be read from the bytes storage.
     *
     * @return The code retrieved from the byte storage.
     */
    int readCode() {
        // Use unchecked method to read the next unsigned byte as code.
        return bytes.uncheckedReadUnsignedByte();
    }

    /**
     * Converts the current state of the bytes storage to a debug string.
     * <p>
     * This method provides a string representation of the bytes, useful for
     * debugging purposes.
     *
     * @return A string representation of the bytes.
     */
    @NotNull
    public String toString() {
        return bytes.toDebugString();
    }

    @NotNull
    @Override
    public LongValue newLongReference() {
        return new BinaryLongReference();
    }

    @NotNull
    @Override
    public BooleanValue newBooleanReference() {
        return new BinaryBooleanReference();
    }

    @NotNull
    @Override
    public TwoLongValue newTwoLongReference() {
        return new BinaryTwoLongReference();
    }

    @NotNull
    @Override
    public IntValue newIntReference() {
        return new BinaryIntReference();
    }

    @NotNull
    @Override
    public BinaryLongArrayReference newLongArrayReference() {
        return new BinaryLongArrayReference();
    }

    @Override
    public @NotNull IntArrayValues newIntArrayReference() {
        return new BinaryIntArrayReference();
    }

    /**
     * Reads a UTF-8 formatted string from the byte storage.
     * <p>
     * The method retrieves a string encoded in UTF-8 from the bytes storage,
     * appending the characters to a StringBuilder.
     *
     * @return A StringBuilder populated with the read string if successful, or null if not.
     */
    @Nullable
    StringBuilder readUtf8() {
        @NotNull StringBuilder sb = acquireStringBuilder();
        // Read the UTF-8 encoded string into the StringBuilder.
        return bytes.readUtf8(sb) ? sb : null;
    }

    /**
     * Determines whether a given object should use the self-describing message format.
     * <p>
     * This method first checks if an override value for self-describing is set. If not,
     * it uses the self-describing value from the provided object.
     *
     * @param object The object whose self-describing status needs to be checked.
     * @return true if the object should use the self-describing message format, false otherwise.
     */
    public boolean useSelfDescribingMessage(@NotNull CommonMarshallable object) {
        // Check for override or get the value from the object.
        return overrideSelfDescribing == null ? object.usesSelfDescribingMessage() : overrideSelfDescribing;
    }

    @Override
    public boolean writingIsComplete() {
        return !writeContext.isNotComplete();
    }

    /**
     * Enum representing any code match within the WireKey interface.
     * This enum is designed to provide a specific code that represents any possible match.
     */
    enum AnyCodeMatch implements WireKey {
        ANY_CODE_MATCH;

        @Override
        public int code() {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Implementation of the ValueOut interface, providing methods to write values in a fixed binary format.
     * This inner class offers various methods to output values in a binary format that retains its order.
     */
    protected class FixedBinaryValueOut implements ValueOut {

        @NotNull
        @Override
        public WireOut bool(@Nullable Boolean flag) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(flag == null ? "null" : flag ? "true" : "false");
            bytes.writeUnsignedByte(flag == null
                    ? NULL
                    : (flag ? TRUE : FALSE));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut nu11() {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription("null");
            writeCode(NULL);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut text(@Nullable CharSequence s) {
            if (s == null) {
                nu11();

            } else {
                if (bytes.retainedHexDumpDescription())
                    bytes.writeHexDumpDescription(s);
                long utflen;
                if (s.length() < 0x20 && (utflen = AppendableUtil.findUtf8Length(s)) < 0x20) {
                    bytes.writeUnsignedByte((int) (STRING_0 + utflen)).appendUtf8(s);
                } else {
                    writeCode(STRING_ANY);
                    bytes.writeUtf8(s);
                }
            }

            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut text(@Nullable String s) {
            if (s == null) {
                writeCode(NULL);

            } else {
                if (bytes.retainedHexDumpDescription())
                    bytes.writeHexDumpDescription(s);
                int len = s.length();
                if (len < 0x20)
                    len = (int) AppendableUtil.findUtf8Length(s);
                char ch;
                if (len == 0) {
                    bytes.writeUnsignedByte(STRING_0);

                } else if (len == 1 && (ch = s.charAt(0)) < 128) {
                    bytes.writeUnsignedByte(STRING_0 + 1).writeUnsignedByte(ch);

                } else if (len < 0x20) {
                    bytes.writeUnsignedByte(STRING_0 + len).appendUtf8(s);
                } else {
                    writeCode(STRING_ANY);
                    bytes.writeUtf8(s);
                }
            }

            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut text(@Nullable BytesStore<?, ?> s) {
            if (s == null) {
                writeCode(NULL);

            } else {
                int len = s.length();
                if (len < 0x20) {
                    bytes.writeUnsignedByte(STRING_0 + len).appendUtf8(s);
                } else {
                    writeCode(STRING_ANY).writeUtf8(s);
                }
            }

            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(@Nullable BytesStore<?, ?> fromBytes) {
            if (fromBytes == null)
                return nu11();
            long remaining = fromBytes.readRemaining();
            if (remaining >= compressedSize()) {
                compress(compression, fromBytes.bytesForRead());
            } else {
                bytes0(fromBytes, remaining);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut bytesLiteral(@Nullable BytesStore<?, ?> fromBytes) {
            if (fromBytes == null)
                return nu11();
            long remaining = fromBytes.readRemaining();
            writeLength(Maths.toInt32(remaining));
            bytes.write(fromBytes);
            return BinaryWire.this;
        }

        @Override
        public int compressedSize() {
            return compressedSize;
        }

        /**
         * Writes bytes from the given BytesStore to the byte storage.
         * <p>
         * It first writes the length of the bytes and then the bytes themselves.
         * This method ensures the U8_ARRAY code is prefixed before the actual byte data.
         *
         * @param fromBytes The source of the bytes to be written.
         * @param remaining The number of bytes to be written.
         */
        public void bytes0(@NotNull BytesStore<?, ?> fromBytes, long remaining) {
            // Write the length of the bytes.
            writeLength(Maths.toInt32(remaining + 1));
            // Write the U8_ARRAY code.
            writeCode(U8_ARRAY);
            // Write the actual bytes if there are any.
            if (remaining > 0)
                bytes.write(fromBytes);
        }

        @NotNull
        @Override
        public WireOut rawBytes(@NotNull byte[] value) {
            typePrefix(byte[].class);
            writeLength(Maths.toInt32(value.length + 1L));
            writeCode(U8_ARRAY);
            if (value.length > 0)
                bytes.write(value);
            return BinaryWire.this;
        }

        @Override
        @NotNull
        public ValueOut writeLength(long length) {
            if (length < 0) {
                throw new IllegalArgumentException("Invalid length " + length);

            } else if (length < 1 << 8) {
                writeCode(BYTES_LENGTH8);
                bytes.writeUnsignedByte((int) length);

            } else if (length < 1 << 16) {
                writeCode(BYTES_LENGTH16);
                bytes.writeUnsignedShort((int) length);

            } else {
                writeCode(BYTES_LENGTH32);
                bytes.writeUnsignedInt(length);
            }
            return this;
        }

        @NotNull
        @Override
        public WireOut bytes(@NotNull byte[] fromBytes) {
            writeLength(Maths.toInt32(fromBytes.length + 1L));
            writeCode(U8_ARRAY);
            bytes.write(fromBytes);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(String type, @Nullable BytesStore<?, ?> fromBytes) {
            typePrefix(type);
            if (fromBytes != null)
                bytes0(fromBytes, fromBytes.readRemaining());
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(String type, @NotNull byte[] fromBytes) {
            typePrefix(type);
            return bytes(fromBytes);
        }

        @NotNull
        @Override
        public WireOut int8(byte i8) {
            return fixedInt8(i8);
        }

        @Override
        @NotNull
        public WireOut fixedInt8(byte i8) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(Integer.toString(i8));
            writeCode(INT8).writeByte(i8);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint8checked(int u8) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(Integer.toString(u8));
            writeCode(UINT8).writeUnsignedByte(u8);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int16(short i16) {
            return fixedInt16(i16);
        }

        @Override
        @NotNull
        public WireOut fixedInt16(short i16) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(Integer.toString(i16));
            writeCode(INT16).writeShort(i16);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint16checked(int u16) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(Integer.toString(u16));
            writeCode(UINT16).writeUnsignedShort(u16);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut utf8(int codepoint) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(new String(Character.toChars(codepoint)));
            writeCode(UINT16);
            bytes.appendUtf8(codepoint);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int32(int i32) {
            return fixedInt32(i32);
        }

        @Override
        @NotNull
        public WireOut fixedInt32(int i32) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(Integer.toString(i32));
            writeCode(INT32).writeInt(i32);
            return BinaryWire.this;
        }

        /**
         * Writes a 32-bit integer value to the byte storage in an order-preserving binary format.
         * <p>
         * Additionally, if hex dump description is retained, it writes a hex dump description.
         *
         * @param i32 The 32-bit integer value to be written.
         * @return The current WireOut instance.
         */
        @NotNull
        public WireOut fixedOrderedInt32(int i32) {
            // Check if hex dump description should be written.
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(Integer.toString(i32));
            // Write the integer value in an ordered format.
            writeCode(INT32).writeOrderedInt(i32);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint32checked(long u32) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(Long.toUnsignedString(u32));
            writeCode(UINT32).writeUnsignedInt(u32);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64(long i64) {
            return fixedInt64(i64);
        }

        @Override
        @NotNull
        public WireOut fixedInt64(long i64) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(Long.toString(i64));
            writeCode(INT64).writeLong(i64);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64_0x(long i64) {
            writeCode(INT64_0x).writeLong(i64);
            return BinaryWire.this;
        }

        /**
         * Writes a 64-bit integer value to the byte storage in an order-preserving binary format.
         * <p>
         * Additionally, if hex dump description is retained, it writes a hex dump description.
         * This method ensures that the writing is aligned to 8 bytes and then the INT64 code is
         * prefixed before the actual 64-bit data.
         *
         * @param i64 The 64-bit integer value to be written.
         * @return The current WireOut instance.
         */
        @NotNull
        private WireOut fixedOrderedInt64(long i64) {
            // Check if hex dump description should be written.
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(Long.toString(i64));

            // Align to 8 bytes before writing the 64-bit integer.
            writeAlignTo(8, 1);
            // Write the 64-bit integer value in an ordered format.
            writeCode(INT64).writeOrderedLong(i64);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64array(long capacity) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(Long.toString(capacity));
            writeAlignTo(8, 1);
            writeCode(I64_ARRAY);
            BinaryLongArrayReference.lazyWrite(bytes, capacity);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int128forBinding(long value, long value2) {
            writeAlignTo(16, 1);
            writeCode(I64_ARRAY);
            bytes.writeLong(2);
            bytes.writeLong(2);
            bytes.writeLong(value);
            bytes.writeLong(value2);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64array(long capacity, @NotNull LongArrayValues values) {
            writeAlignTo(8, 1);
            writeCode(I64_ARRAY);
            long pos = bytes.writePosition();
            BinaryLongArrayReference.lazyWrite(bytes, capacity);
            ((Byteable) values).bytesStore(bytes, pos, bytes.lengthWritten(pos));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut float32(float f) {
            return fixedFloat32(f);
        }

        @Override
        @NotNull
        public WireOut fixedFloat32(float f) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(Float.toString(f));
            writeCode(FLOAT32).writeFloat(f);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut float64(double d) {
            return fixedFloat64(d);
        }

        @Override
        @NotNull
        public WireOut fixedFloat64(double d) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(Double.toString(d));
            writeCode(FLOAT64).writeDouble(d);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut time(@NotNull LocalTime localTime) {
            final String text = localTime.toString();
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(text);
            writeCode(TIME).writeUtf8(text);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut zonedDateTime(@NotNull ZonedDateTime zonedDateTime) {
            final String text = zonedDateTime.toString();
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(text);
            writeCode(ZONED_DATE_TIME).writeUtf8(text);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut date(@NotNull LocalDate localDate) {
            final String text = localDate.toString();
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(text);
            writeCode(DATE).writeUtf8(text);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut dateTime(@NotNull LocalDateTime localDateTime) {
            final String text = localDateTime.toString();
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(text);
            writeCode(DATE_TIME).writeUtf8(text);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public ValueOut typePrefix(CharSequence typeName) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(typeName);
            if (typeName != null)
                writeCode(TYPE_PREFIX).writeUtf8(typeName);
            return this;
        }

        @Override
        public ClassLookup classLookup() {
            return BinaryWire.this.classLookup();
        }

        @NotNull
        @Override
        public WireOut typeLiteral(CharSequence typeName) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(typeName);
            if (typeName == null)
                nu11();
            else
                writeCode(TYPE_LITERAL).writeUtf8(typeName);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@Nullable Class<?> type) {
            if (bytes.retainedHexDumpDescription() && type != null)
                bytes.writeHexDumpDescription(type.getName());
            if (type == null)
                nu11();
            else
                writeCode(TYPE_LITERAL).writeUtf8(classLookup().nameFor(type));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes<?>> typeTranslator, @Nullable Class<?> type) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(type == null ? null : type.getName());
            writeCode(TYPE_LITERAL);
            typeTranslator.accept(type, bytes);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut uuid(@NotNull UUID uuid) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(uuid.toString());
            writeCode(UUID).writeLong(uuid.getMostSignificantBits()).writeLong(uuid.getLeastSignificantBits());
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription("int32 for binding");
            writeAlignTo(Integer.BYTES, 1);
            fixedInt32(value);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription("int64 for binding");
            writeAlignTo(Long.BYTES, 1);
            fixedOrderedInt64(value);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value, @NotNull IntValue intValue) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription("int32 for binding");
            int32forBinding(value);
            ((BinaryIntReference) intValue).bytesStore(bytes, bytes.writePosition() - 4, 4);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value, @NotNull LongValue longValue) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription("int64 for binding");
            int64forBinding(value);
            ((BinaryLongReference) longValue).bytesStore(bytes, bytes.writePosition() - 8, 8);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut boolForBinding(final boolean value, @NotNull final BooleanValue booleanValue) {
            bool(value);
            ((BinaryBooleanReference) booleanValue).bytesStore(bytes, bytes.writePosition() - 1,
                    1);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int128forBinding(long i64x0, long i64x1, TwoLongValue longValue) {
            int128forBinding(i64x0, i64x1);
            ((BinaryTwoLongReference) longValue).bytesStore(bytes, bytes.writePosition() - 16, 16);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireOut sequence(T t, @NotNull BiConsumer<T, ValueOut> writer) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription("sequence");
            writeCode(BYTES_LENGTH32);
            long position = bytes.writePosition();
            bytes.writeInt(0);

            writer.accept(t, this);

            setSequenceLength(position);
            return BinaryWire.this;
        }

        /**
         * Sets the length of the sequence in the byte storage at the given position.
         * <p>
         * It calculates the length from the current position to the specified position,
         * then writes this length at the specified position. If the bytes instance is
         * of type HexDumpBytes, the length is written directly. Otherwise, a check is
         * performed to ensure that the length fits within a 32-bit integer.
         *
         * @param position The position in the byte storage where the length should be written.
         */
        private void setSequenceLength(long position) {
            // Calculate the length from the current position to the given position.
            long length0 = bytes.lengthWritten(position) - 4;

            // Determine the actual length to be written based on the type of bytes instance.
            int length = bytes instanceof HexDumpBytes
                    ? (int) length0
                    : Maths.toInt32(length0, "Document length %,d out of 32-bit int range.");

            // Write the calculated length at the specified position.
            bytes.writeInt(position, length);
        }

        @NotNull
        @Override
        public <T, K> WireOut sequence(T t, K kls, @NotNull TriConsumer<T, K, ValueOut> writer) throws InvalidMarshallableException {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription("sequence");
            writeCode(BYTES_LENGTH32);
            long position = bytes.writePosition();
            bytes.writeInt(0);

            writer.accept(t, kls, this);

            setSequenceLength(position);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull WriteMarshallable object) throws InvalidMarshallableException {
            if (bytes.retainedHexDumpDescription()) {
                Class<? extends @NotNull WriteMarshallable> aClass = object.getClass();
                String simpleName = Jvm.isLambdaClass(aClass)
                        ? "Marshallable"
                        : aClass.getSimpleName();
                bytes.writeHexDumpDescription(simpleName);
            }
            final BinaryLengthLength binaryLengthLength = object.binaryLengthLength();
            long pos = binaryLengthLength.initialise(bytes);

            if (useSelfDescribingMessage(object))
                object.writeMarshallable(BinaryWire.this);
            else
                ((WriteBytesMarshallable) object).writeMarshallable(BinaryWire.this.bytes());

            binaryLengthLength.writeLength(bytes, pos, bytes.writePosition());
            return BinaryWire.this;
        }

        @Override
        public WireOut bytesMarshallable(WriteBytesMarshallable object) throws InvalidMarshallableException {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(object.getClass().getSimpleName());
            writeCode(BYTES_LENGTH32);
            long position = bytes.writePosition();
            bytes.writeInt(0);

            object.writeMarshallable(BinaryWire.this.bytes());

            long length = bytes.lengthWritten(position) - 4;
            if (length > Integer.MAX_VALUE && bytes instanceof HexDumpBytes)
                length = (int) length;
            bytes.writeOrderedInt(position, Maths.toInt32(length, "Document length %,d out of 32-bit int range."));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull Serializable object) throws InvalidMarshallableException {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(object.getClass().getSimpleName());
            writeCode(BYTES_LENGTH32);
            long position = bytes.writePosition();
            bytes.writeInt(0);

            try {
                if (object instanceof Externalizable) {
                    ((Externalizable) object).writeExternal(objectOutput());
                } else {
                    Wires.writeMarshallable(object, BinaryWire.this);
                }
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.lengthWritten(position) - 4, "Document length %,d out of 32-bit int range."));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut map(Map map) throws InvalidMarshallableException {
            return marshallable(map);
        }

        @NotNull
        @Override
        public WireOut wireOut() {
            return BinaryWire.this;
        }

        @Override
        public void resetState() {
            // Do nothing
        }

        @Override
        public void elementSeparator() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Extends the FixedBinaryValueOut class to support binary value outputs with additional logic for converting
     * and writing different types of numbers, namely integers and longs.
     */
     protected class BinaryValueOut extends FixedBinaryValueOut {
        @Override
        public boolean isBinary() {
            return true;
        }

        @Override
        public WireOut writeInt(LongConverter converter, int i) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(converter.asString(i));
            return writeInt(i);
        }

        @Override
        public WireOut writeLong(LongConverter longConverter, long l) {
            if (bytes.retainedHexDumpDescription())
                bytes.writeHexDumpDescription(longConverter.asString(l));
            return writeLong(l);
        }

        @NotNull
        @Override
        public WireOut int8(byte i8) {
            writeNumber(i8);
            return BinaryWire.this;
        }

        /**
         * Writes a number (long value) in an optimized binary format based on the value's size.
         * Uses the number of leading zeros to determine the smallest binary representation
         * that can accommodate the given value.
         *
         * @param l The number (long value) to be written.
         */
        void writeNumber(long l) {
            // Check the number of leading zeros to determine the best representation.
            switch (Long.numberOfLeadingZeros(l)) {
                // Cases for very small numbers that can fit in 8 bits.
                case 64: case 63: case 62: case 61: case 60: case 59: case 58: case 57: case 56:
                    super.uint8checked((short) l);
                    return;
                // Cases for numbers that can fit in 16 bits.
                case 55: case 54: case 53: case 52: case 51: case 50: case 49:
                    super.fixedInt16((short) l);
                    return;

                case 48:
                    super.uint16checked((int) l);
                    return;
                // Cases for numbers that can fit in 32 bits.
                case 47: case 46: case 45: case 44: case 43: case 42: case 41:
                case 40: case 39: case 38: case 37: case 36: case 35: case 34: case 33:
                    super.fixedInt32((int) l);
                    return;

                case 32:
                    super.uint32checked(l);
                    return;

                case 0: // Handling negative numbers
                    if (l >= Byte.MIN_VALUE) {
                        super.int8((byte) l);
                        return;
                    }

                    if (l >= Short.MIN_VALUE) {
                        super.int16((short) l);
                        return;
                    }

                    if (l >= Integer.MIN_VALUE) {
                        super.int32((int) l);
                        return;
                    }
                    break;
                default:
                    break;
            }

            // Fallback checks if the number can be represented as a 32-bit float
            if ((long) (float) l == l) {
                super.float32(l);
                return;
            }

            // Default case for larger numbers.
            super.int64(l);
        }

        /**
         * Writes an integer number in an optimized binary format based on the value's size.
         * Uses the number of leading zeros to determine the smallest binary representation
         * that can accommodate the given value.
         *
         * @param l The number (int value) to be written.
         */
        void writeNumber(int l) {
            // Offset leading zeros count for an integer to align with the long representation logic
            switch (Integer.numberOfLeadingZeros(l) + 32) {
                // Cases for very small numbers that can fit in 8 bits.
                case 64: case 63: case 62: case 61: case 60: case 59: case 58: case 57: case 56:
                    super.uint8checked((short) l);
                    return;
                // Cases for numbers that can fit in 16 bits.
                case 55: case 54: case 53: case 52: case 51: case 50: case 49:
                    super.fixedInt16((short) l);
                    return;

                case 48:
                    super.uint16checked(l);
                    return;
                // Cases for numbers that can fit in 32 bits.
                case 47: case 46: case 45: case 44: case 43: case 42: case 41:
                case 40: case 39: case 38: case 37: case 36: case 35: case 34: case 33:
                    super.fixedInt32(l);
                    return;
                case 32: // Handling negative numbers
                    if (l >= Byte.MIN_VALUE) {
                        super.int8((byte) l);

                    } else if (l >= Short.MIN_VALUE) {
                        super.int16((short) l);

                    } else {
                        super.int32(l);
                    }
                    return;
                default:
                    assert false; // Assertion to ensure all possible cases are handled
            }
        }

        /**
         * Writes a float number either as a float or as an integer based on its value.
         *
         * @param l The float number to be written.
         */
        void writeNumber(float l) {
            boolean canOnlyBeRepresentedAsFloatingPoint = ((long) l) != l;
            if (canOnlyBeRepresentedAsFloatingPoint) {
                writeAsFloat(l);
            } else {
                writeAsIntOrFloat(l);
            }
        }

        /**
         * Writes a double number either as a float or as an integer based on its value.
         *
         * @param l The double number to be written.
         */
        void writeNumber(double l) {
            boolean canOnlyBeRepresentedAsFloatingPoint = ((long) l) != l;
            if (canOnlyBeRepresentedAsFloatingPoint) {
                writeAsFloat(l);
            } else {
                writeAsIntOrFloat(l);
            }
        }

        /**
         * Writes the given float value either as an integer or a float based on its value and range.
         * It checks for possible negative values and writes it in the most appropriate format.
         *
         * @param l The float value to be written.
         */
        private void writeAsIntOrFloat(float l) {
            if (l >= 0) {
                writeAsPositive(l);

            } else if (l >= Byte.MIN_VALUE) {
                super.int8((byte) l); // Write as a signed byte if within the range
            } else if (l >= Short.MIN_VALUE) {
                super.int16((short) l); // Write as a signed short if within the range
            } else {
                super.float32(l); // Write as a float
            }
        }

        /**
         * Writes the given double value either as an integer, a float, or a double
         * based on its value and range. It checks for possible negative values
         * and writes it in the most appropriate format.
         *
         * @param l The double value to be written.
         */
        private void writeAsIntOrFloat(double l) {
            if (l >= 0) {
                writeAsPositive(l);

            } else if (l >= Byte.MIN_VALUE) {
                super.int8((byte) l); // Write as a signed byte if within the range
            } else if (l >= Short.MIN_VALUE) {
                super.int16((short) l); // Write as a signed short if within the range
            } else if ((float) l == l) {
                super.float32((float) l); // Write as a float if can be precisely represented
            } else if (l >= Integer.MIN_VALUE) {
                super.int32((int) l); // Write as a signed int if within the range
            } else {
                super.float64(l); // Write as a double
            }
        }

        /**
         * Writes the given positive double value in an optimized binary format
         * based on its size. Uses the value to determine the smallest binary representation
         * that can accommodate it.
         *
         * @param l The positive double value to be written.
         */
        private void writeAsPositive(double l) {
            if (l <= (1 << 8) - 1) {
                super.uint8checked((short) l); // Write as an unsigned byte
            } else if (l <= (1 << 16) - 1) {
                super.uint16checked((int) l); // Write as an unsigned short
            } else if ((float) l == l) {
                super.float32((float) l); // Write as a float if can be precisely represented
            } else if (l <= (1L << 32L) - 1) {
                super.uint32checked((long) l); // Write as an unsigned int
            } else {
                super.float64(l); // Write as a double
            }
        }

        /**
         * Writes the given float value either as a fixed-point representation or as a regular float.
         * It first attempts to represent the value as fixed-point, but if that's not possible,
         * writes it as a standard float.
         *
         * @param l The float value to be written.
         */
        private void writeAsFloat(float l) {
            // Attempt to convert the float to a fixed-point representation with 6 decimal places
            long l6 = Math.round(l * 1e6);
            // Check if the fixed-point conversion is valid and within specific bounds
            if (l6 / 1e6f == l && l6 > (-1L << 2 * 7) && l6 < (1L << 3 * 7)) {
                if (writeAsFixedPoint(l, l6))
                    return; // If written successfully as fixed-point, exit the method
            }

            // Write as a standard float
            super.float32(l);
        }

        /**
         * Writes the given double value either as a fixed-point representation, a float, or a double.
         * It first tries to represent the value as fixed-point. If that's not possible, checks if
         * it can be represented precisely as a float; otherwise, writes it as a double.
         *
         * @param l The double value to be written.
         */
        private void writeAsFloat(double l) {
            // Attempt to convert the double to a fixed-point representation with 6 decimal places
            long l6 = Math.round(l * 1e6);
            // Check if the fixed-point conversion is valid and within specific bounds
            if (l6 / 1e6 == l && l6 > (-1L << 5 * 7) && l6 < (1L << 6 * 7)) {
                if (writeAsFixedPoint(l, l6))
                    return; // If written successfully as fixed-point, exit the method
            }

            // Check if the double can be represented precisely as a float or if it's NaN
            if (((double) (float) l) == l || Double.isNaN(l)) {
                super.float32((float) l); // Write as a float
                return;
            }

            // Write as a double
            super.float64(l);
        }

        /**
         * Attempts to write the float value as a fixed-point number with 2, 4, or 6 decimal precision.
         * If successful, writes the value with the appropriate code.
         *
         * @param l The float value.
         * @param l6 The float value multiplied by 1e6 and rounded.
         * @return true if the float was written as fixed-point, otherwise false.
         */
        private boolean writeAsFixedPoint(float l, long l6) {
            // Try 2 decimal precision
            long i2 = l6 / 10000;
            if (i2 / 1e2f == l) {
                if (bytes.retainedHexDumpDescription()) bytes.writeHexDumpDescription(i2 + "/1e2");
                writeCode(FLOAT_STOP_2).writeStopBit(i2);
                return true;
            }

            // Try 4 decimal precision
            long i4 = l6 / 100;
            if (i4 / 1e4f == l) {
                if (bytes.retainedHexDumpDescription()) bytes.writeHexDumpDescription(i4 + "/1e4");
                writeCode(FLOAT_STOP_4).writeStopBit(i4);
                return true;
            }

            // Try 6 decimal precision
            if (l6 / 1e6f == l) {
                if (bytes.retainedHexDumpDescription()) bytes.writeHexDumpDescription(l6 + "/1e6");
                writeCode(FLOAT_STOP_6).writeStopBit(l6);
                return true;
            }

            // The float could not be represented as a fixed-point with 2, 4, or 6 decimal precision
            return false;
        }

        /**
         * Attempts to write the double value as a fixed-point number with 2, 4, or 6 decimal precision.
         * If successful, writes the value with the appropriate code.
         *
         * @param l The double value.
         * @param l6 The double value multiplied by 1e6 and rounded.
         * @return true if the double was written as fixed-point, otherwise false.
         */
        private boolean writeAsFixedPoint(double l, long l6) {
            // (The logic here is the same as for the float method)
            long i2 = l6 / 10000;
            if (i2 / 1e2 == l) {
                if (bytes.retainedHexDumpDescription()) bytes.writeHexDumpDescription(i2 + "/1e2");
                writeCode(FLOAT_STOP_2).writeStopBit(i2);
                return true;
            }

            long i4 = l6 / 100;
            if (i4 / 1e4 == l) {
                if (bytes.retainedHexDumpDescription()) bytes.writeHexDumpDescription(i4 + "/1e4");
                writeCode(FLOAT_STOP_4).writeStopBit(i4);
                return true;
            }

            if (l6 / 1e6 == l) {
                if (bytes.retainedHexDumpDescription()) bytes.writeHexDumpDescription(l6 + "/1e6");
                writeCode(FLOAT_STOP_6).writeStopBit(l6);
                return true;
            }
            return false;
        }

        @NotNull
        @Override
        public WireOut uint8checked(int u8) {
            writeNumber(u8);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int16(short i16) {
            writeNumber(i16);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint16checked(int u16) {
            writeNumber(u16);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int32(int i32) {
            writeNumber(i32);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint32checked(long u32) {
            writeNumber(u32);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64(long i64) {
            writeNumber(i64);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut float32(float f) {
            writeNumber(f);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut float64(double d) {
            writeNumber(d);
            return BinaryWire.this;
        }
    }

    /**
     * Represents a binary input value and provides methods to read and manage the state
     * of this binary input. The class encapsulates a stack for managing multiple states
     * and a reader for handling the binary data.
     */
    protected class BinaryValueIn implements ValueIn {

        // Stack for managing the state of the binary input
        final ValueInStack stack = new ValueInStack();

        // The reader responsible for handling the binary data
        final Reader reader0field = this::reader0;

        @Override
        public boolean isBinary() {
            return true;
        }

        @Override
        public long readLong(LongConverter longConverter) {
            return readLong();
        }

        @Override
        public void resetState() {
            stack.reset();
        }

        /**
         * Pushes the current state of the binary input onto the stack.
         * This is useful for saving a state to be restored later.
         */
        public void pushState() {
            stack.push();
        }

        /**
         * Pops the state from the top of the stack and restores the binary input to that state.
         * This method is useful for returning to a previously saved state.
         */
        public void popState() {
            stack.pop();
        }

        /**
         * Retrieves the current state of the binary input.
         *
         * @return The current state of the binary input.
         */
        public ValueInState curr() {
            return stack.curr();
        }

        @NotNull
        @Override
        public BracketType getBracketType() {
            consumePadding();
            switch (peekCode()) {
                case BYTES_LENGTH8:
                    return getBracketTypeFor(bytes.readUnsignedByte(bytes.readPosition() + 1 + 1));
                case BYTES_LENGTH16:
                    return getBracketTypeFor(bytes.readUnsignedByte(bytes.readPosition() + 2 + 1));
                case BYTES_LENGTH32:
                    return getBracketTypeFor(bytes.readUnsignedByte(bytes.readPosition() + 4 + 1));
                case NULL:
                    return BracketType.NONE;
            }
            return BracketType.NONE;
        }

        /**
         * Reads text from the wire and consumes it using the provided string consumer.
         *
         * @param s A {@link Consumer} that accepts and processes the read string.
         * @return The instance of BinaryWire that this handler belongs to.
         */
        @NotNull
        WireIn text(@NotNull Consumer<String> s) {
            // Consume any padding before reading text
            consumePadding();
            int code = readCode();
            switch (code) {
                case NULL:
                    s.accept(null);
                    break;

                case STRING_ANY:
                    // Read the UTF-8 string from bytes and consume it
                    s.accept(bytes.readUtf8());
                    break;
                default:
                    // Check for special string codes
                    if (code >= STRING_0 && code <= STRING_31) {
                        @NotNull StringBuilder sb = acquireStringBuilder();
                        // Parse the UTF-8 string based on its length code and consume it
                        bytes.parseUtf8(sb, code & 0b11111);
                        s.accept(WireInternal.INTERNER.intern(sb));

                    } else {
                        // If an unrecognized code is found, throw an exception
                        cantRead(code);
                    }
            }
            return BinaryWire.this;
        }

        /**
         * Checks if the provided code corresponds to text data.
         *
         * @param code The code to be checked.
         * @return true if the code corresponds to text, false otherwise.
         */
        private boolean isText(int code) {
            // Check for general string code or specific length-based codes
            return code == STRING_ANY ||
                    (code >= STRING_0 && code <= STRING_31);
        }

        @Nullable
        @Override
        public StringBuilder textTo(@NotNull StringBuilder sb) {
            int code = readCode();
            boolean wasNull = code == NULL;
            if (wasNull) {
                sb.setLength(0);
                return null;

            } else {
                @Nullable StringBuilder text = readText(code, sb);
                if (text == null)
                    cantRead(code);
                return sb;
            }
        }

        @Nullable
        @Override
        public Bytes<?> textTo(@NotNull Bytes<?> bytes) {
            int code = readCode();
            boolean wasNull = code == NULL;
            if (wasNull) {
                bytes.readPosition(0);
                return null;

            } else {
                @Nullable Bytes<?> text = readText(code, bytes);
                if (text == null)
                    cantRead(code);
                return bytes;
            }
        }

        @Nullable
        @Override
        public String text() {
            int code = readCode();
            switch (code) {
                case NULL:
                    return null;

                case STRING_ANY: {
                    long len0 = bytes.readStopBit();
                    if (len0 == -1L) {
                        return null;

                    }
                    int len = Maths.toUInt31(len0);
                    long limit = bytes.readLimit();
                    long end = bytes.readPosition() + len;
                    try {
                        bytes.readLimit(end);
                        return UTF8.intern(bytes);
                    } finally {
                        bytes.readLimit(limit);
                        bytes.readPosition(end);
                    }
                }

                case TYPE_PREFIX: {
                    @Nullable StringBuilder sb = readUtf8();
                    if (sb != null) {
                        @Nullable byte[] bytes = Compression.uncompress(sb, this, ValueIn::bytes);
                        if (bytes != null)
                            return new String(bytes, StandardCharsets.UTF_8);
                    }
                    @Nullable StringBuilder text = readText(code, acquireStringBuilder());
                    return WireInternal.INTERNER.intern(text);
                }

                default: {
                    StringBuilder sb = acquireStringBuilder();
                    @Nullable StringBuilder text = ((code & 0xE0) == 0xE0)
                            ? getStringBuilder(code, sb)
                            : readText(code, sb);
                    return text == null ? null : WireInternal.INTERNER.intern(text);
                }
            }
        }

        @Override
        @NotNull
        public WireIn bytes(@NotNull BytesOut<?> toBytes) {
            return bytes(toBytes, true);
        }

        @NotNull
        @Override
        public WireIn bytes(@NotNull BytesOut<?> toBytes, boolean clearBytes) {
            long length = readLength();
            int code = readCode();
            if (clearBytes)
                toBytes.clear();
            if (code == NULL) {
                return BinaryWire.this;
            }
            if (code == TYPE_PREFIX) {
                @Nullable StringBuilder sb = readUtf8();
                assert sb != null;

                long length2 = readLength();
                int code2 = readCode();
                if (code2 != U8_ARRAY)
                    cantRead(code);

                bytes.readWithLength0(length2 - 1, (b, sb1, toBytes1) -> Compression.uncompress(sb1, b, toBytes1), sb, toBytes);
                return wireIn();

            }
            if (code == U8_ARRAY) {
                ((Bytes) bytes).readWithLength(length - 1, toBytes);
            } else {
                bytes.uncheckedReadSkipBackOne();
                textTo((Bytes) toBytes);
            }
            return wireIn();
        }

        @NotNull
        @Override
        public WireIn bytesLiteral(@NotNull BytesOut<?> toBytes) {
            long length = readLength();
            toBytes.clear();
            toBytes.write(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return wireIn();
        }

        @NotNull
        @Override
        public BytesStore<?, ?> bytesLiteral() {
            int length = Maths.toUInt31(readLength());
            @NotNull BytesStore<?, ?> toBytes = BytesStore.wrap(new byte[length]);
            toBytes.write(0, bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return toBytes;
        }

        @Override
        @Nullable
        public WireIn bytesSet(@NotNull PointerBytesStore toBytes) {
            long length = readLength();
            int code = readCode();
            if (code == NULL) {
                return BinaryWire.this;
            }
            if (code != U8_ARRAY)
                cantRead(code);
            long startAddr = bytes.addressForRead(bytes.readPosition());
            toBytes.set(startAddr, length - 1);
            bytes.readSkip(length - 1);
            return wireIn();
        }

        @NotNull
        @Override
        public WireIn bytesMatch(@NotNull BytesStore<?, ?> compareBytes, @NotNull BooleanConsumer consumer) {
            long length = readLength();
            int code = readCode();
            if (code != U8_ARRAY)
                cantRead(code);
            length--;
            if (compareBytes.readRemaining() == length) {
                consumer.accept(bytes.equalBytes(compareBytes, length));
            } else {
                consumer.accept(false);
            }
            bytes.readSkip(length);
            return wireIn();

        }

        @Override
        @Nullable
        public BytesStore<?, ?> bytesStore() {
            long length = readLength() - 1;
            int code = readCode();
            switch (code) {
                case I64_ARRAY:
                case U8_ARRAY:
                    @NotNull BytesStore<?, ?> toBytes = BytesStore.lazyNativeBytesStoreWithFixedCapacity(length);
                    toBytes.write(0, bytes, bytes.readPosition(), length);
                    bytes.readSkip(length);
                    return toBytes;

                case TYPE_PREFIX: {
                    @Nullable StringBuilder sb = readUtf8();
                    @Nullable byte[] bytes = Compression.uncompress(sb, this, ValueIn::bytes);
                    if (bytes != null)
                        return BytesStore.wrap(bytes);
                    throw new UnsupportedOperationException("Unsupported type " + sb);
                }
                case NULL:
                    return null;

                default:
                    cantRead(code);
                    throw new AssertionError();
            }
        }

        /**
         * Reads bytes from the wire and stores them into the provided StringBuilder.
         *
         * @param sb The StringBuilder to store the read bytes.
         */
        public void bytesStore(@NotNull StringBuilder sb) {
            sb.setLength(0);

            // Consume any padding before reading bytes
            consumePadding();
            long pos = bytes.readPosition();
            long length = readLength();

            // Validate if the read length is valid or recognized
            if (length < 0)
                throw cantRead(peekCode());

            // Ensure enough bytes remain for reading the length specified
            if (length > bytes.readRemaining())
                throw new BufferUnderflowException();

            int code = readCode();
            if (code == U8_ARRAY) {
                // Read each byte and cast to char to store in StringBuilder
                for (long i = 1; i < length; i++)
                    sb.append((char) bytes.readUnsignedByte());
            } else {
                // Reset the reading position and store from size-prefixed blobs
                bytes.readPosition(pos);
                long limit = bytes.readLimit();
                bytes.readLimit(pos + 4 + length);
                try {
                    sb.append(Wires.fromSizePrefixedBlobs(bytes));
                } finally {
                    // Restore the original limit and position after reading
                    bytes.readLimit(limit);
                    bytes.readPosition(limit);
                }
            }
        }

        /**
         * Reads bytes from the wire and stores them into the provided Bytes object.
         *
         * @param toBytes The Bytes object to store the read bytes.
         */
        public void bytesStore(@NotNull Bytes<?> toBytes) {
            // Clear the provided Bytes object before storing
            toBytes.clear();
            long length = readLength() - 1;
            int code = readCode();

            // If null code is encountered, terminate early
            if (code == NULL) {
                return;
            }
            if (code != U8_ARRAY)
                cantRead(code);

            // Validate length to ensure no reading past available bytes
            if (length > bytes.readRemaining())
                throw new IllegalStateException("Length of Bytes " + length + " > " + bytes.readRemaining());

            // Write the bytes read into the provided Bytes object
            toBytes.ensureCapacity(toBytes.writePosition() + length);
            toBytes.write(0, bytes, bytes.readPosition(), length);
            toBytes.readLimit(length);
            bytes.readSkip(length);
        }

        @Override
        @NotNull
        public WireIn bytes(@NotNull ReadBytesMarshallable bytesConsumer) {
            long length = readLength() - 1;
            int code = readCode();
            if (code != U8_ARRAY)
                cantRead(code);

            if (length > bytes.readRemaining())
                throw new BufferUnderflowException();
            long limit0 = bytes.readLimit();
            long limit = bytes.readPosition() + length;
            try {
                bytes.readLimit(limit);
                bytesConsumer.readMarshallable(bytes);
            } finally {
                bytes.readLimit(limit0);
                bytes.readPosition(limit);
            }
            return wireIn();
        }

        @Nullable
        @Override
        public byte[] bytes(byte[] using) {
            long length = readLength();
            int code = readCode();
            if (code == NULL) {
                return null;
            }

            if (code == TYPE_PREFIX) {
                @Nullable StringBuilder sb = readUtf8();
                assert "byte[]".contentEquals(sb);
                length = readLength();
                code = readCode();
            }

            if (code != U8_ARRAY)
                cantRead(code);
            length--;
            @NotNull byte[] bytes2 = using != null && using.length == length ? using : new byte[Maths.toUInt31(length)];
            bytes.readWithLength(length, b -> b.read(bytes2));
            return bytes2;
        }

        @NotNull
        @Override
        public WireIn wireIn() {
            return BinaryWire.this;
        }

        @Override
        public long readLength() {
            // TODO handle non length types as well.

            int code = peekCode();
            if ((code & 0x80) == 0)
                return 1;
            switch (code) {

                case BYTES_LENGTH8:
                    bytes.uncheckedReadSkipOne();
                    return bytes.uncheckedReadUnsignedByte();

                case BYTES_LENGTH16:
                    bytes.uncheckedReadSkipOne();
                    return bytes.readUnsignedShort();

                case BYTES_LENGTH32:
                    bytes.uncheckedReadSkipOne();
                    return bytes.readUnsignedInt();

                case TYPE_PREFIX:
                    bytes.uncheckedReadSkipOne();
                    long len = bytes.readStopBit();
                    bytes.readSkip(len);
                    return readLength();
                case FALSE:
                case TRUE:
                case NULL:
                    return 1;
                case UINT8:
                case INT8:
                case FLOAT_SET_LOW_0:
                case FLOAT_SET_LOW_2:
                case FLOAT_SET_LOW_4:
                    return 1 + 1L;
                case UINT16:
                case INT16:
                    return 1 + 2L;
                case FLOAT32:
                case UINT32:
                case INT32:
                    return 1 + 4L;
                case FLOAT64:
                case INT64:
                    return 1 + 8L;

                case PADDING:
                case PADDING32:
                case COMMENT:
                    consumePadding();
                    return readLength();

                case FLOAT_STOP_2:
                case FLOAT_STOP_4:
                case FLOAT_STOP_6: {
                    long pos = bytes.readPosition() + 1;
                    while (pos < bytes.readLimit()) {
                        if (bytes.readUnsignedByte(pos++) < 0x80)
                            break;
                    }
                    return pos - bytes.readPosition();
                }

                case UUID:
                    return 1 + 8 + 8;

                case INT64_0x:
                    return 1 + 8;

                case DATE:
                case TIME:
                case DATE_TIME:
                case ZONED_DATE_TIME:
                case TYPE_LITERAL:
                case STRING_ANY: {
                    long pos0 = bytes.readPosition();
                    try {
                        bytes.uncheckedReadSkipOne();
                        long len2 = bytes.readStopBit();
                        return bytes.readPosition() - pos0 + len2;
                    } finally {
                        bytes.readPosition(pos0);
                    }
                }

                case I64_ARRAY: {
                    long capacity = bytes.readLong(bytes.readPosition() + 1);
                    return 1 + 2 * 8 + (capacity * Long.BYTES);
                }

                case -1:
                    return 0;

                default:
                    if (code >= STRING_0)
                        return code + (1L - STRING_0);
                    //System.out.println("code=" + code + ", bytes=" + bytes.toHexString());
                    return -1;
            }
        }

        @NotNull
        @Override
        public WireIn skipValue() {
            final long length = readLength();
            if (length < 0)
                objectBestEffort();
            else
                bytes.readSkip(length);

            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> tFlag) {
            int code = readCode();
            switch (code) {
                case NULL:
                    // todo take the default.
                    tFlag.accept(t, null);
                    break;

                case FALSE:
                    tFlag.accept(t, false);
                    break;

                case TRUE:
                    tFlag.accept(t, true);
                    break;

                case PADDING:
                case PADDING32:
                case COMMENT:
                    bytes.uncheckedReadSkipBackOne();
                    consumePadding();
                    bool(t, tFlag);
                    break;

                default:
                    throw cantRead(code);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb) {
            int code = bytes.readUnsignedByte();
            if (code < 128) {
                tb.accept(t, (byte) code);
                return BinaryWire.this;
            }
            int8b(t, tb, code);

            return BinaryWire.this;
        }

        /**
         * Interprets the byte data as an 8-bit integer based on the provided code.
         * Depending on the code, this method can handle padding and text-based integers.
         *
         * @param t The object to which the byte value should be applied.
         * @param tb The consumer that processes the byte value and the object.
         * @param code The code determining the format of the incoming data.
         */
        private <T> void int8b(@NotNull T t, @NotNull ObjByteConsumer<T> tb, int code) {
            // Handle special codes for padding and comments
            switch (code) {
                case PADDING:
                case PADDING32:
                case COMMENT:
                    // Move back one byte to handle padding correctly
                    bytes.uncheckedReadSkipBackOne();
                    consumePadding();
                    code = bytes.readUnsignedByte();
                    break;
            }

            // Check if the code indicates a text format
            if (isText(code))
                tb.accept(t, Byte.parseByte(text()));
            else
                // Parse the data as an integer and pass it to the consumer
                tb.accept(t, (byte) BinaryWire.this.readInt(code));
        }

        @NotNull
        @Override
        public <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            consumePadding();

            final int code = readCode();
            if (isText(code))
                ti.accept(t, Short.parseShort(text()));
            else
                ti.accept(t, (short) BinaryWire.this.readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            final int code = readCode();
            if (isText(code))
                ti.accept(t, Short.parseShort(text()));
            else
                ti.accept(t, (short) BinaryWire.this.readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            consumePadding();
            final int code = readCode();
            if (isText(code))
                ti.accept(t, Integer.parseInt(text()));
            else
                ti.accept(t, (int) BinaryWire.this.readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            consumePadding();
            final int code = readCode();
            if (isText(code))
                ti.accept(t, Integer.parseInt(text()));
            else
                ti.accept(t, (int) BinaryWire.this.readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            consumePadding();
            final int code = readCode();
            if (isText(code))
                tl.accept(t, Long.parseLong(text()));
            else
                tl.accept(t, BinaryWire.this.readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            final int code = readCode();
            if (isText(code))
                tl.accept(t, Long.parseLong(text()));
            else
                tl.accept(t, BinaryWire.this.readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf) {
            consumePadding();
            final int code = readCode();
            if (isText(code))
                tf.accept(t, Float.parseFloat(text()));
            else
                tf.accept(t, (float) BinaryWire.this.readFloat(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td) {
            consumePadding();
            final int code = readCode();
            td.accept(t, BinaryWire.this.readFloat(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime) {
            consumePadding();
            int code = readCode();
            if (code == TIME) {
                setLocalTime.accept(t, readLocalTime());

            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        /**
         * Reads UTF-8 encoded string data and converts it into a {@link LocalTime} object.
         *
         * @return A {@link LocalTime} object parsed from the read string.
         */
        private LocalTime readLocalTime() {
            // Read the UTF-8 encoded string and attempt to parse it as a LocalTime
            @Nullable StringBuilder sb = readUtf8();
            return LocalTime.parse(sb);
        }

        @NotNull
        @Override
        public <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime) {
            consumePadding();
            int code = readCode();
            if (code == ZONED_DATE_TIME) {
                @Nullable StringBuilder sb = readUtf8();
                tZonedDateTime.accept(t, ZonedDateTime.parse(sb));

            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate) {
            consumePadding();
            int code = readCode();
            if (code == DATE) {
                @Nullable StringBuilder sb = readUtf8();
                tLocalDate.accept(t, LocalDate.parse(sb));

            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @Override
        public boolean hasNext() {
            return bytes.readRemaining() > 0;
        }

        @Override
        public boolean hasNextSequenceItem() {
            return hasNext();
        }

        @NotNull
        @Override
        public UUID uuid() {
            consumePadding();
            int code = readCode();
            if (code == UUID) {
                return new UUID(bytes.readLong(), bytes.readLong());

            } else {
                throw cantRead(code);
            }
        }

        @NotNull
        @Override
        public <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid) {
            consumePadding();
            int code = readCode();
            if (code == UUID) {
                tuuid.accept(t, new UUID(bytes.readLong(), bytes.readLong()));

            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter) {
            consumePadding();
            int code = readCode();
            if (code == I64_ARRAY) {
                if (!(values instanceof BinaryLongArrayReference) || values.isClosing())
                    values = new BinaryLongArrayReference();
                @Nullable Byteable b = (Byteable) values;
                long length = BinaryLongArrayReference.peakLength(bytes, bytes.readPosition());
                b.bytesStore(bytes, bytes.readPosition(), length);
                bytes.readSkip(length);
                setter.accept(t, values);

            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn int128(@NotNull TwoLongValue value) {
            consumePadding();
            int code = readCode();
            if (code == I64_ARRAY) {
                @Nullable Byteable b = (Byteable) value;
                long length = BinaryLongArrayReference.peakLength(bytes, bytes.readPosition());
                b.bytesStore(bytes, bytes.readPosition() + 8 /* capacity */ + 8 /* used */, length - 2 * 8);
                bytes.readSkip(length);

            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(@NotNull LongValue value) {
            consumePadding();
            int code = readCode();
            if (code != INT64 && code != 0)
                cantRead(code);

            @NotNull Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn int32(@NotNull IntValue value) {
            consumePadding();
            int code = readCode();
            if (code != INT32)
                cantRead(code);

            @NotNull Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return BinaryWire.this;
        }

        @Override
        public WireIn bool(@NotNull final BooleanValue value) {
            consumePadding();
            int code = readCode();
            if (code != TRUE && code != FALSE)
                cantRead(code);
            bytes.readSkip(-1);
            @NotNull Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter) {
            // if the value is null, then we will create a LongDirectReference to write the data
            // into and then call setter.accept(), this will then update the value
            if (!(value instanceof BinaryLongReference)) {
                value = new BinaryLongReference();
                setter.accept(t, value);
            }
            return int64(value);
        }

        @NotNull
        @Override
        public <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter) {
            consumePadding();
            int code = readCode();
            if (code != INT32)
                cantRead(code);
            if (!(value instanceof Byteable) || ((Byteable) value).maxSize() != 4) {
                value = new BinaryIntReference();
                setter.accept(t, value);
            }
            @NotNull Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return BinaryWire.this;
        }

        @Override
        public <T> boolean sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader) {
            if (isNull())
                return false;
            long length = readLength();
            if (length < 0)
                throw cantRead(peekCode());

            long limit = bytes.readLimit();
            long limit2 = bytes.readPosition() + length;
            bytes.readLimit(limit2);
            try {
                tReader.accept(t, this);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(limit2);
            }
            return true;
        }

        @Override
        public <T> boolean sequence(@NotNull List<T> list,
                                    @NotNull List<T> buffer,
                                    @NotNull Supplier<T> bufferAdd) throws InvalidMarshallableException {
            list.clear();
            return sequence(list, buffer, bufferAdd, reader0field);
        }

        @Override
        public <T> boolean sequence(List<T> list,
                                    @NotNull List<T> buffer,
                                    Supplier<T> bufferAdd,
                                    Reader tReader) throws InvalidMarshallableException {
            if (isNull())
                return false;
            long length = readLength();
            if (length < 0)
                throw cantRead(peekCode());

            long limit = bytes.readLimit();
            long limit2 = bytes.readPosition() + length;
            bytes.readLimit(limit2);
            try {
                tReader.accept(this, list, buffer, bufferAdd);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(limit2);
            }
            return true;
        }

        @NotNull
        @Override
        public <T, K> WireIn sequence(@NotNull T t, K kls, @NotNull TriConsumer<T, K, ValueIn> tReader) throws InvalidMarshallableException {
            consumePadding();
            int code = readCode();
            long length = readLengthPrefixed(code);
            long limit = bytes.readLimit();
            long limit2 = bytes.readPosition() + length;
            bytes.readLimit(limit2);
            try {
                tReader.accept(t, kls, this);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(limit2);
            }
            return BinaryWire.this;
        }

        /**
         * Reads a length-prefixed value based on the provided code.
         * The length can be encoded as 8-bit, 16-bit, or 32-bit depending on the code.
         *
         * @param code The code determining the format of the length value.
         * @return The length value read from the binary data.
         */
        private long readLengthPrefixed(int code) {
            long length;
            switch (code) {
                case BYTES_LENGTH8:
                    // Read an 8-bit unsigned integer as the length
                    length = bytes.readUnsignedByte();
                    break;
                case BYTES_LENGTH16:
                    // Read a 16-bit unsigned integer as the length
                    length = bytes.readUnsignedShort();
                    break;
                case BYTES_LENGTH32:
                    // Read a 32-bit unsigned integer as the length
                    length = bytes.readUnsignedInt();
                    break;
                default:
                    // If an unrecognized code is encountered, throw an exception
                    throw cantRead(code);
            }
            return length;
        }

        @Override
        public <T> int sequenceWithLength(@NotNull T t, @NotNull ToIntBiFunction<ValueIn, T> tReader) {
            consumePadding();
            int code = readCode();
            long length = readLengthPrefixed(code);
            long limit = bytes.readLimit();
            long limit2 = bytes.readPosition() + length;
            bytes.readLimit(limit2);
            try {
                return tReader.applyAsInt(this, t);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(limit2);
            }
        }

        @Override
        public <T> T applyToMarshallable(@NotNull Function<WireIn, T> marshallableReader) {
            consumePadding();
            pushState();
            try {
                long length = readLength();
                if (length >= 0) {
                    long limit = bytes.readLimit();
                    long limit2 = bytes.readPosition() + length;
                    bytes.readLimit(limit2);
                    try {
                        return marshallableReader.apply(BinaryWire.this);
                    } finally {
                        bytes.readLimit(limit);
                        bytes.readPosition(limit2);
                    }
                } else {
                    return marshallableReader.apply(BinaryWire.this);
                }
            } finally {
                popState();
            }
        }

        @Override
        public boolean isTyped() {
            int code = peekCode();
            return code == TYPE_PREFIX;
        }

        @Override
        @Nullable
        public <T> T typedMarshallable() throws IORuntimeException, InvalidMarshallableException {
            pushState();
            try {
                int code = readCode();
                switch (code) {
                    case TYPE_PREFIX:
                        return typedMarshallable0();

                    case NULL:
                        return null;

                    default:
                        cantRead(code);
                        return null; // only if the throw doesn't work.
                }
            } finally {
                popState();
            }
        }

        /**
         * Tries to deserialize a typed Marshallable object from the current state of the wire.
         * The method identifies the type of the Marshallable object by reading a UTF-8 encoded class name
         * and then instantiates and initializes the object accordingly.
         *
         * @param <T> Type of the expected return object.
         * @return The deserialized Marshallable object or null if the UTF-8 encoded class name is not found.
         * @throws InvalidMarshallableException If the deserialization encounters any issues.
         */
        @Nullable
        protected <T> T typedMarshallable0() throws InvalidMarshallableException {
            @Nullable StringBuilder sb = readUtf8();
            if (sb == null)
                return null;
            // its possible that the object that you are allocating may not have a
            // default constructor
            final Class<T> clazz = (Class<T>) classLookup().forName(sb);

            if (Demarshallable.class.isAssignableFrom(clazz)) {
                return (T) demarshallable((Class<? extends Demarshallable>) clazz);
            }

            // Check if the class is neither of type Marshallable nor Demarshallable
            if (!Marshallable.class.isAssignableFrom(clazz) && !Demarshallable.class.isAssignableFrom(clazz))
                throw new IllegalStateException("its not possible to Marshallable and object that" +
                        " is not of type Marshallable, type=" + sb);

            ReadMarshallable m = ObjectUtils.newInstance((Class<ReadMarshallable>) clazz);

            marshallable(m, true);
            return readResolve(m);
        }

        @Override
        @Nullable
        public <T> T typedMarshallable(@NotNull Function<Class<T>, ReadMarshallable> marshallableFunction)
                throws IORuntimeException, InvalidMarshallableException {

            int code = peekCode();
            if (code != TYPE_PREFIX)
                // todo get delta wire to support Function<Class, ReadMarshallable> correctly
                return typedMarshallable();

            @Nullable final Class<T>aClass = (Class<T>) typePrefix();

            if (ReadMarshallable.class.isAssignableFrom(aClass)) {
                final ReadMarshallable marshallable = marshallableFunction.apply(aClass);
                marshallable(marshallable);
                return (T) marshallable;
            }
            return object(null, aClass);
        }

        @Override
        public Class<?> typePrefix() {
            int code = peekCode();
            if (code != TYPE_PREFIX) {
                return null;
            }
            bytes.uncheckedReadSkipOne();
            @Nullable StringBuilder sb = readUtf8();

            try {
                return classLookup().forName(sb);
            } catch (ClassNotFoundRuntimeException e) {
                Jvm.warn().on(BinaryWire.this.getClass(), "Unable to find class " + sb);
                return null;
            }
        }

        @Override
        public Object typePrefixOrObject(Class<?> tClass) {
            int code = peekCode();
            if (code != TYPE_PREFIX) {
                return null;
            }
            bytes.uncheckedReadSkipOne();
            @Nullable StringBuilder sb = readUtf8();

            try {
                return sb == null ? null : classLookup().forName(sb);
            } catch (ClassNotFoundRuntimeException e) {
                if (Wires.dtoInterface(tClass) && GENERATE_TUPLES) {
                    return Wires.tupleFor(tClass, sb.toString());
                }
                throw e;
            }
        }

        @NotNull
        @Override
        public <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts) {
            @NotNull StringBuilder sb = acquireStringBuilder();
            int code = readCode();
            switch (code) {
                case TYPE_PREFIX:
                    bytes.readUtf8(sb);

                    break;
                case NULL:
                    sb.setLength(0);
                    sb.append("!null");
                    break;
                default:
                    cantRead(code);
                    break;
            }
            ts.accept(t, sb);
            return this;
        }

        @NotNull
        @Override
        public <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer) {
            int code = readCode();
            switch (code) {
                case TYPE_LITERAL:
                    @Nullable StringBuilder sb = readUtf8();
                    classNameConsumer.accept(t, sb);
                    break;
                case NULL:
                    classNameConsumer.accept(t, null);
                    break;
                default:
                    cantRead(code);
                    break;
            }
            return BinaryWire.this;
        }

        @Override
        public ClassLookup classLookup() {
            return BinaryWire.this.classLookup();
        }

        @Override
        public Type typeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler) {
            int code = readCode();
            switch (code) {
                case TYPE_LITERAL:
                    @Nullable StringBuilder sb = readUtf8();
                    try {
                        return classLookup().forName(sb);
                    } catch (ClassNotFoundRuntimeException e) {
                        return unresolvedHandler.apply(sb, e.getCause());
                    }
                case NULL:
                    return null;

                default:
                    throw cantRead(code);
            }
        }

        @Override
        public boolean marshallable(@NotNull ReadMarshallable object)
                throws BufferUnderflowException, IORuntimeException, InvalidMarshallableException {
            return marshallable(object, true);
        }

        /**
         * Marshalls the given object and can optionally overwrite existing values.
         *
         * @param object The object to be marshalled.
         * @param overwrite Determines if the existing values should be overwritten.
         * @return True if the operation is successful.
         * @throws BufferUnderflowException If there's not enough data available in the buffer.
         * @throws IORuntimeException If there's a general IO error.
         * @throws InvalidMarshallableException If there's an error specific to marshalling.
         */
        public boolean marshallable(@NotNull ReadMarshallable object, boolean overwrite)
                throws BufferUnderflowException, IORuntimeException, InvalidMarshallableException {
            consumePadding();

            // Check for null value
            if (this.isNull())
                return false;
            pushState();

            // Get the length of the data to be read
            long length = readLength();
            if (length >= 0) {
                long limit = bytes.readLimit();
                long limit2 = bytes.readPosition() + length;
                bytes.readLimit(limit2);
                try {
                    // Read the object based on its type
                    if (useSelfDescribingMessage(object)) {
                        if (overwrite)
                            object.readMarshallable(BinaryWire.this);
                        else
                            Wires.readMarshallable(object, BinaryWire.this, false);
                    } else {
                        ((ReadBytesMarshallable) object).readMarshallable(BinaryWire.this.bytes);
                    }
                } finally {
                    bytes.readLimit(limit);
                    bytes.readPosition(limit2);
                    popState();
                }
            } else {
                throw new IORuntimeException("Length unknown");
            }
            return true;
        }

        @Override
        public boolean isNull() {
            consumePadding();
            if (peekCode() == NULL) {
                bytes.uncheckedReadSkipOne();
                return true;
            }
            return false;
        }

        @Override
        @Nullable
        public Object marshallable(@NotNull Object object, @NotNull SerializationStrategy strategy)
                throws BufferUnderflowException, IORuntimeException, InvalidMarshallableException {
            if (this.isNull())
                return null;
            pushState();
            consumePadding();
            long length = readLength();
            if (length >= 0) {
                long limit = bytes.readLimit();
                long limit2 = bytes.readPosition() + length;
                bytes.readLimit(limit2);
                try {
                    strategy.readUsing(null, Jvm.uncheckedCast(object), this, BracketType.MAP);

                } finally {
                    bytes.readLimit(limit);
                    bytes.readPosition(limit2);
                    popState();
                }
            } else {
                throw new IORuntimeException("Length unknown " + length);
            }
            return object;
        }

        /**
         * Deserializes an object of the given class from the wire data.
         *
         * @param clazz Class to be deserialized.
         * @return An instance of the provided class.
         * @throws BufferUnderflowException if there's not enough data in the buffer.
         * @throws IORuntimeException for general IO issues.
         */
        @Nullable
        public Demarshallable demarshallable(@NotNull Class<? extends Demarshallable> clazz) throws BufferUnderflowException, IORuntimeException {
            if (this.isNull())
                return null;

            long length = readLength();  // Read the expected length of the data.
            if (length >= 0) {  // If length is valid, proceed.
                long limit = bytes.readLimit();
                long limit2 = bytes.readPosition() + length;
                bytes.readLimit(limit2);  // Set a temporary limit for the buffer based on the expected length.
                try {
                    return Demarshallable.newInstance(clazz, wireIn());  // Deserialize the object using the current wire input.
                } finally {
                    bytes.readLimit(limit);  // Reset the buffer limit.
                    bytes.readPosition(limit2);  // Move the buffer's position past the read data.
                }
            } else {  // If length is not valid (negative), still try to deserialize the object.
                return Demarshallable.newInstance(clazz, wireIn());
            }
        }

        /**
         * Reads a text from the wire and attempts to convert it into a long value.
         * If the text is not a valid representation of a long, tries to parse it as a double
         * and then rounds to the nearest long.
         *
         * @param otherwise Default value to return if the conversion fails.
         * @return Parsed long value or the provided default value.
         * @throws IORuntimeException for general IO issues.
         * @throws BufferUnderflowException if there's not enough data in the buffer.
         */
        private long readTextAsLong(long otherwise) throws IORuntimeException, BufferUnderflowException {
            bytes.uncheckedReadSkipBackOne();  // Go back one position.
            @Nullable String text;
            try {
                text = text();  // Read text from the wire.
            } catch (Exception e) {
                return otherwise;  // On any exception, return the default value.
            }
            if (text == null || text.length() == 0)  // If text is empty or null, return the default value.
                return otherwise;
            try {
                return Long.parseLong(text);  // Try to parse the text as a long.
            } catch (NumberFormatException e) {
                return Math.round(Double.parseDouble(text));  // If failed, try parsing as double and then round it.
            }
        }

        /**
         * Reads a text from the wire and attempts to convert it into a double value.
         *
         * @return Parsed double value or NaN if the conversion fails.
         * @throws IORuntimeException for general IO issues.
         * @throws BufferUnderflowException if there's not enough data in the buffer.
         */
        private double readTextAsDouble() throws IORuntimeException, BufferUnderflowException {
            bytes.uncheckedReadSkipBackOne();  // Go back one position.
            @Nullable String text;
            try {
                text = text();  // Read text from the wire.
            } catch (BufferUnderflowException e) {
                return Double.NaN; // On buffer underflow, return NaN.
            }
            if (text == null || text.length() == 0)  // If text is empty or null, return NaN.
                return Double.NaN;
            return Double.parseDouble(text);  // Try to parse the text as a double.
        }

        @Override
        public boolean bool() throws IORuntimeException {
            int code = readCode();
            if (isText(code))
                return Boolean.valueOf(text());

            switch (code) {
                case TRUE:
                    return true;
                case FALSE:
                    return false;

                case PADDING:
                case PADDING32:
                case COMMENT:
                    bytes.uncheckedReadSkipBackOne();
                    consumePadding();
                    return bool();
            }
            throw new IORuntimeException(stringForCode(code));
        }

        @Override
        public byte int8() {
            int code = readCode();
            if (code < 128)
                return (byte) code;
            return int8b(code);
        }

        /**
         * Reads an 8-bit integer value based on a given code.
         *
         * @param code The code representing the type of data.
         * @return The byte value read.
         */
        private byte int8b(int code) {
            // Handle padding or comment codes by skipping back and consuming the padding,
            // then reading the next code.
            switch (code) {
                case PADDING:
                case PADDING32:
                case COMMENT:
                    bytes.uncheckedReadSkipBackOne();  // Move back by one position.
                    consumePadding();  // Handle the padding or comment.
                    code = readCode();  // Read the next code.
                    break;
            }

            // Determine if the code represents text or an integer value.
            final long value = isText(code) ? readTextAsLong(Byte.MIN_VALUE) : readInt0(code);

            // Ensure the value is within the byte range, otherwise throw an exception.
            if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE)
                throw new IllegalStateException();
            return (byte) value; // Cast the long value to a byte and return.
        }

        @Override
        public short int16() {
            consumePadding();
            int code = readCode();
            final long value = isText(code) ? readTextAsLong(Short.MIN_VALUE) : readInt0(code);
            if (value > Short.MAX_VALUE || value < Short.MIN_VALUE)
                throw new IllegalStateException();
            return (short) value;
        }

        @Override
        public int uint16() {
            consumePadding();
            int code = readCode();

            final long value = isText(code) ? readTextAsLong(0) : readInt0(code);

            if (value > (1L << 32L) || value < 0)
                throw new IllegalStateException("value " + value + " cannot be cast to an unsigned 16-bit int without loss of information");

            return (int) value;

        }

        @Override
        public float float32() {
            consumePadding();
            int code = readCode();
            final double value;
            switch (code >> 16) {
                case BinaryWireHighCode.INT:
                    value = readInt0(code);
                    break;
                case BinaryWireHighCode.FLOAT:
                    value = readFloat0(code);
                    break;
                case BinaryWireHighCode.STR0:
                case BinaryWireHighCode.STR1:
                default:
                    value = readTextAsDouble();
                    break;
            }

            return (float) value;
        }

        @Override
        public int int32() {
            consumePadding();
            int code = readCode();
            final long value = isText(code) ? readTextAsLong(Integer.MIN_VALUE) : readInt0(code);

            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE)
                throw new IllegalStateException("value " + value + " cannot be cast to int without loss of information");

            return (int) value;
        }

        @Override
        public long int64() {
            int code = readCode();
            if (code == PADDING || code == PADDING32 || code == COMMENT) {
                bytes.uncheckedReadSkipBackOne();
                consumePadding();
                code = readCode();
            }
            if ((code & 0x80) == 0)
                return code;

            switch (code >> 4) {
                case BinaryWireHighCode.FLOAT:
                    return (long) readFloat0(code);
                case BinaryWireHighCode.INT:
                    return readInt0(code);
                default:
                    return readTextAsLong(Long.MIN_VALUE);
            }
        }

        @Override
        public double float64() {
            int code = readCode();
            if (code >> 4 == BinaryWireHighCode.FLOAT)
                return readFloat0(code);
            return isText(code) ? readTextAsDouble() : readInt0(code);
        }

        /**
         * Throws an exception indicating that reading the provided code is not supported.
         *
         * @param code The unsupported code.
         * @return A runtime exception (never actually returned since an exception is always thrown).
         */
        @NotNull
        private RuntimeException cantRead(int code) {
            throw new UnsupportedOperationException(stringForCode(code));
        }

        @Override
        public Object objectWithInferredType(Object using, @NotNull SerializationStrategy strategy, Class<?> type) throws InvalidMarshallableException {
            int code = peekCode();
            if ((code & 0x80) == 0) {
                bytes.uncheckedReadSkipOne();
                return code;
            }
            switch (code >> 4) {
                case BinaryWireHighCode.CONTROL:
                    switch (code) {
                        case BYTES_LENGTH8:
                        case BYTES_LENGTH16:
                        case BYTES_LENGTH32: {
                            if (using instanceof StringBuilder) {
                                bytesStore((StringBuilder) using);
                                return using;
                            } else if (using instanceof Bytes) {
                                bytesStore((Bytes) using);
                                return using;
                            } else {
                                long pos = bytes.readPosition();
                                bytes.uncheckedReadSkipOne();
                                int len = readLength(code);
                                code = peekCode();
                                if (code == U8_ARRAY) {
                                    bytes.readPosition(pos);
                                    return bytesStore();
                                }
                                long lim = bytes.readLimit();
                                try {
                                    bytes.readLimit(bytes.readPosition() + len);
                                    Object using1 = using;
                                    if (using1 == null && type != null)
                                        using1 = strategy.newInstanceOrNull(type);
                                    if (isEvent(code))
                                        return (strategy == SerializationStrategies.ANY_OBJECT ? SerializationStrategies.MAP : strategy)
                                                .readUsing(type, using1, this, BracketType.MAP);
                                    else
                                        return (strategy == SerializationStrategies.ANY_OBJECT ? SerializationStrategies.LIST : strategy)
                                                .readUsing(type, using1, this, BracketType.SEQ);

                                } finally {
                                    bytes.readLimit(lim);
                                }
                            }
                        }
                        case U8_ARRAY: {
                            bytes.uncheckedReadSkipOne();
                            long length = bytes.readRemaining();
                            if (length == 0)
                                return BytesStore.empty();
                            @NotNull BytesStore<?, ?> toBytes = BytesStore.lazyNativeBytesStoreWithFixedCapacity(length);
                            toBytes.write(0, bytes, bytes.readPosition(), length);
                            bytes.readSkip(length);
                            return toBytes;
                        }

                        case ANCHOR:
                        case UPDATED_ALIAS:
                            return typedMarshallable();

                    }
                    break;
                case BinaryWireHighCode.SPECIAL:
                    switch (code) {
                        case FALSE:
                            bytes.uncheckedReadSkipOne();
                            return Boolean.FALSE;
                        case TRUE:
                            bytes.uncheckedReadSkipOne();
                            return Boolean.TRUE;
                        case NULL:
                            bytes.uncheckedReadSkipOne();
                            return null;
                        case STRING_ANY:
                            return text();
                        case TYPE_PREFIX: {
                            readCode();
                            @Nullable StringBuilder sb = readUtf8();
                            final Class<?> clazz2 = classLookup().forName(sb);
                            return object(null, clazz2);
                        }
                        case EVENT_OBJECT: {
                            if (using == null) {
                                strategy = SerializationStrategies.MAP;
                                using = strategy.newInstanceOrNull(null);
                            }

                            strategy.readUsing(type, using, valueIn, BracketType.MAP);
                            return ObjectUtils.convertTo(type, using);
                        }
                        case TIME:
                            return time();

                        case DATE:
                            return date();

                        case DATE_TIME:
                            return dateTime();

                        case ZONED_DATE_TIME:
                            return zonedDateTime();

                        case TYPE_LITERAL:
                            return typeLiteral();

                    }
                    break;

                case BinaryWireHighCode.FLOAT:
                    bytes.uncheckedReadSkipOne();
                    return readFloat0bject(code);

                case BinaryWireHighCode.INT:
                    bytes.uncheckedReadSkipOne();
                    if (code == UUID)
                        return new java.util.UUID(bytes.readLong(), bytes.readLong());
                    return readInt0object(code);
            }
            // assume it a String
            return text();
        }

        /**
         * Reads the length of the data based on a given code.
         *
         * @param code The code representing the length format.
         * @return The length value read.
         */
        private int readLength(int code) {
            int len;
            // Determine the code and read the corresponding length.
            switch (code) {
                case BYTES_LENGTH8:
                    len = bytes.readUnsignedByte();  // Read an unsigned 8-bit value.
                    break;
                case BYTES_LENGTH16:
                    len = bytes.readUnsignedShort();  // Read an unsigned 16-bit value.
                    break;
                case BYTES_LENGTH32:
                    len = bytes.readInt();  // Read a 32-bit integer value.
                    break;
                default:
                    throw new AssertionError(); // Throw an assertion error if the code is unrecognized.
            }
            return len;  // Return the read length.
        }

        /**
         * Checks if the provided code corresponds to an event.
         *
         * @param code The code to check.
         * @return True if the code corresponds to an event, otherwise false.
         */
        private boolean isEvent(int code) {
            // Return true if the code matches the EVENT_NAME or falls within the range of field names.
            return code == EVENT_NAME || (FIELD_NAME0 <= code && code <= FIELD_NAME31);
        }

        /**
         * Consumes the next set of bytes based on the provided code.
         * The method moves the reader pointer after the current item or structure.
         *
         * @throws InvalidMarshallableException If there's an error while marshalling.
         */
        void consumeNext() throws InvalidMarshallableException {
            // Peek at the next byte to determine the code.
            int code = peekCode();

            // If the highest bit of the code isn't set, skip the current byte.
            if ((code & 0x80) == 0) {
                bytes.uncheckedReadSkipOne();
                return;
            }

            // Check the high 4 bits to determine the category of the code.
            switch (code >> 4) {
                case BinaryWireHighCode.CONTROL:
                    switch (code) {
                        // For lengths, skip the bytes that define the length and then
                        // skip the number of bytes specified by the length.
                        case BYTES_LENGTH8:
                            bytes.readSkip(1);  // Skip length definition byte.
                            bytes.readSkip(bytes.readUnsignedByte());  // Skip the specified bytes.
                            return;

                        case BYTES_LENGTH16:
                            bytes.readSkip(1);
                            bytes.readSkip(bytes.readUnsignedShort());
                            return;

                        case BYTES_LENGTH32:
                            bytes.readSkip(1);
                            bytes.readSkip(bytes.readUnsignedInt());

                            return;

                        default:
                            // Warn if an unrecognized control code is encountered.
                            Jvm.warn().on(getClass(), "reading control code as text");
                    }
                    break;
                case BinaryWireHighCode.SPECIAL:
                    switch (code) {
                        // For boolean values or null, just skip the code byte.
                        case FALSE:
                        case TRUE:
                        case NULL:
                            bytes.uncheckedReadSkipOne();
                            return;

                        // For any string type, read the text.
                        case STRING_ANY:
                            text();
                            return;

                        // For type prefix, skip the code byte, read the UTF-8 string,
                        // and then recursively consume the next set of bytes.
                        case TYPE_PREFIX: {
                            readCode();
                            readUtf8();
                            consumeNext();
                            return;
                        }
                    }
                    break;

                // Handling floating point numbers
                case BinaryWireHighCode.FLOAT:
                    bytes.uncheckedReadSkipOne();  // Skip the current byte
                    // If the code is a small integer (between 0 and 127), nothing more to do
                    if (code < 128 && code >= 0) {
                        return;
                    }

                    // copy/pasted from readFloat0bject to avoid auto-boxing
                    switch (code) {
                        case FLOAT32:
                            bytes.readFloat();  // Read a 32-bit float
                            return;
                        // The next set of cases are special encoding mechanisms (StopBit)
                        case FLOAT_STOP_2:
                            bytes.readStopBit();
                            return;
                        case FLOAT_STOP_4:
                            bytes.readStopBit();
                            return;
                        case FLOAT_STOP_6:
                            bytes.readStopBit();  // Read a number encoded in StopBit format
                            return;
                        case FLOAT64:
                            bytes.readDouble();  // Read a 64-bit double
                            return;
                        // Handling optimized cases for specific ranges of floats
                        case FLOAT_SET_LOW_0:
                        case FLOAT_SET_LOW_2:
                        case FLOAT_SET_LOW_4:
                            bytes.readUnsignedByte();  // Read a byte and treat it as unsigned
                            return;
                    }
                    // If none of the known float codes were matched, throw an exception
                    throw new UnsupportedOperationException(stringForCode(code));

                // Handling integers
                case BinaryWireHighCode.INT:
                    bytes.uncheckedReadSkipOne();  // Skip the current byte
                    // Check if the integer can be represented by a small int (this method would be defined elsewhere)
                    if (isSmallInt(code))
                        return;

                    // The following is optimized code to read various integer representations without converting them to actual integers (avoiding auto-boxing)
                    switch (code) {
                        case INT8:
                            bytes.readByte();  // Read an 8-bit signed integer
                            return;
                        case UINT8:
                        case SET_LOW_INT8:
                            bytes.readUnsignedByte();  // Read an 8-bit unsigned integer
                            return;
                        case INT16:
                            bytes.readShort();  // Read a 16-bit signed integer
                            return;
                        case SET_LOW_INT16:
                        case UINT16:
                            bytes.readUnsignedShort();  // Read a 16-bit unsigned integer
                            return;
                        case INT32:
                            bytes.readInt();  // Read a 32-bit signed integer
                            return;
                        case UINT32:
                            bytes.readUnsignedInt();  // Read a 32-bit unsigned integer
                            return;
                        case INT64:
                        case INT64_0x:
                            bytes.readLong();  // Read a 64-bit signed integer
                            return;
                    }
                    // If none of the known integer codes were matched, throw an exception
                    throw new UnsupportedOperationException(stringForCode(code));
            }
            // If the code doesn't match any known pattern, assume it's a text encoding
            text();
        }
    }
}
