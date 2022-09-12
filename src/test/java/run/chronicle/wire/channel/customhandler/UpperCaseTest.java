package run.chronicle.wire.channel.customhandler;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.TextMethodTester;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;
import run.chronicle.wire.channel.channelArith.AnswerListener;
import run.chronicle.wire.channel.channelArith.Calculator;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class UpperCaseTest extends WireTestCommon {

    public static void test(String basename) {
        TextMethodTester<TextMessageOutput> tester = new TextMethodTester<>(
            basename + "/in.yaml",
            out -> new UpperCase().msgOutput(out),
            TextMessageOutput.class,
            basename + "/out.yaml");
        tester.setup(basename + "/setup.yaml");
        try {
            tester.run();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        assertEquals(tester.expected(), tester.actual());
    }


    @Test
    public void testTwo() {
        test("demo-text");
    }

}
