/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.affinity.Affinity;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.VanillaBytes;
import net.openhft.chronicle.core.ClassLocal;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.threads.ThreadLocalHelper;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.core.util.ReadResolvable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static net.openhft.chronicle.core.util.ReadResolvable.readResolve;
import static net.openhft.chronicle.wire.SerializationStrategies.*;
import static net.openhft.chronicle.wire.WireType.TEXT;

/*
 * Created by Peter Lawrey on 31/08/15.
 */
public enum Wires {
    ;
    public static final int LENGTH_MASK = -1 >>> 2;
    public static final int NOT_COMPLETE = 0x8000_0000;
    @Deprecated
    public static final int NOT_READY = NOT_COMPLETE;
    public static final int META_DATA = 1 << 30;
    public static final int UNKNOWN_LENGTH = 0x0;
    // public static final int MAX_LENGTH = (1 << 30) - 1;
    // value to use when the message is not ready and of an unknown length
    public static final int NOT_COMPLETE_UNKNOWN_LENGTH = NOT_COMPLETE;
    // value to use when no more data is possible e.g. on a roll.
    public static final int END_OF_DATA = NOT_COMPLETE | META_DATA;
    public static final int NOT_INITIALIZED = 0x0;
    public static final Bytes<?> NO_BYTES = new VanillaBytes<>(BytesStore.empty());
    //public static final WireIn EMPTY = new BinaryWire(NO_BYTES);
    public static final int SPB_HEADER_SIZE = 4;
    public static final List<Function<Class, SerializationStrategy>> CLASS_STRATEGY_FUNCTIONS = new CopyOnWriteArrayList<>();
    public static final ClassLocal<SerializationStrategy> CLASS_STRATEGY = ClassLocal.withInitial(c -> {
        for (@NotNull Function<Class, SerializationStrategy> func : CLASS_STRATEGY_FUNCTIONS) {
            final SerializationStrategy strategy = func.apply(c);
            if (strategy != null)
                return strategy;
        }
        return ANY_OBJECT;
    });
    static final ClassLocal<Function<String, Marshallable>> MARSHALLABLE_FUNCTION = ClassLocal.withInitial(tClass -> {
        Class[] interfaces = {Marshallable.class, tClass};
        if (tClass == Marshallable.class)
            interfaces = new Class[]{Marshallable.class};
        Class<?> proxyClass = Proxy.getProxyClass(tClass.getClassLoader(), interfaces);
        try {
            Constructor<?> constructor = proxyClass.getConstructor(InvocationHandler.class);
            return typeName -> newInstance(constructor, typeName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    });
    static final ClassLocal<FieldInfoPair> FIELD_INFOS = ClassLocal.withInitial(VanillaFieldInfo::lookupClass);
    static final StringBuilderPool SBP = new StringBuilderPool();
    private static final int TID_MASK = 0b00111111_11111111_11111111_11111111;
    private static final int INVERSE_TID_MASK = ~TID_MASK;
    static boolean ENCODE_TID_IN_HEADER = Boolean.getBoolean("wire.encodeTidInHeader");

    static {
        CLASS_STRATEGY_FUNCTIONS.add(SerializeEnum.INSTANCE);
        CLASS_STRATEGY_FUNCTIONS.add(SerializeJavaLang.INSTANCE);
        CLASS_STRATEGY_FUNCTIONS.add(SerializeBytes.INSTANCE);
        CLASS_STRATEGY_FUNCTIONS.add(SerializeMarshallables.INSTANCE); // must be after SerializeBytes.
        WireInternal.addAliases();
    }

    @Deprecated(/*to be removed?*/)
    @Nullable
    public static <T> T read(@NotNull Class<T> tClass, ValueIn in) {
        final SerializationStrategy<T> strategy = CLASS_STRATEGY.get(tClass);
        return strategy.read(in, tClass);
    }

    /**
     * This decodes some Bytes where the first 4-bytes is the length.  e.g. Wire.writeDocument wrote
     * it. <a href="https://github.com/OpenHFT/RFC/tree/master/Size-Prefixed-Blob">Size Prefixed
     * Blob</a>
     *
     * @param bytes to decode
     * @return as String
     */
    public static String fromSizePrefixedBlobs(@NotNull Bytes bytes) {
        return WireDumper.of(bytes).asString();
    }

    public static String fromSizePrefixedBlobs(@NotNull Bytes bytes, long position) {
        final long limit = bytes.readLimit();
        if (position > limit)
            return "";
        return WireDumper.of(bytes).asString(position, limit - position);
    }

    public static String fromSizePrefixedBlobs(@NotNull DocumentContext dc) {
        Wire wire = dc.wire();
        Bytes<?> bytes = wire.bytes();
        if (wire instanceof TextWire) {
            return bytes.toString();
        }

        long headerPosition;

        long length;
        if ("BufferedTailer".equals(dc.getClass().getSimpleName())) {
            length = wire.bytes().readLimit();
            int metaDataBit = dc.isMetaData() ? Wires.META_DATA : 0;
            int header = metaDataBit | toIntU30(length, "Document length %,d out of 30-bit int range.");

            Bytes tempBytes = Bytes.allocateElasticDirect();
            try {
                tempBytes.writeOrderedInt(header);
                tempBytes.write(((BinaryReadDocumentContext) dc).wire.bytes, 0, ((BinaryReadDocumentContext) dc).wire.bytes.readLimit());

                final WireType wireType = WireType.valueOf(wire);

                assert wireType != null;
                Wire tempWire = wireType.apply(tempBytes);

                return WireDumper.of(tempWire).asString(0, length + 4);

            } finally {
                tempBytes.release();
            }

        } else {
            if (dc instanceof BinaryReadDocumentContext) {
                long start = ((BinaryReadDocumentContext) dc).lastStart;
                if (start != -1)
                    headerPosition = start;
                else
                    headerPosition = bytes.readPosition() - 4;
            } else
                headerPosition = bytes.readPosition() - 4;

            length = Wires.lengthOf(bytes.readInt(headerPosition));
        }

        return WireDumper.of(wire).asString(headerPosition, length + 4);
    }

    public static String fromSizePrefixedBlobs(@NotNull WireIn wireIn) {
        return WireDumper.of(wireIn).asString();
    }

    @NotNull
    public static CharSequence asText(@NotNull WireIn wireIn) {
        long pos = wireIn.bytes().readPosition();
        try {
            Bytes bytes = acquireBytes();
            wireIn.copyTo(new TextWire(bytes).addTimeStamps(true));
            return bytes;
        } finally {
            wireIn.bytes().readPosition(pos);
        }
    }

    public static StringBuilder acquireStringBuilder() {
        return SBP.acquireStringBuilder();
    }

    public static int lengthOf(int len) {
        if (isNotComplete(len) && ENCODE_TID_IN_HEADER) {
            return Wires.removeMaskedTidFromHeader(len) & LENGTH_MASK;
        }
        return len & LENGTH_MASK;
    }

    public static boolean isReady(int header) {
        return (header & NOT_COMPLETE) == 0 && header != 0;
    }

    public static boolean isNotComplete(int header) {
        return (header & NOT_COMPLETE) != 0 || header == 0;
    }

    public static boolean isReadyData(int header) {
        return ((header & (META_DATA | NOT_COMPLETE)) == 0) && (header != 0);
    }

    @Deprecated
    public static boolean isData(long len) {
        return isData((int) len);
    }

    public static boolean isData(int len) {
        return (len & META_DATA) == 0;
    }

    public static boolean isReadyMetaData(int len) {
        return (len & (META_DATA | NOT_COMPLETE)) == META_DATA;
    }

    public static boolean isKnownLength(int len) {
        return (len & (META_DATA | LENGTH_MASK)) != UNKNOWN_LENGTH;
    }

    public static boolean isNotInitialized(int len) {
        return len == NOT_INITIALIZED;
    }

    public static int toIntU30(long l, @NotNull String error) {
        if (l < 0 || l > LENGTH_MASK)
            throw new IllegalStateException(String.format(error, l));
        return (int) l;
    }

    public static boolean acquireLock(@NotNull BytesStore store, long position) {
        return store.compareAndSwapInt(position, NOT_INITIALIZED, NOT_COMPLETE);
    }

    public static boolean exceedsMaxLength(long length) {
        return length > LENGTH_MASK;
    }

    @ForceInline
    public static <T extends WriteMarshallable> long writeData(
            @NotNull WireOut wireOut,
            @NotNull T writer) {
        return WireInternal.writeData(wireOut, false, false, writer);
    }

    @ForceInline
    public static long readWire(@NotNull WireIn wireIn, long size, @NotNull ReadMarshallable readMarshallable) {
        @NotNull final Bytes<?> bytes = wireIn.bytes();
        final long limit0 = bytes.readLimit();
        final long limit = bytes.readPosition() + size;
        try {
            bytes.readLimit(limit);
            readMarshallable.readMarshallable(wireIn);
        } finally {
            bytes.readLimit(limit0);
            bytes.readPosition(limit);
        }

        return bytes.readPosition();
    }

    @NotNull
    public static Bytes<?> acquireBytes() {
        Bytes bytes = ThreadLocalHelper.getTL(WireInternal.BYTES_TL, Bytes::allocateElasticDirect);
        bytes.clear();
        return bytes;
    }

    @NotNull
    static Bytes<?> acquireBytesForToString() {
        Bytes bytes = ThreadLocalHelper.getTL(WireInternal.BYTES_F2S_TL, Bytes::allocateElasticDirect);
        bytes.clear();
        return bytes;
    }

    @NotNull
    public static Wire acquireBinaryWire() {
        Wire wire = ThreadLocalHelper.getTL(WireInternal.BINARY_WIRE_TL, () -> new BinaryWire(acquireBytes()));
        wire.clear();
        return wire;
    }

    @NotNull
    public static Bytes acquireAnotherBytes() {
        Bytes bytes = ThreadLocalHelper.getTL(WireInternal.BYTES_TL, Bytes::allocateElasticDirect);
        bytes.clear();
        return bytes;
    }

    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes, long position, long length) {
        return WireDumper.of(bytes).asString(position, length);
    }

    public static void readMarshallable(@NotNull Object marshallable, @NotNull WireIn wire, boolean overwrite) {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass());
        wm.readMarshallable(marshallable, wire, wm.defaultValue(), overwrite);
    }

