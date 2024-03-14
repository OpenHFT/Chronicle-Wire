package net.openhft.chronicle.wire.generated;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;

/**
 * Represents a service with methods for demonstration of the Chronicle Wire's method
 * writer and reader functionality.
 */
interface MyService {
    /**
     * A demonstration method that prints its arguments.
     *
     * @param arg1 An integer argument.
     * @param arg2 A string argument.
     */
    void foo(int arg1, String arg2);

    /**
     * Another demonstration method that prints its arguments and returns a result.
     *
     * @param arg1 A string argument.
     * @param arg2 A long argument.
     * @return Length of the string argument added to the long argument casted to int.
     */
    int bar(String arg1, long arg2);
}

/**
 * Implementation of the MyService interface for demonstrating method serialization and deserialization.
 */
class MyServiceImpl implements MyService {
    @Override
    public void foo(int arg1, String arg2) {
        System.out.printf("MyServiceImpl.foo(%d, %s)\n", arg1, arg2);
    }

    @Override
    public int bar(String arg1, long arg2) {
        System.out.printf("MyServiceImpl.bar(%s, %d)\n", arg1, arg2);
        return arg1.length() + (int) arg2;
    }
}

/**
 * Demonstrates the Chronicle Wire library's method writer and reader capabilities.
 * The example showcases serializing method calls and their arguments, and later
 * deserializing and executing these methods.
 */
public class ChronicleWireMethodExample {

    // GPT-4 Generated example
    public static void main(String[] args) {
        // Create an instance of the MyService implementation
        MyService service = new MyServiceImpl();

        // Initialize the Bytes buffer and obtain a MethodWriter for the MyService interface
        Bytes<?> bytes = Bytes.elasticByteBuffer();
        Wire wire = WireType.BINARY.apply(bytes);
        MyService writer = wire.methodWriter(MyService.class);

        // Invoke methods using the MethodWriter, which serializes the method invocations
        writer.foo(123, "hello world");
        int result = writer.bar("hello", 12345L);

        // Set up a MethodReader to deserialize and execute method invocations
        Wire wire2 = WireType.BINARY.apply(bytes);
        int counter = 0;
        MethodReader reader = wire2.methodReader(service, new MyServiceImpl());

        // Read and process serialized method invocations from the BytesStore
        while (reader.readOne()) {
            counter++;
        }

        // Assert that the expected number of method invocations were processed
        assert (counter == 2);

        // Display the result of the serialized and executed 'bar' method
        System.out.println("Result: " + result);

        // Release resources associated with the Bytes buffer
        bytes.releaseLast();
    }
}
