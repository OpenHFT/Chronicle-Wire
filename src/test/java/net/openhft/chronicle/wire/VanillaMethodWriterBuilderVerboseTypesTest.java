package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

@RunWith(value = Parameterized.class)
public class VanillaMethodWriterBuilderVerboseTypesTest {

    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(MyObject.class, MyObject2.class);
    }

    private boolean verboseTypes;
    private final String expects;

    public VanillaMethodWriterBuilderVerboseTypesTest(boolean verboseTypes, String expects) {
        this.verboseTypes = verboseTypes;
        this.expects = expects;
    }

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

    public static class MyObject2 extends SelfDescribingMarshallable {

        private final String str;
        private final int value;

        public MyObject2(String str, int value) {
            this.str = str;
            this.value = value;
        }
    }

    public static class MyObject extends SelfDescribingMarshallable {

        private final ArrayList<MyObject2> list = new ArrayList<>();


        public MyObject(String str, int value) {
            list.add(new MyObject2(str, value));
        }
    }

    interface Printer {
        void print(MyObject msg);
    }

    @Test
    public void test() {

        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        TextWire textWire = new TextWire(bytes);

        VanillaMethodWriterBuilder<Printer> methodWriterBuilder = (VanillaMethodWriterBuilder) textWire.methodWriterBuilder(false, Printer.class);
        methodWriterBuilder.verboseTypes(verboseTypes);
        Printer printer = methodWriterBuilder.build();
        printer.print(new MyObject("hello world", 23));
        Assert.assertEquals(expects, bytes.toString());
    }

}