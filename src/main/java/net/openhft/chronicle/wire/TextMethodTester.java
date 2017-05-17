package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;

import java.io.IOException;
import java.util.function.Function;

/**
 * Created by peter on 17/05/2017.
 */
public class TextMethodTester<T> {
    private final String input;
    private final Class<T> outputClass;
    private final String output;
    private final Function<T, Object> componentFunction;
    private String expected;
    private String actual;

    public TextMethodTester(String input, Function<T, Object> componentFunction, Class<T> outputClass, String output) {
        this.input = input;
        this.outputClass = outputClass;
        this.output = output;
        this.componentFunction = componentFunction;
    }

    public TextMethodTester run() throws IOException {
        Wire wire = new TextWire(BytesUtil.readFile(input));
        Wire wire2 = new TextWire(Bytes.allocateElasticDirect());
        // expected
        expected = BytesUtil.readFile(output).toString().trim().replace("\r", "");
        T writer = wire2.methodWriter(outputClass);
        Object component = componentFunction.apply(writer);
        MethodReader reader = wire.methodReader(component);
        while (reader.readOne()) {
            while (wire2.bytes().peekUnsignedByte(wire2.bytes().writePosition() - 1) == ' ')
                wire2.bytes().writeSkip(-1);
            wire2.bytes().append("---\n");
        }
        actual = wire2.toString().trim();
        return this;
    }

    public String expected() {
        return expected;
    }

    public String actual() {
        return actual;
    }
}
