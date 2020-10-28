package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Mocker;
import org.junit.Test;

import java.io.StringWriter;
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

    interface RCSB extends Runnable, Consumer, Supplier, BiConsumer {
    }
}