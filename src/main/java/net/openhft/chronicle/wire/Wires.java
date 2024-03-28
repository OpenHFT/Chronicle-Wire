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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.pool.EnumCache;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.scoped.ScopedResourcePool;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.wire.internal.StringConsumerMarshallableOut;
import net.openhft.compiler.CachedCompiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static net.openhft.chronicle.core.util.ReadResolvable.readResolve;
import static net.openhft.chronicle.wire.SerializationStrategies.*;
import static net.openhft.chronicle.wire.WireType.TEXT;
import static net.openhft.chronicle.wire.WireType.YAML_ONLY;

@SuppressWarnings({"rawtypes", "unchecked"})
public enum Wires {
    ; // none
    public static final int LENGTH_MASK = -1 >>> 2;
    public static final int NOT_COMPLETE = 0x8000_0000;
    public static final int META_DATA = 1 << 30;
    public static final int UNKNOWN_LENGTH = 0x0;
    // value to use when the message is not ready and of an unknown length
    public static final int NOT_COMPLETE_UNKNOWN_LENGTH = NOT_COMPLETE;
    // value to use when no more data is possible e.g. on a roll.
    public static final int END_OF_DATA = NOT_COMPLETE | META_DATA;
    public static final int NOT_INITIALIZED = 0x0;
    public static final Bytes<?> NO_BYTES = BytesStore.empty().bytesForRead();
    public static final int SPB_HEADER_SIZE = 4;
    public static final List<Function<Class, SerializationStrategy>> CLASS_STRATEGY_FUNCTIONS = new CopyOnWriteArrayList<>();

    // Adding SuppressWarnings on every usage could have too many side effects, so make it a comment for now, but still remove it.
    // @Deprecated(/* for removal in x.26 */)
    static boolean THROW_CNFRE = Jvm.getBoolean("class.not.found.for.missing.class.alias", true);

