package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;
import org.junit.Test;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class GenerateMethodDelegateTest extends WireTestCommon {

    @Test
    public void testAcquireClass() throws IllegalAccessException, InstantiationException {
        GenerateMethodDelegate gmd = new GenerateMethodDelegate();
        gmd.metaData().packageName(GenerateMethodDelegateTest.class.getPackage().getName())
                .baseClassName("GMDT");

        Collections.addAll(gmd.metaData().interfaces(),
                Runnable.class,
                Consumer.class,
                Supplier.class,
                BiConsumer.class);
        Class aClass = gmd.acquireClass(GenerateMethodDelegateTest.class.getClassLoader());
        MethodDelegate md = (MethodDelegate) aClass.newInstance();
        StringWriter sw = new StringWriter();
        md.delegate(Mocker.logging(RCSB.class, "", sw));
        ((Runnable) md).run();
        ((Consumer) md).accept("consumer");
        ((Supplier) md).get();
        ((BiConsumer) md).accept("bi", "consumer");
        assertEquals("run[]\n" +
                "accept[consumer]\n" +
                "get[]\n" +
                "accept[bi, consumer]\n", sw.toString().replace("\r", ""));
    }

    @Test
    public void chainedDelegate() throws IllegalAccessException, InstantiationException {
        GenerateMethodDelegate gmd = new GenerateMethodDelegate() {
            @Override
            protected String getDelegateType() {
                return Chained.class.getName().replace("$", ".");
            }

            @Override
            protected SourceCodeFormatter getDelegate(SourceCodeFormatter mainCode, Method method) {
                String method2 = StringUtils.firstLowerCase(method.getDeclaringClass().getSimpleName());
                return super.getDelegate(mainCode, method).append(".").append(method2).append("(\"one\")");
            }
        };
        gmd.metaData().packageName(GenerateMethodDelegateTest.class.getPackage().getName())
                .baseClassName("GMDTC");
        gmd.metaData().interfaces().add(Chained1.class);
        StringWriter sw = new StringWriter();
        Class aClass = gmd.acquireClass(GenerateMethodDelegateTest.class.getClassLoader());
        MethodDelegate md = (MethodDelegate) aClass.newInstance();
        md.delegate(Mocker.logging(Chained.class, "", sw));
        Chained1 c1 = (Chained1) md;
        c1.say("hello");
        c1.say("bye");
        assertEquals("chained1[one]\n" +
                "say[hello]\n" +
                "chained1[one]\n" +
                "say[bye]\n", sw.toString().replace("\r", ""));

    }

    interface Chained {
        Chained1 chained1(String name);

        Chained2 chained2(String name);
    }

    interface Chained1 {
        void say(String text);
    }

    interface Chained2 {
        void say(String text);
    }

    interface RCSB extends Runnable, Consumer, Supplier, BiConsumer {
    }
}