    public static void writeMarshallable(@NotNull Object marshallable, @NotNull WireOut wire) {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass());
        wm.writeMarshallable(marshallable, wire);
    }

    public static void writeMarshallable(@NotNull Object marshallable, @NotNull WireOut wire, boolean writeDefault) {
        WireMarshaller marshaller = WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass());
        if (writeDefault)
            marshaller.writeMarshallable(marshallable, wire);
        else
            marshaller.writeMarshallable(marshallable, wire, marshaller.defaultValue(), false);
    }

    public static void writeMarshallable(@NotNull Object marshallable, @NotNull WireOut wire, @NotNull Object previous, boolean copy) {
        assert marshallable.getClass() == previous.getClass();
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass());
        wm.writeMarshallable(marshallable, wire, previous, copy);
    }

    public static void writeKey(@NotNull Object marshallable, Bytes bytes) {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass());
        wm.writeKey(marshallable, bytes);
    }

    @NotNull
    public static <T extends Marshallable> T deepCopy(@NotNull T marshallable) {
        Wire wire = acquireBinaryWire();
        marshallable.writeMarshallable(wire);
        @NotNull T t = (T) ObjectUtils.newInstance(marshallable.getClass());
        t.readMarshallable(wire);
        return t;
    }

    @NotNull
    public static <T> T copyTo(Object source, @NotNull T target) {
        Wire wire = acquireBinaryWire();
        wire.getValueOut().object(source);
        wire.getValueIn().typePrefix(); // drop the type prefix.
        wire.getValueIn().object(target, target.getClass());
        return target;
    }

    @NotNull
    public static <T> T project(Class<T> tClass, Object source) {
        T target = ObjectUtils.newInstance(tClass);
        Wires.copyTo(source, target);
        return target;
    }

    public static boolean isEquals(@NotNull Object o1, @NotNull Object o2) {
        return o1.getClass() == o2.getClass() && WireMarshaller.WIRE_MARSHALLER_CL.get(o1.getClass()).isEqual(o1, o2);
    }

    @NotNull
    public static List<FieldInfo> fieldInfos(@NotNull Class aClass) {
        return FIELD_INFOS.get(aClass).list;
    }

    public static FieldInfo fieldInfo(@NotNull Class aClass, String name) {
        return FIELD_INFOS.get(aClass).map.get(name);
    }

    public static boolean isEndOfFile(int num) {
        return num == END_OF_DATA;
    }

    @Nullable
    public static <T> T getField(@NotNull Object o, String name, Class<T> tClass) throws NoSuchFieldException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(o.getClass());
        Object value = wm.getField(o, name);
        return ObjectUtils.convertTo(tClass, value);
    }

    public static void setField(@NotNull Object o, String name, Object value) throws NoSuchFieldException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(o.getClass());
        wm.setField(o, name, value);
    }

    public static void reset(@NotNull Object o) {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(o.getClass());
        if (o instanceof ResetOverride) {
            ((ResetOverride) o).onReset();
        } else {
            wm.reset(o);
        }
    }

    public static int addMaskedTidToHeader(final int header) {
        return ENCODE_TID_IN_HEADER ? header | (TID_MASK & Affinity.getThreadId()) : header;
    }

    public static int removeMaskedTidFromHeader(final int header) {
        return header & INVERSE_TID_MASK;
    }

    public static int extractTidFromHeader(final int header) {
        return header & TID_MASK;
    }

    @Nullable
    public static <E> E objectSequence(ValueIn in, @Nullable E using, @Nullable Class clazz, SerializationStrategy<E> strategy) {
        if (clazz == Object.class)
            strategy = LIST;
        if (using == null)
            using = (E) strategy.newInstance(clazz);

        return in.sequence(using, strategy::readUsing) ? readResolve(using) : null;
    }

    @Nullable
    public static <E> E objectMap(ValueIn in, @Nullable E using, @Nullable Class clazz, SerializationStrategy<E> strategy) {
        if (clazz == Object.class)
            strategy = MAP;
        if (using == null)
            using = (E) strategy.newInstance(clazz);
        if (Throwable.class.isAssignableFrom(clazz))
            return (E) WireInternal.throwable(in, false, (Throwable) using);

        if (using == null)
            throw new IllegalStateException("failed to create instance of clazz=" + clazz + " is it aliased?");

        @Nullable Object ret = in.marshallable(using, strategy);
        return readResolve(ret);
    }

    @NotNull
    public static <E> E objectDate(ValueIn in, @Nullable E using) {
        // skip the field if it is there.
        in.wireIn().read();
        final long time = in.int64();
        if (using instanceof Date) {
            ((Date) using).setTime(time);
            return using;
        } else
            return (E) new Date(time);
    }

    @Nullable
    public static <E> E object0(ValueIn in, @Nullable E using, @Nullable Class clazz) {
        Object o = in.typePrefixOrObject(clazz);
        if (o != null && !(o instanceof Class)) {
            return (E) in.marshallable(o, MARSHALLABLE);
        }
        @Nullable final Class clazz2 = (Class) o;
        if (clazz2 == void.class) {
            in.text();
            return null;
        } else if (clazz2 == BytesStore.class) {
            if (using == null)
                using = (E) Bytes.elasticHeapByteBuffer(32);
            clazz = Base64.class;
        }
        if (clazz2 != null &&
                clazz != clazz2 &&
                (clazz == null
                        || clazz.isAssignableFrom(clazz2)
                        || ReadResolvable.class.isAssignableFrom(clazz2)
                        || !ObjectUtils.isConcreteClass(clazz))) {
            clazz = clazz2;
            if (!clazz.isInstance(using))
                using = null;
        }
        if (clazz == null)
            clazz = Object.class;
        SerializationStrategy<E> strategy = CLASS_STRATEGY.get(clazz);
        BracketType brackets = strategy.bracketType();
        if (brackets == BracketType.UNKNOWN)
            brackets = in.getBracketType();

        if (Date.class.isAssignableFrom(clazz))
            return objectDate(in, using);

        if (BitSet.class.isAssignableFrom(clazz)) {

            PrimArrayWrapper longWrapper = new PrimArrayWrapper(long[].class);
            objectSequence(in, longWrapper, PrimArrayWrapper.class, PRIM_ARRAY);

            return (using == null) ?
                    (E) BitSet.valueOf((long[]) longWrapper.array) :
                    (E) BitSetUtil.set((BitSet) using, (long[]) longWrapper.array);

        }

        switch (brackets) {
            case MAP:
                return objectMap(in, using, clazz, strategy);

            case SEQ:
                return objectSequence(in, using, clazz, strategy);

            case NONE:
                @NotNull final Object e = strategy.readUsing(using, in);
                return clazz == Base64.class
                        ? (E) e
                        : (E) ObjectUtils.convertTo(clazz, e);

            default:
                throw new AssertionError();
        }
    }

    public static boolean dtoInterface(Class clazz) {
        return clazz != null
                && clazz.isInterface()
                && clazz != Bytes.class
                && clazz != BytesStore.class
                && !clazz.getPackage().getName().startsWith("java");
    }

    public static String typeNameFor(@NotNull Object value) {
        return value instanceof Marshallable ? ((Marshallable) value).getClassName() : ClassAliasPool.CLASS_ALIASES.nameFor(value.getClass());
    }

    static Marshallable newInstance(Constructor constructor, String typeName) {
        try {
            return (Marshallable) constructor.newInstance(new TupleInvocationHandler(typeName));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> T tupleFor(Class<T> tClass, String typeName) {
        if (tClass == null || tClass == Object.class)
            tClass = (Class<T>) Marshallable.class;
        return (T) MARSHALLABLE_FUNCTION.get(tClass).apply(typeName);
    }

    public static boolean encodeTidInHeader() {
        return ENCODE_TID_IN_HEADER;
    }

    public static void encodeTidInHeader(boolean encodeTidInHeader) {
        ENCODE_TID_IN_HEADER = encodeTidInHeader;
    }

    enum SerializeEnum implements Function<Class, SerializationStrategy> {
        INSTANCE;

        @Nullable
        static SerializationStrategy getSerializationStrategy(@NotNull Class aClass) {
            if (Enum.class.isAssignableFrom(aClass))
                return ENUM;
            return null;
        }

        @Nullable
        @Override
        public SerializationStrategy apply(@NotNull Class aClass) {
            return getSerializationStrategy(aClass);
        }
    }

    enum SerializeJavaLang implements Function<Class, SerializationStrategy> {
        INSTANCE;

        @Override
        public SerializationStrategy apply(@NotNull Class aClass) {
            switch (aClass.getName()) {
                case "[B":
                    return ScalarStrategy.of(byte[].class, (o, in) -> in.bytes());

                case "java.lang.StringBuilder":
                    return ScalarStrategy.of(StringBuilder.class, (o, in) -> {
                        StringBuilder builder = (o == null)
                                ? acquireStringBuilder()
                                : o;
                        in.textTo(builder);
                        return o;
                    });

                case "java.lang.String":
                    return ScalarStrategy.of(String.class, (o, in) -> in.text());

                case "java.lang.Object":
                    return ANY_OBJECT;

                case "java.lang.Class":
                    return ScalarStrategy.of(Class.class, (o, in) -> {
                        try {
                            return ClassAliasPool.CLASS_ALIASES.forName(in.text());
                        } catch (ClassNotFoundException e) {
                            throw new IORuntimeException(e);
                        }
                    });

                case "java.lang.Boolean":
                    return ScalarStrategy.of(Boolean.class, (o, in) -> in.bool());

                case "java.lang.Byte":
                    return ScalarStrategy.of(Byte.class, (o, in) -> in.int8());

                case "java.lang.Short":
                    return ScalarStrategy.of(Short.class, (o, in) -> in.int16());

                case "java.lang.Character":
                    return ScalarStrategy.of(Character.class, (o, in) -> {
                        //noinspection unchecked
                        @Nullable final String text = in.text();
                        if (text == null || text.length() == 0)
                            return null;
                        return text.charAt(0);
                    });

                case "java.lang.Integer":
                    return ScalarStrategy.of(Integer.class, (o, in) -> in.int32());

                case "java.lang.Float":
                    return ScalarStrategy.of(Float.class, (o, in) -> in.float32());

                case "java.lang.Long":
                    return ScalarStrategy.of(Long.class, (o, in) -> in.int64());

                case "java.lang.Double":
                    return ScalarStrategy.of(Double.class, (o, in) -> in.float64());

                case "java.time.LocalTime":
                    return ScalarStrategy.of(LocalTime.class, (o, in) -> in.time());

                case "java.time.LocalDate":
                    return ScalarStrategy.of(LocalDate.class, (o, in) -> in.date());

                case "java.time.LocalDateTime":
                    return ScalarStrategy.of(LocalDateTime.class, (o, in) -> in.dateTime());

                case "java.time.ZonedDateTime":
                    return ScalarStrategy.of(ZonedDateTime.class, (o, in) -> in.zonedDateTime());

                case "java.io.File":
                    return ScalarStrategy.text(File.class, File::new);

                case "java.util.UUID":
                    return ScalarStrategy.of(UUID.class, (o, in) -> in.uuid());

                case "java.math.BigInteger":
                    return ScalarStrategy.text(BigInteger.class, BigInteger::new);

                case "java.math.BigDecimal":
                    return ScalarStrategy.text(BigDecimal.class, BigDecimal::new);

                default:
                    if (aClass.isPrimitive())
                        return ANY_SCALAR;
                    if (aClass.isArray()) {
                        final Class componentType = aClass.getComponentType();
                        if (componentType.isPrimitive())
                            return PRIM_ARRAY;
                        return ARRAY;
                    }
                    if (Enum.class.isAssignableFrom(aClass)) {
                        @Nullable final SerializationStrategy ss = SerializeMarshallables.getSerializationStrategy(aClass);
                        return ss == null ? ENUM : ss;
                    }
                    return null;
            }
        }
    }

    enum SerializeMarshallables implements Function<Class, SerializationStrategy> {
        INSTANCE;

        @Nullable
        static SerializationStrategy getSerializationStrategy(@NotNull Class aClass) {
            if (Demarshallable.class.isAssignableFrom(aClass))
                return DEMARSHALLABLE;
            if (ReadMarshallable.class.isAssignableFrom(aClass))
                return MARSHALLABLE;
            return null;
        }

        @Override
        public SerializationStrategy apply(@NotNull Class aClass) {
            @Nullable SerializationStrategy x = getSerializationStrategy(aClass);
            if (x != null) return x;
            if (Map.class.isAssignableFrom(aClass))
                return MAP;
            if (Set.class.isAssignableFrom(aClass))
                return SET;
            if (List.class.isAssignableFrom(aClass))
                return LIST;
            if (Externalizable.class.isAssignableFrom(aClass))
                return EXTERNALIZABLE;
            if (Serializable.class.isAssignableFrom(aClass))
                return ANY_NESTED;
            return ANY_SCALAR;
        }
    }

    enum SerializeBytes implements Function<Class, SerializationStrategy> {
        INSTANCE;

        static Bytes decodeBase64(Bytes o, ValueIn in) {
            @NotNull StringBuilder sb0 = acquireStringBuilder();
            in.text(sb0);
            String s = WireInternal.INTERNER.intern(sb0);
            byte[] decode = Base64.getDecoder().decode(s);
            if (o == null)
                return Bytes.wrapForRead(decode);
            o.clear();
            o.write(decode);
            return o;
        }

        @Override
        public SerializationStrategy apply(@NotNull Class aClass) {
            switch (aClass.getName()) {
                case "net.openhft.chronicle.bytes.BytesStore":
                    return ScalarStrategy.of(BytesStore.class, (o, in) -> in.bytesStore());
                case "net.openhft.chronicle.bytes.Bytes":
                    return ScalarStrategy.of(Bytes.class,
                            (o, in) -> in.bytesStore().bytesForRead());
                case "java.util.Base64":
                    return ScalarStrategy.of(Bytes.class,
                            SerializeBytes::decodeBase64);
                default:
                    return null;
            }
        }
    }

    static class FieldInfoPair {
        static final FieldInfoPair EMPTY = new FieldInfoPair(Collections.emptyList(), Collections.emptyMap());

        @NotNull
        final List<FieldInfo> list;
        @NotNull
        final Map<String, FieldInfo> map;

        public FieldInfoPair(@NotNull List<FieldInfo> list, @NotNull Map<String, FieldInfo> map) {
            this.list = list;
            this.map = map;
        }
    }

    static class TupleInvocationHandler implements InvocationHandler {
        final String typeName;
        final Map<String, Object> fields = new LinkedHashMap<>();

        private TupleInvocationHandler(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
            switch (method.getName()) {
                case "hashCode":
                    if (args == null || args.length == 0) {
                        return Maths.agitate(typeName.hashCode() * 1019L + fields.hashCode() * 10191L);
                    }
                    break;
                case "equals":
                    if (args != null && args.length == 1) {
                        return equals0(proxy, args[0]);
                    }
                    break;
                case "deepCopy":
                    if (args == null || args.length == 0) {
                        TupleInvocationHandler h2 = new TupleInvocationHandler(typeName);
                        h2.fields.putAll(fields);
                        return proxy.getClass().getDeclaredConstructor(InvocationHandler.class).newInstance(h2);
                    }
                    break;
                case "toString":
                    if (args == null || args.length == 0)
                        return TEXT.asString(proxy);
                    break;
                case "readMarshallable":
                    if (args.length == 1) {
                        WireIn in = (WireIn) args[0];
                        while (in.hasMore()) {
                            fields.put(in.readEvent(String.class), in.getValueIn().object());
                        }
                        return null;
                    }
                    break;
                case "writeMarshallable":
                    if (args.length == 1) {
                        WireOut out = (WireOut) args[0];
                        for (Map.Entry<String, Object> entry : fields.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            if (value != null) {
                                out.write(key).object(value);
                            }
                        }
                        return null;
                    }
                    break;
                case "getField":
                    if (args.length == 2) {
                        Object value = fields.get(args[0]);
                        return ObjectUtils.convertTo((Class) args[1], value);
                    }
                    break;
                case "setField":
                    if (args.length == 2) {
                        fields.put(args[0].toString(), args[1]);
                        return null;
                    }
                    break;
                case "getClassName":
                    if (args == null || args.length == 0)
                        return typeName;
                    break;
                case "$fieldInfos":
                    if (args == null || args.length == 0) {
                        List<FieldInfo> fieldInfos = new ArrayList<>();
                        for (Map.Entry<String, Object> entry : fields.entrySet()) {
                            Class<?> valueClass = entry.getValue().getClass();
                            fieldInfos.add(new TupleFieldInfo(entry.getKey(), valueClass));
                        }
                        return fieldInfos;
                    }
                    break;
            }
            if (args == null || args.length == 0) {
                Class returnType = method.getReturnType();
                return ObjectUtils.convertTo(returnType, fields.get(method.getName()));
            }
            if (args.length == 1) {
                fields.put(method.getName(), args[0]);
                return proxy;
            }
            throw new UnsupportedOperationException(method.toString());
        }

        @NotNull
        private Object equals0(Object proxy, Object o) {
            if (proxy == o)
                return true;
            if (!(o instanceof Marshallable))
                return false;
            Marshallable m = (Marshallable) o;
            if (!m.getClassName().equals(typeName))
                return false;
            if (!Proxy.isProxyClass(m.getClass()))
                return false;
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(m);
            if (!(invocationHandler instanceof TupleInvocationHandler))
                return false;
            TupleInvocationHandler tih = (TupleInvocationHandler) invocationHandler;
            return fields.equals(tih.fields);
        }
    }

    static class TupleFieldInfo extends AbstractFieldInfo {
        public TupleFieldInfo(String name, Class type) {
            super(type, SerializeMarshallables.INSTANCE.apply(type).bracketType(), name);
        }

        private Map<String, Object> getMap(Object o) {
            TupleInvocationHandler invocationHandler = (TupleInvocationHandler) Proxy.getInvocationHandler(o);
            return invocationHandler.fields;
        }

        @Nullable
        @Override
        public Object get(Object object) {
            return getMap(object).get(name);
        }

        @Override
        public long getLong(Object object) {
            return ObjectUtils.convertTo(Long.class, get(object));
        }

        @Override
        public int getInt(Object object) {
            return ObjectUtils.convertTo(Integer.class, get(object));
        }

        @Override
        public char getChar(Object object) {
            return ObjectUtils.convertTo(Character.class, get(object));
        }

        @Override
        public double getDouble(Object object) {
            return ObjectUtils.convertTo(Double.class, get(object));
        }

        @Override
        public void set(Object object, Object value) throws IllegalArgumentException {
            getMap(object).put(name, value);
        }

        @Override
        public void set(Object object, char value) throws IllegalArgumentException {
            set(name, (Object) value);
        }

        @Override
        public void set(Object object, int value) throws IllegalArgumentException {
            set(name, (Object) value);
        }

        @Override
        public void set(Object object, long value) throws IllegalArgumentException {
            set(name, (Object) value);
        }

        @Override
        public void set(Object object, double value) throws IllegalArgumentException {
            set(name, (Object) value);
        }

        @Override
        public Class genericType(int index) {
            return Object.class;
        }
    }
}
