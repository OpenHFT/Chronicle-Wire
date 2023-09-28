package net.openhft.chronicle.wire.generated;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;

interface MyService {
    void foo(int arg1, String arg2);

    int bar(String arg1, long arg2);
}

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

public class ChronicleWireMethodExample {

    // GPT-4 Generated example
    public static void main(String[] args) {

        MyService service = new MyServiceImpl();

        // Create a MethodWriter
        Bytes<?> bytes = Bytes.elasticByteBuffer();
        Wire wire = WireType.BINARY.apply(bytes);
        MyService writer = wire.methodWriter(MyService.class);

        // Call methods on the MethodWriter
        writer.foo(123, "hello world");
        int result = writer.bar("hello", 12345L);

        // Create a MethodReader
        Wire wire2 = WireType.BINARY.apply(bytes);
        int counter = 0;
        MethodReader reader = wire2.methodReader(service, new MyServiceImpl());

        // Read and process the method calls from the BytesStore
        while (reader.readOne()) {
            counter++;
        }

        // Check that the correct number of method calls were read
        assert (counter == 2);

        // Print the return value of the second method call
        System.out.println("Result: " + result);

        // Clean up
        bytes.releaseLast();
    }
}
