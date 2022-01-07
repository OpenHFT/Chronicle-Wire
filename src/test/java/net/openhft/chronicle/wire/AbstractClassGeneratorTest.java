package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;
import org.junit.Test;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

class SimpleClassGenerator extends AbstractClassGenerator<SimpleMetaData> {
    protected SimpleClassGenerator() {
        super(new SimpleMetaData());
        metaData().interfaces().add(Callable.class);
    }

    @Override
    protected void generateMethod(Method method, StringBuilder params, List<String> paramList, SourceCodeFormatter mainCode) {
        assertEquals("call", method.getName());
        withLineNumber(mainCode)
                .append("return \"").append(metaData().message).append("\";\n");
    }
}

class UIClassGenerator extends AbstractClassGenerator<SimpleMetaData> {
    protected UIClassGenerator() {
        super(new SimpleMetaData());
        metaData().interfaces().add(Consumer.class);
    }

    @Override
    protected void generateConstructors(SourceCodeFormatter mainCode) {
        super.generateConstructors(mainCode);
        mainCode.append("public ").append(className()).append("(").append(nameForClass(UpdateInterceptor.class)).append(" updateInterceptor) {\n")
                .append("this.updateInterceptor = updateInterceptor;\n")
                .append("}\n");
    }

    @Override
    protected void generateMethod(Method method, StringBuilder params, List<String> paramList, SourceCodeFormatter mainCode) {
        switch (method.getName()) {
            case "accept":
                withLineNumber(mainCode)
                        .append("((").append(nameForClass(MyTypes.class)).append(")").append(params).append(").text().append('-').append(\"").append(metaData().message).append("\");\n");
                break;
            case "andThen":
                withLineNumber(mainCode)
                        .append("return this;\n");
                break;
            default:
                fail("Unexpected method " + method.getName());
        }
    }
}

class SimpleMetaData extends AbstractClassGenerator.MetaData<SimpleMetaData> {
    public String message;
}

public class AbstractClassGeneratorTest extends WireTestCommon {
    @Test
    public void simpleGenerator() throws Exception {
        doTest("Hello World");
        doTest("Bye now");
        doTest("The time is " + LocalDateTime.now());
    }

    protected void doTest(String message) throws Exception {
        SimpleClassGenerator scg = new SimpleClassGenerator();
        scg.metaData()
                .packageName(getClass().getPackage().getName())
                .baseClassName("ACGT")
                .message = message;
        Class<Callable> aClass = scg.acquireClass(getClass().getClassLoader());
        Callable<String> callable = aClass.getDeclaredConstructor().newInstance();
        // break point on the next line to be able to debug the generated class.
        String call = callable.call();
        assertEquals(message, call);
    }

    @Test
    public void useInterceptor() throws Exception {
        StringWriter sw = new StringWriter();
        UpdateInterceptor ui = (methodName, t) -> {
            sw.append(methodName).append(": ").append(String.valueOf(t));
            boolean block = !((MyTypes) t).text().toString().equals("block");
            sw.append("return: ").append(String.valueOf(block)).append("\n\n");
            return block;
        };
        doTest(ui, "Hello World");
        doTest(ui, "block");
        String theTimeIs = "The time is " + LocalDateTime.now();
        doTest(ui, theTimeIs);
        assertEquals("accept: !net.openhft.chronicle.wire.MyTypes {\n" +
                "  text: Hello World,\n" +
                "  flag: false,\n" +
                "  b: 0,\n" +
                "  s: 0,\n" +
                "  ch: \"\\0\",\n" +
                "  i: 0,\n" +
                "  f: 0.0,\n" +
                "  d: 0.0,\n" +
                "  l: 0\n" +
                "}\n" +
                "return: true\n" +
                "\n" +
                "accept: !net.openhft.chronicle.wire.MyTypes {\n" +
                "  text: block,\n" +
                "  flag: false,\n" +
                "  b: 0,\n" +
                "  s: 0,\n" +
                "  ch: \"\\0\",\n" +
                "  i: 0,\n" +
                "  f: 0.0,\n" +
                "  d: 0.0,\n" +
                "  l: 0\n" +
                "}\n" +
                "return: false\n" +
                "\n" +
                "accept: !net.openhft.chronicle.wire.MyTypes {\n" +
                "  text: \"" + theTimeIs + "\",\n" +
                "  flag: false,\n" +
                "  b: 0,\n" +
                "  s: 0,\n" +
                "  ch: \"\\0\",\n" +
                "  i: 0,\n" +
                "  f: 0.0,\n" +
                "  d: 0.0,\n" +
                "  l: 0\n" +
                "}\n" +
                "return: true\n" +
                "\n", sw.toString());
    }

    protected void doTest(UpdateInterceptor ui, String message) throws Exception {
        UIClassGenerator scg = new UIClassGenerator();
        scg.metaData()
                .packageName(getClass().getPackage().getName())
                .baseClassName("ACGTUI")
                .useUpdateInterceptor(true)
                .message = message;
        Class<Consumer> aClass = scg.acquireClass(getClass().getClassLoader());
        Consumer callable = aClass.getDeclaredConstructor(UpdateInterceptor.class).newInstance(ui);
        // break point on the next line to be able to debug the generated class.
        MyTypes mt = new MyTypes().text(message);
        callable.accept(mt);
        String expected = message.equals("block") ? message : (message + '-' + message);
        assertEquals(expected, mt.text().toString());
    }
}