    static final ClassLocal<SerializationStrategy> CLASS_STRATEGY = ClassLocal.withInitial(c -> {
        for (@NotNull Function<Class, SerializationStrategy> func : CLASS_STRATEGY_FUNCTIONS) {
            final SerializationStrategy strategy = func.apply(c);
            if (strategy != null)
                return strategy;
        }
        return ANY_OBJECT;
    });
    static final ClassLocal<FieldInfoPair> FIELD_INFOS = ClassLocal.withInitial(FieldInfo::lookupClass);
    static final ClassLocal<Function<String, Marshallable>> MARSHALLABLE_FUNCTION = ClassLocal.withInitial(tClass -> {
        Class[] interfaces = {Marshallable.class, tClass};
        if (tClass == Marshallable.class)
            interfaces = new Class[]{Marshallable.class};
        try {
            Class<?> proxyClass = Proxy.getProxyClass(tClass.getClassLoader(), interfaces);
            Constructor<?> constructor = proxyClass.getConstructor(InvocationHandler.class);
            constructor.setAccessible(true);
            return typeName -> newInstance(constructor, typeName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    });
    static final ScopedResourcePool<StringBuilder> STRING_BUILDER_SCOPED_RESOURCE_POOL = StringBuilderPool.createThreadLocal();
    static final ThreadLocal<BinaryWire> WIRE_TL = ThreadLocal.withInitial(() -> new BinaryWire(Bytes.allocateElasticOnHeap()));
    static final boolean DUMP_CODE_TO_TARGET = Jvm.getBoolean("dumpCodeToTarget", Jvm.isDebug());
    private static final int TID_MASK = 0b00111111_11111111_11111111_11111111;
    private static final int INVERSE_TID_MASK = ~TID_MASK;
    public static boolean GENERATE_TUPLES = Jvm.getBoolean("wire.generate.tuples");
    static volatile boolean warnedUntypedBytesOnce = false;
    static ThreadLocal<StringBuilder> sb = ThreadLocal.withInitial(StringBuilder::new);
    private static CachedCompiler CACHED_COMPILER = null;

    static {
        Jvm.addToClassPath(Wires.class);
        CLASS_STRATEGY_FUNCTIONS.add(SerializeEnum.INSTANCE);
        CLASS_STRATEGY_FUNCTIONS.add(SerializeJavaLang.INSTANCE);
        CLASS_STRATEGY_FUNCTIONS.add(SerializeBytes.INSTANCE);
        CLASS_STRATEGY_FUNCTIONS.add(SerializeMarshallables.INSTANCE); // must be after SerializeBytes.
        WireInternal.addAliases();
    }

    // force static initialise
    public static void init() {
        // Do nothing here
    }

    /**
     * Creates and returns a proxy of the specified interface. The proxy writes inputs into the specified PrintStream in Yaml format.
     *
     * @param tClass the specified interface
     * @param ps     the PrintStream used to write output data into as Yaml format
     * @param <T>    type of the specified interface
     * @return a proxy of the specified interface
     */
    public static <T> T recordAsYaml(Class<T> tClass, PrintStream ps) {
        MarshallableOut out = new StringConsumerMarshallableOut(s -> {
            if (!s.startsWith("---\n"))
                ps.print("---\n");
            ps.print(s);
            if (!s.endsWith("\n"))
                ps.print("\n");
        }, YAML_ONLY);
        return out.methodWriter(tClass);
    }

    /**
     * Reads the content of a specified Yaml file and feeds it to the specified object.
     *
     * @param file name of the input Yaml file
     * @param obj  the object that replays the data in the specified file
     * @throws IOException is thrown if an IO operation fails
     */
    public static void replay(String file, Object obj) throws IOException, InvalidMarshallableException {
        Bytes bytes = BytesUtil.readFile(file);
        Wire wire = new YamlWire(bytes).useTextDocuments();
        MethodReader readerObj = wire.methodReader(obj);
        while (readerObj.readOne()) {
        }
        bytes.releaseLast();
    }

    /**
     * This decodes some Bytes where the first 4-bytes is the length.  e.g. Wire.writeDocument wrote
     * it. <a href="https://github.com/OpenHFT/RFC/tree/master/Size-Prefixed-Blob">Size Prefixed
     * Blob</a>
     *
     * @param bytes to decode
     * @return as String
     */
    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes) {
        return WireDumper.of(bytes).asString();
    }

    public static String fromAlignedSizePrefixedBlobs(@NotNull Bytes<?> bytes) {
        return WireDumper.of(bytes, true).asString();
    }

    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes, boolean abbrev) {
        return WireDumper.of(bytes).asString(abbrev);
    }

    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes, long position) {
        return fromSizePrefixedBlobs(bytes, position, false);
    }

    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes, long position, boolean padding) {
        final long limit = bytes.readLimit();
        if (position > limit)
            return "";
        return WireDumper.of(bytes, padding).asString(position, limit - position);
    }

    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes, boolean padding, boolean abbrev) {
        return WireDumper.of(bytes, padding).asString(abbrev);
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

            Bytes<?> tempBytes = Bytes.allocateElasticDirect();
            try {
                tempBytes.writeOrderedInt(header);
                final AbstractWire wire2 = ((BinaryReadDocumentContext) dc).wire;
                tempBytes.write(wire2.bytes, 0, wire2.bytes.readLimit());

                final WireType wireType = WireType.valueOf(wire);

                assert wireType != null;
                Wire tempWire = wireType.apply(tempBytes);

                return WireDumper.of(tempWire).asString(0, length + 4);

            } finally {
                tempBytes.releaseLast();
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
        return fromSizePrefixedBlobs(wireIn, false);
    }

    public static String fromSizePrefixedBlobs(@NotNull WireIn wireIn, boolean abbrev) {
        return WireDumper.of(wireIn).asString(abbrev);
    }

    @NotNull
    public static CharSequence asText(@NotNull WireIn wireIn, Bytes<?> output) {
        ValidatableUtil.startValidateDisabled();
        try {
            return asType(wireIn, Wires::newTextWire, output);
        } finally {
            ValidatableUtil.endValidateDisabled();
        }
    }

    private static Wire newJsonWire(Bytes bytes) {
        return new JSONWire(bytes).useTypes(true).trimFirstCurly(false).useTextDocuments();
    }

    public static Bytes<?> asBinary(@NotNull WireIn wireIn, Bytes<?> output) throws InvalidMarshallableException {
        return asType(wireIn, BinaryWire::new, output);
    }

    private static Bytes<?> asType(@NotNull WireIn wireIn, Function<Bytes, Wire> wireProvider, Bytes<?> output) throws InvalidMarshallableException {
        long pos = wireIn.bytes().readPosition();
        try {
            wireIn.copyTo(new TextWire(output).addTimeStamps(true));
            return output;
        } finally {
            wireIn.bytes().readPosition(pos);
        }
    }

    public static Bytes<?> asJson(@NotNull WireIn wireIn, Bytes<?> output) throws InvalidMarshallableException {
        return asType(wireIn, Wires::newJsonWire, output);
    }

    private static Wire newTextWire(Bytes bytes) {
        return new TextWire(bytes).addTimeStamps(true);
    }

    public static ScopedResource<StringBuilder> acquireStringBuilderScoped() {
        return STRING_BUILDER_SCOPED_RESOURCE_POOL.get();
    }

    public static int lengthOf(int len) {
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
            @NotNull T writer) throws InvalidMarshallableException {
        return WireInternal.writeData(wireOut, false, false, writer);
    }

    @ForceInline
    public static long readWire(@NotNull WireIn wireIn, long size, @NotNull ReadMarshallable readMarshallable) throws InvalidMarshallableException {
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

    static Bytes<?> unmonitoredDirectBytes() {
        Bytes<?> bytes = Bytes.allocateElasticDirect(128);
        IOTools.unmonitor(bytes);
        return bytes;
    }

    @NotNull
    public static ScopedResource<Bytes<?>> acquireBytesScoped() {
        return WireInternal.BYTES_SCOPED_THREAD_LOCAL.get();
    }

    public static ScopedResource<Wire> acquireBinaryWireScoped() {
        return WireInternal.BINARY_WIRE_SCOPED_TL.get();
    }

    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes, long position, long length) {
        return WireDumper.of(bytes).asString(position, length);
    }

    public static void readMarshallable(@NotNull Object marshallable, @NotNull WireIn wire, boolean overwrite) throws InvalidMarshallableException {
        final Class<?> clazz = marshallable.getClass();
        readMarshallable(clazz, marshallable, wire, overwrite);
    }

    public static void readMarshallable(Class<?> clazz, @NotNull Object marshallable, @NotNull WireIn wire, boolean overwrite) throws InvalidMarshallableException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(clazz == null ? marshallable.getClass() : clazz);
        wm.readMarshallable(marshallable, wire, overwrite);
    }

    public static void writeMarshallable(@NotNull Object marshallable, @NotNull WireOut wire) throws InvalidMarshallableException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass());
        wm.writeMarshallable(marshallable, wire);
    }

    public static void writeMarshallable(@NotNull Object marshallable, @NotNull WireOut wire, boolean writeDefault) throws InvalidMarshallableException {
        WireMarshaller marshaller = WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass());
        if (writeDefault)
            marshaller.writeMarshallable(marshallable, wire);
        else
            marshaller.writeMarshallable(marshallable, wire, false);
    }

    public static void writeMarshallable(@NotNull Object marshallable, @NotNull WireOut wire, @NotNull Object previous, boolean copy) throws InvalidMarshallableException {
        assert marshallable.getClass() == previous.getClass();
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass());
        wm.writeMarshallable(marshallable, wire, copy);
    }

    public static void writeKey(@NotNull Object marshallable, Bytes<?> bytes) {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass());
        wm.writeKey(marshallable, bytes);
    }

    @NotNull
    public static <T extends Marshallable> T deepCopy(@NotNull T marshallable) throws InvalidMarshallableException {
        if (Enum.class.isAssignableFrom(marshallable.getClass()))
            return marshallable;

        try (ScopedResource<Wire> wireSR = acquireBinaryWireScoped()) {
            Wire wire = wireSR.get();
            @NotNull T t = (T) ObjectUtils.newInstance(marshallable.getClass());
            boolean useSelfDescribing = t.usesSelfDescribingMessage() || !(t instanceof BytesMarshallable);
            if (useSelfDescribing) {
                marshallable.writeMarshallable(wire);
                t.readMarshallable(wire);
            } else {
                ((BytesMarshallable) marshallable).writeMarshallable(wire.bytes());
                ((BytesMarshallable) t).readMarshallable(wire.bytes());
            }
            return t;
        }
    }

    /**
     * Copy fields from source to target by marshalling out and then in. Allows copying of fields by name
     * even if there is no type relationship between the source and target
     *
     * @param source source
     * @param target dest
     * @return target
     * @param <T> target type
     */
    @NotNull
    public static <T> T copyTo(Object source, @NotNull T target) throws InvalidMarshallableException {
        try (ScopedResource<Wire> wireSR = acquireBinaryWireScoped()) {
            ValidatableUtil.startValidateDisabled();
            Wire wire = wireSR.get();
            wire.getValueOut().object(source);
            wire.getValueIn().typePrefix(); // drop the type prefix.
            wire.getValueIn().object(target, target.getClass());
            return target;
        } finally {
            ValidatableUtil.endValidateDisabled();
        }
    }

    @NotNull
    public static <T> T project(Class<T> tClass, Object source) throws InvalidMarshallableException {
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

    public static @NotNull Map<String, FieldInfo> fieldInfoMap(@NotNull Class aClass) {
        return FIELD_INFOS.get(aClass).map;
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

    public static long getLongField(@NotNull Object o, String name) throws NoSuchFieldException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(o.getClass());
        return wm.getLongField(o, name);
    }

    public static void setField(@NotNull Object o, String name, Object value) throws NoSuchFieldException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(o.getClass());
        wm.setField(o, name, value);
    }

    public static void setLongField(@NotNull Object o, String name, long value) throws NoSuchFieldException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(o.getClass());
        wm.setLongField(o, name, value);
    }

    public static void reset(@NotNull Object o) {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(o.getClass());
        wm.reset(o);
    }

    public static int removeMaskedTidFromHeader(final int header) {
        return header & INVERSE_TID_MASK;
    }

    @Nullable
    public static <E> E objectSequence(ValueIn in, @Nullable E using, @Nullable Class clazz, SerializationStrategy<E> strategy) {
        if (clazz == Object.class)
            strategy = LIST;
        if (using == null)
            using = (E) strategy.newInstanceOrNull(clazz);

        SerializationStrategy<E> finalStrategy = strategy;
        return in.sequence(using, (using1, in1) -> finalStrategy.readUsing(clazz, using1, in1, BracketType.UNKNOWN)) ? readResolve(using) : null;
    }

    @Nullable
    public static <E> E objectMap(ValueIn in, @Nullable E using, @Nullable Class clazz, @NotNull SerializationStrategy<E> strategy) throws InvalidMarshallableException {
        if (in.isNull())
            return null;
        if (clazz == Object.class)
            strategy = MAP;
        if (using == null) {
            using = (E) strategy.newInstanceOrNull(clazz);
        }
        if (Throwable.class.isAssignableFrom(clazz))
            return (E) WireInternal.throwable(in, false, (Throwable) using);

        if (using == null)
            throw new ClassNotFoundRuntimeException(new ClassNotFoundException("failed to create instance of clazz=" + clazz + " is it aliased?"));

        Object marshallable = in.marshallable(using, strategy);
        E e = readResolve(marshallable);
        String name = nameOf(e);
        if (name != null) {
            Class<?> aClass = e.getClass();
            E e2 = (E) EnumCache.of(aClass).valueOf(name);
            if (e != e2) {
                try (ScopedResource<Wire> wireSR = Wires.acquireBinaryWireScoped()) {
                    Wire wire = wireSR.get();
                    WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(aClass);
                    wm.writeMarshallable(e, wire);
                    wm.readMarshallable(e2, wire, false);
                }
                return e2;
            }
        }
        return e;
    }

    private static <E> String nameOf(E e) {
        return e instanceof CoreDynamicEnum ? ((CoreDynamicEnum) e).name()
                : e instanceof Enum ? ((Enum) e).name() : null;
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
    public static <E> E object0(ValueIn in, @Nullable E using, @Nullable Class clazz) throws InvalidMarshallableException {
        return ValidatableUtil.validate(object1(in, using, clazz, true));
    }

    public static <E> E object0(ValueIn in, @Nullable E using, @Nullable Class clazz, boolean bestEffort) throws InvalidMarshallableException {
        return ValidatableUtil.validate(object1(in, using, clazz, bestEffort));
    }

    public static <E> E object1(ValueIn in, @Nullable E using, @Nullable Class clazz, boolean bestEffort) throws InvalidMarshallableException {
        Object o = in.typePrefixOrObject(clazz);
        if (o == null && using instanceof ReadMarshallable)
            o = using;
        if (o != null && !(o instanceof Class)) {
            return (E) in.marshallable(o, MARSHALLABLE);
        }
        return object2(in, using, clazz, bestEffort, (Class) o);
    }

    @Nullable
    static <E> E object2(ValueIn in, @Nullable E using, @Nullable Class clazz, boolean bestEffort, Class o) {
        @Nullable final Class clazz2 = o;
        if (clazz2 == void.class) {
            in.text();
            return null;
        } else if (clazz2 == BytesStore.class) {
            if (using == null)
                using = (E) Bytes.allocateElasticOnHeap(32);
            clazz = Base64.class;
            bestEffort = true;
        }
        if (clazz2 == null && clazz != null) {
            clazz = ObjectUtils.implementationToUse(clazz);
        }

        if (clazz2 != null &&
                clazz != clazz2) {
            if (clazz == null
                    || clazz.isAssignableFrom(clazz2)
                    || ReadResolvable.class.isAssignableFrom(clazz2)
                    || !ObjectUtils.isConcreteClass(clazz)) {
                clazz = clazz2;
                if (!clazz.isInstance(using))
                    using = null;
            } else if (!bestEffort && !(isScalarClass(clazz) && isScalarClass(clazz2))) {
                throw new ClassCastException("Unable to read a " + clazz2 + " as a " + clazz);
            }
        }
        if (clazz == null)
            clazz = Object.class;
        Class classForStrategy = clazz.isInterface() && using != null ? using.getClass() : clazz;
        SerializationStrategy<E> strategy = CLASS_STRATEGY.get(classForStrategy);
        BracketType brackets = strategy.bracketType();
        if (brackets == BracketType.UNKNOWN)
            brackets = in.getBracketType();

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
                @NotNull final Object e = strategy.readUsing(clazz, using, in, BracketType.NONE);
                return clazz == Base64.class || e == null
                        ? (E) e
                        : (E) WireInternal.intern(clazz, e);

            default:
                throw new AssertionError();
        }
    }

    static boolean isScalar(Serializable object) {
        if (object instanceof Comparable) {
            final SerializationStrategy strategy = Wires.CLASS_STRATEGY.get(object.getClass());
            return strategy != ANY_OBJECT && strategy != ANY_NESTED;
        }
        return false;
    }

    static boolean isScalarClass(Class type) {
        if (Comparable.class.isAssignableFrom(type)) {
            final SerializationStrategy strategy = Wires.CLASS_STRATEGY.get(type);
            return strategy != ANY_OBJECT && strategy != ANY_NESTED;
        }
        return false;
    }

    public static boolean dtoInterface(Class clazz) {
        return clazz != null
                && clazz.isInterface()
                && clazz != Bytes.class
                && clazz != BytesStore.class
                && !clazz.getPackage().getName().startsWith("java");
    }

    public static String typeNameFor(@NotNull Object value) {
        return typeNameFor(ClassAliasPool.CLASS_ALIASES, value);
    }

    public static String typeNameFor(ClassLookup classLookup, @NotNull Object value) {
        return classLookup == ClassAliasPool.CLASS_ALIASES
                && value instanceof Marshallable
                ? ((Marshallable) value).className()
                : classLookup.nameFor(value.getClass());
    }

    static Marshallable newInstance(Constructor constructor, String typeName) {
        try {
            return (Marshallable) constructor.newInstance(new TupleInvocationHandler(typeName));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Nullable
    public static <T> T tupleFor(Class<T> tClass, String typeName) {
        if (!GENERATE_TUPLES) {
            Jvm.warn().on(Wires.class, "Cannot find a class for " + typeName + " are you missing an alias?");
            return null;
        }

        if (tClass == null || tClass == Object.class)
            tClass = (Class<T>) Marshallable.class;
        if (!tClass.isInterface()) {
            Jvm.warn().on(Wires.class, "Cannot generate a class for " + typeName + " are you missing an alias?");
            return null;
        }
        return (T) MARSHALLABLE_FUNCTION.get(tClass).apply(typeName);
    }

    public static boolean isInternal(@NotNull Object value) {
        String name = value.getClass().getPackage().getName();
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("jdk.");
    }

    @NotNull
    public static BinaryWire binaryWireForRead(Bytes<?> in, long position, long length) {
        BinaryWire wire = WIRE_TL.get();
        VanillaBytes bytes = (VanillaBytes) wire.bytes();
        wire.clear();
        bytes.bytesStore(in.bytesStore(), position, length);
        return wire;
    }

    @NotNull
    public static BinaryWire binaryWireForWrite(Bytes<?> in, long position, long length) {
        BinaryWire wire = WIRE_TL.get();
        VanillaBytes bytes = (VanillaBytes) wire.bytes();
        bytes.bytesStore(in.bytesStore(), 0, position);
        bytes.writeLimit(position + length);
        return wire;
    }

    static synchronized Class loadFromJava(ClassLoader classLoader, String className, String code) throws ClassNotFoundException {
        if (CACHED_COMPILER == null) {
            final String target = OS.getTarget();
            File sourceDir = null;
            File classDir = null;

            if (new File(target).exists() && DUMP_CODE_TO_TARGET) {
                sourceDir = new File(target, "generated-test-sources");
                classDir = new File(target, "test-classes");
            }

            String compilerOptions = Jvm.getProperty("compiler.options");

            if (compilerOptions == null || compilerOptions.trim().isEmpty()) {
                CACHED_COMPILER = new CachedCompiler(sourceDir, classDir);
            } else {
                CACHED_COMPILER = new CachedCompiler(sourceDir, classDir, asList(compilerOptions.split("\\s")));
            }
        }
        try {
            return CACHED_COMPILER.loadFromJava(classLoader, className, code);
        } catch (Throwable t) {
            Closeable.closeQuietly(CACHED_COMPILER);
            CACHED_COMPILER = null;
            throw t;
        }
    }

    enum SerializeEnum implements Function<Class, SerializationStrategy> {
        INSTANCE;

        @Nullable
        static SerializationStrategy getSerializationStrategy(@NotNull Class aClass) {
            if (DynamicEnum.class.isAssignableFrom(aClass))
                return DYNAMIC_ENUM;
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

        private static final String SDF_4_STRING = "yyyy-MM-dd";

        private static final SimpleDateFormat SDF = new SimpleDateFormat("EEE MMM d HH:mm:ss.S zzz yyyy");
        private static final SimpleDateFormat SDF_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS zzz");
        private static final SimpleDateFormat SDF_3 = new SimpleDateFormat("EEE MMM d HH:mm:ss.S zzz yyyy", Locale.US);
        private static final SimpleDateFormat SDF_4 = new SimpleDateFormat(SDF_4_STRING);

        static {
            SDF.setTimeZone(TimeZone.getTimeZone("GMT"));
            SDF_2.setTimeZone(TimeZone.getTimeZone("GMT"));
            SDF_3.setTimeZone(TimeZone.getTimeZone("GMT"));
            SDF_4.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        public static WireOut writeDate(Date date, ValueOut out) {
            final String format = SDF_2.format(date);
            return out.writeString(format);
        }

        public static Date parseDate(ValueIn in) {
            final String text = in.text().trim();
            if (text.length() < 1) {
                throw new IORuntimeException("At least one character (e.g. '0') must be present in order to deserialize a Date object");
            }
            final char firstChar = text.charAt(0);

            // Check if it is a number
            if (firstChar == '+' || firstChar == '-' || Character.isDigit(firstChar)) {
                boolean isAllNum = true;
                for (int i = 1; i < text.length(); i++) {
                    if (!Character.isDigit(text.charAt(i))) {
                        isAllNum = false;
                        break;
                    }
                }
                if (isAllNum) {
                    try {
                        // It is a number and not any of the other formats as they all contain non digits
                        return new Date(Long.parseLong(text));
                    } catch (NumberFormatException nfe) {
                        throw new IORuntimeException(nfe);
                    }
                }
            }

            // Check if it is a string of format "yyyy-mm-dd"
            if (text.length() == SDF_4_STRING.length()) {
                try {
                    synchronized (SDF_4) {
                        // Since we ruled out it is a number and SDF_$ is the only one with this length it must be SDF_4
                        return SDF_4.parse(text);
                    }
                } catch (ParseException pe) {
                    throw new IORuntimeException(pe);
                }
            }

            // Try the other remaining formats
            // Todo: optimize away exception chaining
            try {
                synchronized (SDF_2) {
                    return SDF_2.parse(text);
                }
            } catch (ParseException ignored) {
                // Ignore
            }

            synchronized (SDF) {
                try {
                    return SDF.parse(text);
                } catch (ParseException ignored) {
                    // Ignore
                }
            }

            try {
                synchronized (SDF_3) {
                    return SDF_3.parse(text);
                }
            } catch (ParseException pe3) {
                throw new IORuntimeException("unable to parse: " + text, pe3);
            }

        }

        private static Class forName(Class o, ValueIn in) {
            final StringBuilder sb0 = sb.get();
            sb0.setLength(0);
            in.text(sb0);

            return in.classLookup().forName(sb0);
        }

        @Override
        public SerializationStrategy apply(@NotNull Class aClass) {
            switch (aClass.getName()) {
                case "[B":
                    return ScalarStrategy.of(byte[].class, (o, in) -> in.bytes());

                case "java.lang.StringBuilder":
                    return ScalarStrategy.of(StringBuilder.class, (o, in) -> {
                        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                            StringBuilder builder = (o == null)
                                    ? stlSb.get()
                                    : o;
                            in.textTo(builder);
                        }
                        return o;
                    });

                case "java.lang.String":
                    return ScalarStrategy.of(String.class, (o, in) -> in.text());

                case "java.lang.Object":
                    return ANY_OBJECT;

                case "java.lang.Class":
                    return ScalarStrategy.of(Class.class, SerializeJavaLang::forName);

                case "java.lang.Boolean":
                    return ScalarStrategy.of(Boolean.class, (o, in) -> in.bool());

                case "java.lang.Byte":
                    return ScalarStrategy.of(Byte.class, (o, in) -> in.int8());

                case "java.lang.Short":
                    return ScalarStrategy.of(Short.class, (o, in) -> in.int16());

                case "java.lang.Character":
                    return ScalarStrategy.of(Character.class, (o, in) -> {
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

                case "java.sql.Time":
                    return ScalarStrategy.of(Time.class, (o, in) -> new Time(parseDate(in).getTime()));

                case "java.sql.Date":
                    return ScalarStrategy.of(java.sql.Date.class, (o, in) -> new java.sql.Date(parseDate(in).getTime()));

                case "javax.naming.CompositeName":
                    return ScalarStrategy.of(CompositeName.class, (o, in) -> {
                        try {
                            return new CompositeName(in.text());
                        } catch (InvalidNameException e) {
                            throw Jvm.rethrow(e);
                        }
                    });

                case "java.io.File":
                    return ScalarStrategy.text(File.class, File::new);

                case "java.util.UUID":
                    return ScalarStrategy.of(UUID.class, (o, in) -> in.uuid());

                case "java.math.BigInteger":
                    return ScalarStrategy.text(BigInteger.class, BigInteger::new);

                case "java.math.BigDecimal":
                    return ScalarStrategy.text(BigDecimal.class, BigDecimal::new);

                case "java.util.Date":
                    return ScalarStrategy.of(Date.class, (o, in) -> parseDate(in));

                case "java.time.Duration":
                    return ScalarStrategy.of(Duration.class, (o, in) -> Duration.parse(in.text()));

                case "java.time.Instant":
                    return ScalarStrategy.of(Instant.class, (o, in) -> Instant.parse(in.text()));

                case "java.sql.Timestamp":
                    return ScalarStrategy.of(Timestamp.class, (o, in) -> new Timestamp(parseDate(in).getTime()));

                case "java.util.GregorianCalendar":
                    return ScalarStrategy.of(GregorianCalendar.class, (o, in) -> GregorianCalendar.from(in.zonedDateTime()));

                case "java.util.Locale":
                    return ScalarStrategy.of(Locale.class, (o, in) -> Locale.forLanguageTag(in.text()));

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
            if (ReadMarshallable.class.isAssignableFrom(aClass)
                    || ReadBytesMarshallable.class.isAssignableFrom(aClass))
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
            if (Comparable.class.isAssignableFrom(aClass))
                return ANY_SCALAR;
            if (aClass.isInterface())
                return null;
            return ANY_NESTED;
        }
    }

    enum SerializeBytes implements Function<Class, SerializationStrategy> {
        INSTANCE;

        static Bytes<?> decodeBase64(Bytes<?> o, ValueIn in) {
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                @NotNull StringBuilder sb0 = stlSb.get();
                in.text(sb0);
                String s = WireInternal.INTERNER.intern(sb0);
                byte[] decode = Base64.getDecoder().decode(s);
                if (o == null)
                    return Bytes.wrapForRead(decode);
                o.clear();
                o.write(decode);
                return o;
            }
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
            String name = method.getName();
            switch (name) {
                case "hashCode":
                    if (args == null || args.length == 0) {
                        return (int) Maths.agitate(typeName.hashCode() * 1019L + fields.hashCode() * 10191L);
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
                case "className":
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
                case "usesSelfDescribingMessage":
                    return Boolean.TRUE;
            }
            if (args == null || args.length == 0) {
                Class returnType = method.getReturnType();
                if (fields.containsKey(name))
                    return ObjectUtils.convertTo(returnType, fields.get(name));
                return ObjectUtils.defaultValue(returnType);
            }
            if (args.length == 1) {
                fields.put(name, args[0]);
                return proxy;
            }
            throw new UnsupportedOperationException("The class or alias " + typeName + " could not be found, so unable to call " + method);
        }

        @NotNull
        private Object equals0(Object proxy, Object o) {
            if (proxy == o)
                return true;
            if (!(o instanceof Marshallable))
                return false;
            Marshallable m = (Marshallable) o;
            if (!m.className().equals(typeName))
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
            super(type, bracketType(SerializeMarshallables.INSTANCE.apply(type)), name);
        }

        static BracketType bracketType(SerializationStrategy ss) {
            return ss == null ? BracketType.UNKNOWN : ss.bracketType();
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
        public Class<?> genericType(int index) {
            return Object.class;
        }

        @Override
        public boolean isEqual(Object a, Object b) {
            return Objects.deepEquals(get(a), get(b));
        }
    }
}
