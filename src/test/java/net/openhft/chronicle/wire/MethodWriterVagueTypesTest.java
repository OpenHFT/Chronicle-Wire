package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * This class tests the behavior of MethodWriter when handling vague/interfaced messages.
 * It extends the WireTestCommon from the `net.openhft.chronicle.wire` package for common test setup and utilities.
 */
@RunWith(value = Parameterized.class)
public class MethodWriterVagueTypesTest extends net.openhft.chronicle.wire.WireTestCommon {
    private ArrayBlockingQueue<Object> singleQ = new ArrayBlockingQueue<>(1);
    private ArrayBlockingQueue<Object> doubleQ = new ArrayBlockingQueue<>(2);
    private final List<Map<Class<?>, Object>> usedObjects = Arrays.asList(new HashMap<>(), new HashMap<>());
    private Class<?>[] prevObjClasses = new Class<?>[2];
    private final Boolean multipleNonMarshallableParamTypes;

    public MethodWriterVagueTypesTest(Boolean multipleNonMarshallableParamTypes) {
        this.multipleNonMarshallableParamTypes = multipleNonMarshallableParamTypes;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{null},
                new Object[]{true},
                new Object[]{false}
        );
    }
    /**
     * An interface defining a single method that accepts a String message.
     */
    interface PrintObjectSingle {
        void msg(Object message);
    }

    interface PrintObjectDouble {
        void msg(Object key, Container message);
    }

    interface PrintPrimitiveDouble {
        void msg(long key, Container message);
    }

    interface PrintFinalObjectDouble {
        void msg(FinalMarshallableContainer m, FinalNonMarshallableContainer nm);
    }

    public static final class FinalMarshallableContainer extends NonMarshallableTestContainer implements Marshallable{}
    public static final class FinalNonMarshallableContainer extends NonMarshallableTestContainer{}

    public static class MarshallableTestContainer extends NonMarshallableTestContainer implements Marshallable {
    }

    public static class NonMarshallableTestContainer implements Container {

        String randomInt = String.valueOf(new Random().nextInt());

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + randomInt + "}";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof NonMarshallableTestContainer && randomInt.equals(((NonMarshallableTestContainer)obj).randomInt);
        }

        @Override
        public int hashCode() {
            return randomInt.hashCode();
        }
    }

    public interface Container{}

    @Test
    public void testSingle() throws Exception {
        // Initialization of the wire
        Wire w = new BinaryWire(Bytes.allocateElasticOnHeap());
        PrintObjectSingle printer = w.methodWriter(PrintObjectSingle.class);

        // Set up a MethodReader to read the String message and process it using the println method
        MethodReader reader = w.methodReaderBuilder()
                .multipleNonMarshallableParamTypes(multipleNonMarshallableParamTypes)
                .build((PrintObjectSingle) message -> {
                    singleQ.add(message);
                });
        // test with a series of objects
        testSingle(printer, reader, new MarshallableTestContainer());
        testSingle(printer, reader, "hello");
        testSingle(printer, reader, new NonMarshallableTestContainer());
        testSingle(printer, reader, new MarshallableTestContainer());
        testSingle(printer, reader, new NonMarshallableTestContainer());
    }

    @Test
    public void testDouble() throws Exception {
        // Initialization of the wire
        Wire w = new TextWire(Bytes.allocateElasticOnHeap());
        PrintObjectDouble printer = w.methodWriter(PrintObjectDouble.class);

        // Set up a MethodReader to read the String message and process it using the println method
        MethodReader reader = w.methodReaderBuilder()
                .multipleNonMarshallableParamTypes(multipleNonMarshallableParamTypes)
                .build((PrintObjectDouble) (key, message) -> {
                    doubleQ.add(key);
                    doubleQ.add(message);
                });
        // test with a series of objects
        testDouble(printer::msg, reader, "key1", new MarshallableTestContainer());
        testDouble(printer::msg, reader, "key2", new NonMarshallableTestContainer());
        testDouble(printer::msg, reader, new MarshallableTestContainer(), new MarshallableTestContainer());
        testDouble(printer::msg, reader, new NonMarshallableTestContainer(), new NonMarshallableTestContainer());
        testDouble(printer::msg, reader, Integer.valueOf(5), new MarshallableTestContainer());
        testDouble(printer::msg, reader, Integer.valueOf(6), new MarshallableTestContainer());
        testDouble(printer::msg, reader, Long.valueOf(3L), new MarshallableTestContainer());
    }

    @Test
    public void testDoubleFinal() throws Exception {
        // Initialization of the wire
        Wire w = new TextWire(Bytes.allocateElasticOnHeap());
        PrintFinalObjectDouble printer = w.methodWriter(PrintFinalObjectDouble.class);

        // Set up a MethodReader to read the String message and process it using the println method
        MethodReader reader = w.methodReaderBuilder()
                .multipleNonMarshallableParamTypes(multipleNonMarshallableParamTypes)
                .build((PrintFinalObjectDouble) (key, message) -> {
                    doubleQ.add(key);
                    doubleQ.add(message);
                });
        // test with a series of objects
        testDouble(printer::msg, reader, new FinalMarshallableContainer(), new FinalNonMarshallableContainer());
        testDouble(printer::msg, reader, new FinalMarshallableContainer(), new FinalNonMarshallableContainer());
    }

    @Test
    public void testPrimitive() throws Exception {
        // Initialization of the wire
        Wire w = new BinaryWire(Bytes.allocateElasticOnHeap());
        PrintPrimitiveDouble printer = w.methodWriter(PrintPrimitiveDouble.class);

        // Set up a MethodReader to read the String message and process it using the println method
        MethodReader reader = w.methodReaderBuilder()
                .multipleNonMarshallableParamTypes(multipleNonMarshallableParamTypes)
                .build((PrintPrimitiveDouble) (key, message) -> {
                    doubleQ.add(key);
                    doubleQ.add(message);
                });
        // test with a series of objects
        testDoubleWithPrimitive(printer, reader, 1L, new MarshallableTestContainer());
        testDoubleWithPrimitive(printer, reader, 2L, new NonMarshallableTestContainer());
        testDoubleWithPrimitive(printer, reader, Long.MAX_VALUE, new MarshallableTestContainer());
        testDoubleWithPrimitive(printer, reader, 2L, new NonMarshallableTestContainer());
    }

    private void testSingle(PrintObjectSingle printer, MethodReader reader, Object obj) throws Exception {
        test(() -> printer.msg(obj), reader, singleQ, obj);
    }

    private <K extends Object, C extends Container> void testDouble(BiConsumer<K, C> printer, MethodReader reader, K key, C obj) throws Exception {
        test(() -> printer.accept(key, obj), reader, doubleQ, key, obj);
    }

    private void testDoubleWithPrimitive(PrintPrimitiveDouble printer, MethodReader reader, long key, Container obj) throws Exception {
        test(() -> printer.msg(key, obj), reader, doubleQ, key, obj);
    }

    private void test(Runnable methodCall, MethodReader reader, ArrayBlockingQueue<Object> queue, Object ... objs) throws Exception {
        methodCall.run();
        boolean marshallableToNonMarshallable = false;
        if (prevObjClasses[0] != null) {
            for (int i=0; i < objs.length; i++) {
                marshallableToNonMarshallable |= Marshallable.class.isAssignableFrom(prevObjClasses[i]) && !(objs[i] instanceof Marshallable) && !(objs[i] instanceof Number);
            }
        }
        if (Boolean.FALSE.equals(multipleNonMarshallableParamTypes) && marshallableToNonMarshallable) {
            Assert.assertThrows(RuntimeException.class, () -> assertWrite(reader, queue, objs));
        } else {
            assertWrite(reader, queue, objs);
        }

        Assert.assertTrue("Reception Queue should be empty", queue.isEmpty());
    }

    private void assertWrite(MethodReader reader, ArrayBlockingQueue<Object> queue, Object ... objs) throws Exception {
        reader.readOne();

        String classMismatchString = "";
        for (int i = 0; i < objs.length; i++) {
            Object obj = objs[i];
            // Fetch the read message from the blocking queue with a timeout
            Object result = queue.poll(10, TimeUnit.SECONDS);
            // Verify that the fetched message matches the expected content
            Class<?> objClass = obj.getClass();
            if (objClass != result.getClass()) {
                classMismatchString = "Invalid class type! " + objClass.getSimpleName() + " != " + result.getClass().getSimpleName();
                continue;
            }
            Assert.assertEquals(obj, result);
            prevObjClasses[i] = objClass;
            Object usedObj = usedObjects.get(i).get(objClass);
            if (usedObj != null) {
                if (objClass != String.class && !Number.class.isAssignableFrom(objClass) && (!Boolean.FALSE.equals(multipleNonMarshallableParamTypes) ||
                        Marshallable.class.isAssignableFrom(objClass) || Modifier.isFinal(objClass.getModifiers()))) {
                    Assert.assertSame(usedObj, result);
                } else {
                    Assert.assertNotSame(usedObj, result);
                }
            }
            usedObjects.get(i).put(objClass, result);
        }
        if (!classMismatchString.isEmpty()) {
            throw new RuntimeException(classMismatchString);
        }
    }
}
