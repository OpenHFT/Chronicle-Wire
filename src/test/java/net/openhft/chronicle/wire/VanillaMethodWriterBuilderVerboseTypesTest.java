package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

@RunWith(value = Parameterized.class)
public class VanillaMethodWriterBuilderVerboseTypesTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Static initialization block to alias two classes
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(MyObject.class, MyObject2.class);
    }

    // Flag to determine if verbose types should be used
    private boolean verboseTypes;

    // Expected string representation for the current test run
    private final String expects;

    // Constructor initializes fields with parameterized values
    public VanillaMethodWriterBuilderVerboseTypesTest(boolean verboseTypes, String expects) {
        this.verboseTypes = verboseTypes;
        this.expects = expects;
    }

    // Provide different combinations of parameters for the test runs
    @NotNull
    @Parameterized.Parameters(name = "verboseTypes={0}, expected={1}")
    public static Collection<Object[]> combinations() {
        return Arrays.asList(new Object[]{true, "print: !MyObject {\n" +
                "  list: [\n" +
                "    { str: hello world, value: 23 }\n" +
                "  ]\n" +
                "}\n" +
                "...\n"}, new Object[]{false, "print: {\n" +
                "  list: [\n" +
                "    { str: hello world, value: 23 }\n" +
                "  ]\n" +
                "}\n" +
                "...\n"});
    }

    // Nested class representing a specific object with a string and value
    public static class MyObject2 extends SelfDescribingMarshallable {

        private final String str;
        private final int value;

        public MyObject2(String str, int value) {
            this.str = str;
            this.value = value;
        }
    }

    // Nested class representing an object containing a list of `MyObject2`
    public static class MyObject extends SelfDescribingMarshallable {

        private final ArrayList<MyObject2> list = new ArrayList<>();

        public MyObject(String str, int value) {
            list.add(new MyObject2(str, value));
        }
    }

    // Interface defining a printing method
    interface Printer {
        void print(MyObject msg);
    }

    // Test case to validate the output of the method writer based on the verbose types setting
    @Test
    public void test() {
        // Allocate elastic bytes on heap and create a TextWire instance
        final Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap();
        TextWire textWire = new TextWire(bytes);

        // Configure method writer builder with verbosity settings
        VanillaMethodWriterBuilder<Printer> methodWriterBuilder = (VanillaMethodWriterBuilder) textWire.methodWriterBuilder(false, Printer.class);
        methodWriterBuilder.verboseTypes(verboseTypes);

        // Create a printer instance and print a message
        Printer printer = methodWriterBuilder.build();
        printer.print(new MyObject("hello world", 23));

        // Assert that the output matches the expected representation for the current run
        Assert.assertEquals(expects, bytes.toString());
    }
}
