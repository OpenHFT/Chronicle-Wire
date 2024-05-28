/*
 * Copyright 2016-2022 chronicle.software
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

import io.github.classgraph.*;
import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Disabled;
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

// Test class to verify serializable objects with Wire.
final class SerializableObjectTest extends WireTestCommon {

    // Constant to represent a specific time.
    private static final long TIME_MS = 1_000_000_000;

    // Define packages which should be ignored.
    private static final Set<String> IGNORED_PACKAGES = Stream.of(
                    // Various packages to ignore during the test
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
                    "apple.security",
                    // Requires non-headless so skip these classes
                    "java.awt",
                    // Do not test classes from the maven plugins
                    "org.apache.maven"
            )
            .collect(Collectors.collectingAndThen(toSet(), Collections::unmodifiableSet));  // Collect into an unmodifiable set for safety.

    // Define classes which should be ignored.
    private static final Set<Class<?>> IGNORED_CLASSES = new HashSet<>(Arrays.asList(
            DoubleSummaryStatistics.class, // Specific classes to exclude from testing.
            DriverPropertyInfo.class,
            SimpleDateFormat.class
    ));

    // Static block to handle specific classes that fail in certain Java versions.
    static {

        try {
            // Include this class for exclusion as it fails in Java 11.
            final Class<?> aClass = Class.forName("com.sun.jndi.toolkit.ctx.Continuation");
            IGNORED_CLASSES.add(aClass);
        } catch (ClassNotFoundException ignore) {
            // This exception means the class isn't present, so we can safely ignore it.
        }
    }

    // Predicate to check if a constructor is the default one.
    private static final Predicate<MethodInfo> CONSTRUCTOR_IS_DEFAULT = methodInfo -> methodInfo.isPublic() && methodInfo.getTypeDescriptor().getTypeParameters().isEmpty();
    // Filter to exclude the ignored packages.
    private static final ClassInfoList.ClassInfoFilter NOT_IGNORED = ci -> IGNORED_PACKAGES.stream().noneMatch(ip -> ci.getPackageName().startsWith(ip));

    // Return test cases for different wire types and objects.
    private static Stream<WireTypeObject> cases() {
        return wires()
                .flatMap(wt -> mergedObjects().map(o -> new WireTypeObject(wt, o)));  // Combine wire types with objects.
    }

    // Merge the handcrafted and reflected objects.
    private static Stream<Object> mergedObjects() {
        // Create a map of objects using their class names.
        Map<String, Object> map = handcraftedObjects()
                .collect(Collectors.toMap(o -> {
                    final Class<?> aClass = o.getClass();
                    IGNORED_CLASSES.add(aClass);  // Update the ignored classes list.
                    return aClass.getName();
                }, Function.identity()));  // Use the object itself as the value.

        // Add reflected objects to the map.
        reflectedObjects()
                .forEach(o -> map.put(o.getClass().getName(), o));
        return map.values().stream();  // Return the values as a stream.
    }

    // Generates a stream of manually created objects to be tested.
    private static Stream<Object> handcraftedObjects() {
        return Stream.of(
                // java.lang
                true,
                (byte) 1,
                '2',
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
        ).filter(SerializableObjectTest::isSerializableEqualsByObject);  // Retain only those objects that are serializable and equivalent when reconstituted.
    }

    private static Stream<Object> reflectedObjects() {
        try (ScanResult scanResult = new ClassGraph().enableSystemJarsAndModules().enableAllInfo().scan()) {
            // Use ClassGraph to scan for all classes implementing Serializable.
            final ClassInfoList widgetClasses = scanResult.getClassesImplementing(Serializable.class)
                    .filter(ci -> !ci.isAbstract())  // Exclude abstract classes.
                    .filter(ClassInfo::isPublic)  // Only consider public classes.
                    .filter(NOT_IGNORED)  // Exclude classes from ignored packages.
                    .filter(ci -> !ci.isAnonymousInnerClass())  // No anonymous inner classes.
                    .filter(ci -> !ci.extendsSuperclass(LookAndFeel.class)) // Excludes classes which extend LookAndFeel.
                    .filter(ci -> !ci.implementsInterface(DesktopManager.class)) // Excludes classes implementing DesktopManager.
                    .filter(ci -> ci.getConstructorInfo().stream().anyMatch(CONSTRUCTOR_IS_DEFAULT)) // Only classes with a default constructor.
                    .filter(SerializableObjectTest::isSerializableEquals);  // Ensure that it's serializable and equivalent upon reconstitution.

            List<Object> objects = widgetClasses.stream()
                    .filter(c -> !IGNORED_CLASSES.contains(c.loadClass(true)))  // Filter out classes from the ignored list.
                    .filter(SerializableObjectTest::overridesEqualsObject)  // Ensure the class overrides equals() method.
                    .map(ci -> ci.loadClass(true))  // Load the actual class.
                    .filter(Objects::nonNull)  // Filter out nulls.
                    .map(SerializableObjectTest::createOrNull)  // Create an instance or return null if not possible.
                    .filter(Objects::nonNull)  // Filter out nulls.
                    .collect(Collectors.toList());

            // Uncomment below to see the counts and details of the discovered classes.
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

    // Check if a ClassInfo object is serializable by loading the class and invoking the serialization check
    private static boolean isSerializableEquals(ClassInfo ci) {
        return isSerializableEquals(ci.loadClass(), null);
    }

    // Check if the given object is serializable by checking its class
    private static boolean isSerializableEqualsByObject(Object o) {
        return isSerializableEquals(o.getClass(), o);
    }

    /**
     * Determines if a given class is serializable and whether its serialized form can be deserialized
     * back to an object that is equal to the original.
     *
     * @param aClass The class to check.
     * @param o An optional instance of the class to check. If null, a new instance will be created.
     * @return true if the class is serializable and deserializable, and the original and deserialized objects are equal; false otherwise.
     */
    private static boolean isSerializableEquals(Class<?> aClass, Object o) {
        try {
            // Create an instance if not provided
            Object source = o == null ? aClass.getConstructor().newInstance() : o;
            // Sanity check to ensure non-null toString representation
            if (source.toString() == null)
                return false;

            // Attempt to serialize the object
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(source);

            // Deserialize the object from the serialized form
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            Object source2 = ois.readObject();

            // Compare the original and deserialized objects for equality
            if (source instanceof Throwable) {
                return source.getClass() == source2.getClass()
                        && Objects.equals(((Throwable) source).getMessage(), ((Throwable) source2).getMessage());
            } else {
                return Objects.equals(source, source2);
            }
        } catch (InstantiationException | NotSerializableException | IllegalAccessException t) {
            return false;
        } catch (Throwable t) {
            System.out.println(aClass + ": " + t);
            return false;
        }
    }

    // Tries to create a new instance of the specified class. If it fails, returns null.
    private static <T> T createOrNull(final Class<T> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (ReflectiveOperationException ignore) {
            return null;
        }
    }

    // Check if the given ClassInfo overrides the equals method with Object as a parameter
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

    // Wrap a ThrowingSupplier's get method to propagate its checked exceptions as runtime exceptions
    private static <T, X extends Exception> T wrap(ThrowingSupplier<T, X> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Compose multiple operations on an original object and return the modified object
    @SuppressWarnings("unchecked")
    @SafeVarargs
    private static <T> T compose(final T original,
                                 final Consumer<T>... operations) {
        for (Consumer<T> operation : operations)
            operation.accept(original);
        return original;
    }

    // Return a Stream of WireType enumerations for testing
    private static Stream<WireType> wires() {
        return Stream.of(WireType.TEXT, WireType.JSON, WireType.BINARY);
    }

    // Assert that two objects are "equivalent" based on certain conditions
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Disabled("https://github.com/OpenHFT/Chronicle-Wire/issues/482")
    @TestFactory
    Stream<DynamicTest> test() {
        return DynamicTest.stream(cases(), Objects::toString, wireTypeObject -> {
            final Object source = wireTypeObject.object;
            // Exclude handling of subclasses of Properties
            if (source instanceof Properties && source.getClass() != Properties.class)
                return;

            final Bytes<?> bytes = Bytes.allocateElasticDirect();
            try {
                // Serialize and deserialize the object using the wire type
                final Wire wire = wireTypeObject.wireType.apply(bytes);
                wire.getValueOut().object((Class) source.getClass(), source);
                final Object target = wire.getValueIn().object(source.getClass());

                // Assert the source and target objects are equivalent
                if (!(source instanceof Comparable) || ((Comparable) source).compareTo(target) != 0) {
                    if (wireTypeObject.wireType == WireType.JSON || source instanceof EnumMap)
                        assertEquals(source.toString(), target.toString());
                    else
                        assertEqualEnough(source, target);
                }
            } catch (IllegalArgumentException iae) {
                // Allow JSON wire type to reject unsupported types
                if (wireTypeObject.wireType == WireType.JSON)
                    return;
                throw iae;
            } finally {
                // Release the allocated bytes
                bytes.releaseLast();
            }
        });
    }

    // Functional interface representing suppliers that can throw exceptions
    @FunctionalInterface
    public interface ThrowingSupplier<T, X extends Exception> {
        T get() throws X;
    }

    // Inner class representing a pairing of WireType and object for testing
    private static final class WireTypeObject {
        WireType wireType;
        Object object;

        public WireTypeObject(WireType wireType, Object object) {
            this.wireType = wireType;
            this.object = object;
        }

        // Provide a descriptive string representation for this WireTypeObject
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
