package net.openhft.chronicle.wire;

import io.github.classgraph.*;
import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

final class SerializableObjectTest extends WireTestCommon {

    private static final long TIME_MS = 1_000_000_000;

    private static final Set<String> IGNORED_PACKAGES = Stream.of(
                    "jnr.",
                    "sun.",
                    "io.github.",
                    "com.sun.",
                    "org.junit.",
                    "org.jcp.xml.dsig.internal.",
                    "jdk.nashorn.",
                    "org.easymock.",
                    "org.omg.",
                    "org.yaml.",
                    "com.sun.corba.",
                    "com.sun.org.",
                    "com.sun.security.cert.internal",
                    "javax.management.remote.rmi",
                    "javax.smartcardio",
                    "javafx.",
                    "javax.swing",
                    "javax.print",
                    "apple.security"
            )
            .collect(Collectors.collectingAndThen(toSet(), Collections::unmodifiableSet));


    private static final Set<Class<?>> IGNORED_CLASSES = new HashSet<>(Arrays.asList(
            DoubleSummaryStatistics.class,
            DriverPropertyInfo.class,
            SimpleDateFormat.class
    ));

    static {

        try {
            // change to this because it fails in java11
            final Class<?> aClass = Class.forName("com.sun.jndi.toolkit.ctx.Continuation");
            IGNORED_CLASSES.add(aClass);
        } catch (ClassNotFoundException ignore) {

        }

    }


    private static final Predicate<MethodInfo> CONSTRUCTOR_IS_DEFAULT = methodInfo -> methodInfo.isPublic() && methodInfo.getTypeDescriptor().getTypeParameters().isEmpty();
    private static final ClassInfoList.ClassInfoFilter NOT_IGNORED = ci -> IGNORED_PACKAGES.stream().noneMatch(ip -> ci.getPackageName().startsWith(ip));

    private static Stream<WireTypeObject> cases() {
        return wires()
                .flatMap(wt -> mergedObjects().map(o -> new WireTypeObject(wt, o)));
    }

    private static Stream<Object> mergedObjects() {
        Map<String, Object> map = handcraftedObjects()
                .collect(Collectors.toMap(o -> {
                    final Class<?> aClass = o.getClass();
                    IGNORED_CLASSES.add(aClass);
                    return aClass.getName();
                }, Function.identity()));

        reflectedObjects()
                .forEach(o -> map.put(o.getClass().getName(), o));
        return map.values().stream();
    }

