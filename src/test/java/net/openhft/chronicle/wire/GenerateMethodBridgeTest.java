package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Mocker;
import org.junit.Test;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

interface GMBA {
    void method1(String arg);

    void method2(MyTypes myType);
}

interface GMBB {
    void method2(MyTypes myType);

    void method3(String arg);
}

interface GMBZ extends GMBA, GMBB {
}

public class GenerateMethodBridgeTest {

    @Test
    public void createBridge() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        GenerateMethodBridge gmb = new GenerateMethodBridge();
        GenerateMethodBridge.MethodBridgeMetaData md = gmb.metaData();
        md.interfaces().add(GMBZ.class);
        md.handlers().add(GMBA.class);
        md.handlers().add(GMBB.class);
        md.handlers().add(GMBZ.class);
        md.packageName(getClass().getPackage().getName());
        md.baseClassName("GMB");
        Class<GMBZ> aClass = gmb.acquireClass(getClass().getClassLoader());
        StringWriter sw = new StringWriter();
        List args = Arrays.asList(
                Mocker.logging(GMBA.class, "gmba.", sw),
                Mocker.logging(GMBB.class, "gmbb.", sw),
                Mocker.logging(GMBZ.class, "gmbz.", sw)
        );
        GMBZ in = aClass.getDeclaredConstructor(List.class)
                .newInstance(args);
        in.method1("method1");
        in.method2(new MyTypes());
        in.method3("method3");
        assertEquals("gmba.method1[method1]\n" +
                        "gmbz.method1[method1]\n" +
                        "gmba.method2[!net.openhft.chronicle.wire.MyTypes {\n" +
                        "  text: \"\",\n" +
                        "  flag: false,\n" +
                        "  b: 0,\n" +
                        "  s: 0,\n" +
                        "  ch: \"\\0\",\n" +
                        "  i: 0,\n" +
                        "  f: 0.0,\n" +
                        "  d: 0.0,\n" +
                        "  l: 0\n" +
                        "}\n" +
                        "]\n" +
                        "gmbb.method2[!net.openhft.chronicle.wire.MyTypes {\n" +
                        "  text: \"\",\n" +
                        "  flag: false,\n" +
                        "  b: 0,\n" +
                        "  s: 0,\n" +
                        "  ch: \"\\0\",\n" +
                        "  i: 0,\n" +
                        "  f: 0.0,\n" +
                        "  d: 0.0,\n" +
                        "  l: 0\n" +
                        "}\n" +
                        "]\n" +
                        "gmbz.method2[!net.openhft.chronicle.wire.MyTypes {\n" +
                        "  text: \"\",\n" +
                        "  flag: false,\n" +
                        "  b: 0,\n" +
                        "  s: 0,\n" +
                        "  ch: \"\\0\",\n" +
                        "  i: 0,\n" +
                        "  f: 0.0,\n" +
                        "  d: 0.0,\n" +
                        "  l: 0\n" +
                        "}\n" +
                        "]\n" +
                        "gmbb.method3[method3]\n" +
                        "gmbz.method3[method3]\n",
                sw.toString().replace("\r", ""));
    }
}
