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

    private String setup;
    private Function<String, String> afterRun;

    private String expected;
    private String actual;

    public TextMethodTester(String input, Function<T, Object> componentFunction, Class<T> outputClass, String output) {
        this.input = input;
        this.outputClass = outputClass;
        this.output = output;
        this.componentFunction = componentFunction;
    }

    public String setup() {
        return setup;
    }

    public TextMethodTester setup(String setup) {
        this.setup = setup;
        return this;
    }

    public Function<String, String> afterRun() {
        return afterRun;
    }

    public TextMethodTester afterRun(Function<String, String> afterRun) {
        this.afterRun = afterRun;
        return this;
    }

    public TextMethodTester run() throws IOException {

        Wire wire2 = new TextWire(Bytes.allocateElasticDirect()).useTextDocuments();
        T writer = wire2.methodWriter(outputClass);
        Object component = componentFunction.apply(writer);

        if (setup != null) {
            Wire wire0 = new TextWire(BytesUtil.readFile(setup)).useTextDocuments();

            MethodReader reader0 = wire0.methodReader(component);
            while (reader0.readOne()) {
                wire2.bytes().clear();
            }
            wire2.bytes().clear();
        }

        Wire wire = new TextWire(BytesUtil.readFile(input)).useTextDocuments();

        // expected
        expected = BytesUtil.readFile(output).toString().trim().replace("\r", "");
        MethodReader reader = wire.methodReader(component);
        while (reader.readOne()) {
            while (wire2.bytes().peekUnsignedByte(wire2.bytes().writePosition() - 1) == ' ')
                wire2.bytes().writeSkip(-1);
            wire2.bytes().append("---\n");
        }
        actual = wire2.toString().trim();
        if (afterRun != null) {
            expected = afterRun.apply(expected);
            actual = afterRun.apply(actual);
        }
        return this;
    }

    public String expected() {
        return expected;
    }

    public String actual() {
        return actual;
    }
}