    private static Stream<Object> handcraftedObjects() {
        return Stream.of(
                // java.lang
                true,
                (byte) 1,
                (char) '2',
                (short) 3,
                4,
                5L,
                6.0f,
                7.0,
                BigInteger.valueOf(Long.MIN_VALUE),
                BigDecimal.valueOf(12.34),
                MathContext.DECIMAL32,
                // java.sql
                new Time(TIME_MS),
                new Timestamp(TIME_MS),
                new SQLException("Test exception"),
                new DriverPropertyInfo("A", "B"),
                new Date(TIME_MS),
                wrap(() -> new SerialClob("A".toCharArray())),
                wrap(() -> new SerialBlob("A".getBytes(StandardCharsets.UTF_8))),
                // java.util
                compose(new ArrayList<String>(), l -> l.add("a"), l -> l.add("b")),
                compose(new BitSet(), bs -> bs.set(10)),
                Currency.getAvailableCurrencies().iterator().next(),
                DoubleStream.of(1, 2, 3).summaryStatistics(),
                compose(new EnumMap<TimeUnit, String>(TimeUnit.class), m -> m.put(TimeUnit.SECONDS, "secs")),
                EnumSet.of(TimeUnit.NANOSECONDS, TimeUnit.SECONDS),
                new EventObject("A"),
                compose(new HashMap<>(), m -> m.put(1, 1)),
                compose(new Hashtable<>(), m -> m.put(1, 1)),
                compose(new IdentityHashMap<>(), m -> m.put(1, 1)),
                IntStream.of(1, 2, 3).summaryStatistics(),
                compose(new LinkedHashMap<>(), m -> m.put(2, 2), m -> m.put(1, 1), m -> m.put(3, 3)),
                compose(new LinkedHashSet<>(), m -> m.add(2), m -> m.add(1), m -> m.add(3)),
                compose(new LinkedList<>(), m -> m.add(2), m -> m.add(1), m -> m.add(3)),
                Locale.getAvailableLocales()[1],
                Optional.of("A"),
                Optional.empty(),
                OptionalDouble.of(2),
                OptionalDouble.empty(),
                OptionalInt.of(2),
                OptionalInt.empty(),
                OptionalLong.of(2),
                OptionalLong.empty(),
                compose(new Properties(), p -> p.put("A", 1), p -> p.put("B", 2)),
                new SimpleTimeZone(1, "EU"),
                compose(new Stack<Integer>(), q -> q.push(2), q -> q.push(1), q -> q.push(3)),
                compose(new StringJoiner("[", ", ", "]"), sj -> sj.add("a"), sj -> sj.add("b")),
                compose(new TreeMap<>(), m -> m.put(2, 2), m -> m.put(1, 1), m -> m.put(3, 3)),
                compose(new TreeSet<>(), m -> m.add(2), m -> m.add(1), m -> m.add(3)),
                UUID.randomUUID(),
                compose(new Vector<>(), v -> v.add("a"), v -> v.add("b")),
                //
                Instant.ofEpochMilli(TIME_MS),
                Color.BLUE,
//                new MessageFormat("%s%n"),
//                InetAddress.getLoopbackAddress(),
                new File("file")
//                create(() -> new URL("http://chronicle.software/dir/files"))
        ).filter(SerializableObjectTest::isSerializableEqualsByObject);
    }

    private static Object create(ThrowingSupplier s) {
        try {
            return s.get();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Stream<Object> reflectedObjects() {
        try (ScanResult scanResult = new ClassGraph().enableSystemJarsAndModules().enableAllInfo().scan()) {
            final ClassInfoList widgetClasses = scanResult.getClassesImplementing(Serializable.class)
                    .filter(ci -> !ci.isAbstract())
                    .filter(ClassInfo::isPublic)
                    .filter(NOT_IGNORED)
                    .filter(ci -> !ci.isAnonymousInnerClass())
                    .filter(ci -> !ci.extendsSuperclass(LookAndFeel.class)) // These create problems
                    .filter(ci -> !ci.implementsInterface(DesktopManager.class)) // These create problems
                    .filter(ci -> ci.getConstructorInfo().stream().anyMatch(CONSTRUCTOR_IS_DEFAULT))
                    .filter(SerializableObjectTest::isSerializableEquals);

            List<Object> objects = widgetClasses.stream()
                    .filter(c -> !IGNORED_CLASSES.contains(c.loadClass(true)))
                    .filter(SerializableObjectTest::overridesEqualsObject)
                    .map(ci -> ci.loadClass(true))
                    .filter(Objects::nonNull)
                    .map(SerializableObjectTest::createOrNull)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            /*
            System.out.println("widgetClasses.size() = " + widgetClasses.size());
            System.out.println("objects.size = " + objects.size());

            objects.stream()
                    .map(i -> i.getClass().getName() + " -> " + i)
                    .forEach(System.out::println);*/
            return objects.stream();

        }
        //return null;
    }

    private static boolean isSerializableEquals(ClassInfo ci) {
        return isSerializableEquals(ci.loadClass(), null);
    }

    private static boolean isSerializableEqualsByObject(Object o) {
        return isSerializableEquals(o.getClass(), o);
    }

    private static boolean isSerializableEquals(Class aClass, Object o) {
        try {
            Object source = o == null ? aClass.newInstance() : o;
            // sanity check
            assertNotNull(source.toString());
            // can it be serialized
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(source);

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            Object source2 = ois.readObject();
            if (source instanceof Throwable) {
                assertEquals(source.getClass(), source2.getClass());
                assertEquals(((Throwable) source).getMessage(), ((Throwable) source2).getMessage());
            } else {
                assertEquals(source, source2);
            }
            return true;
        } catch (AssertionError ae) {
            // Java Serialization does serialize/deserialize a class which is equal
            return false;
        } catch (InstantiationException | NotSerializableException | IllegalAccessException t) {
            return false;
        } catch (Throwable t) {
            System.out.println(aClass + ": " + t);
            return false;
        }
    }

    private static <T> T createOrNull(final Class<T> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (ReflectiveOperationException ignore) {
            return null;
        }
    }
/*

    @Test
    void reflectedObjects2() {
        try (ScanResult scanResult = new ClassGraph().enableSystemJarsAndModules().enableAllInfo().scan()) {
            final ClassInfoList widgetClasses = scanResult.getClassesImplementing(Serializable.class)
                    .filter(ci -> !ci.isAbstract())
                    .filter(ClassInfo::isPublic)
                    .filter(ci -> !IGNORED_PACKAGES.stream().anyMatch(ip -> ci.getPackageName().startsWith(ip)));


            widgetClasses.
                    forEach(System.out::println);
            System.out.println("widgetClasses.size() = " + widgetClasses.size());
        }
        //return null;
    }
*/

    private static boolean overridesEqualsObject(ClassInfo ci) {
        return ci.getMethodInfo("equals").stream()
                .anyMatch(m -> {
                    final MethodParameterInfo[] parameters = m.getParameterInfo();
                    if (parameters.length != 1)
                        return false;
                    final MethodParameterInfo parameter = parameters[0];
                    if (!Object.class.getName().equals(parameter.getTypeSignatureOrTypeDescriptor().toString()))
                        return false;
                    return !m.isNative();
                });
    }

    private static <T, X extends Exception> T wrap(ThrowingSupplier<T, X> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    private static <T> T compose(final T original,
                                 final Consumer<T>... operations) {
        return Stream.of(operations)
                .reduce(original, (t, oper) -> {
                    oper.accept(t);
                    return t;
                }, (a, b) -> a);
    }

    private static Stream<WireType> wires() {
        return Stream.of(WireType.TEXT, WireType.JSON, WireType.BINARY);
    }

    static void assertEqualEnough(Object a, Object b) {
        if (a.getClass() != b.getClass())
            assertEquals(a, b);
        if (a instanceof Throwable) {
            assertEquals(a.getClass(), b.getClass());
            assertEquals(((Throwable) a).getMessage(), ((Throwable) b).getMessage());
        } else if (a instanceof Queue) {
            assertEquals(a.toString(), b.toString());
        } else {
            assertEquals(a, b);
        }
    }

    @TestFactory
    Stream<DynamicTest> test() {
        return DynamicTest.stream(cases(), Objects::toString, wireTypeObject -> {
            final Object source = wireTypeObject.object;
            // Can't handle suclasses of Properties.
            if (source instanceof Properties && source.getClass() != Properties.class)
                return;

            final Bytes<?> bytes = Bytes.allocateElasticDirect();
            try {
                final Wire wire = wireTypeObject.wireType.apply(bytes);
                wire.getValueOut().object((Class) source.getClass(), source);
                final Object target = wire.getValueIn().object(source.getClass());
                if (!(source instanceof Comparable) || ((Comparable) source).compareTo(target) != 0) {
                    if (wireTypeObject.wireType == WireType.JSON || source instanceof EnumMap)
                        assertEquals(source.toString(), target.toString());
                    else
                        assertEqualEnough(source, target);
                }
            } catch (IllegalArgumentException iae) {
                // allow JSON to reject types not supported.
                if (wireTypeObject.wireType == WireType.JSON)
                    return;
                throw iae;
            } finally {
                bytes.releaseLast();
            }
        });
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T, X extends Exception> {
        T get() throws X;
    }

    private static final class WireTypeObject {
        WireType wireType;
        Object object;

        public WireTypeObject(WireType wireType, Object object) {
            this.wireType = wireType;
            this.object = object;
        }

        @Override
        public String toString() {
            try {
                return wireType + ", " + object.getClass().getName() + " : " + object;
            } catch (Throwable t) {
                return t.toString();
            }
        }
    }

}
