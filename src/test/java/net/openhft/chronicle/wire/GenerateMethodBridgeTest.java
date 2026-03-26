/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.Mocker;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

// Two base interfaces with some methods
interface GMBA {
    void method1(String arg);

    void method2(MyTypes myType);
}

interface GMBB {
    void method2(MyTypes myType);

    void method3(String arg);
}

// Interface extending both GMBA and GMBB
interface GMBZ extends GMBA, GMBB {
}

// A JUnit test class
class GenerateMethodBridgeTest extends WireTestCommon {

    @Test
    void createBridge() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Instantiating an object to generate method bridges
        GenerateMethodBridge gmb = new GenerateMethodBridge();

        // Acquiring metadata for the bridge
        GenerateMethodBridge.MethodBridgeMetaData md = gmb.metaData();

        // Adding interfaces to metadata
        md.interfaces().add(GMBZ.class);
        md.invokes().add(GMBA.class);
        md.invokes().add(GMBB.class);
        md.invokes().add(GMBZ.class);

        // Setting package and base class names for the metadata
        md.packageName(Jvm.getPackageName(getClass()));
        md.baseClassName("GMB");

        // Acquiring a class based on the metadata
        Class<GMBZ> aClass = gmb.acquireClass(getClass().getClassLoader());

        // StringWriter to capture logs
        StringWriter sw = new StringWriter();

        // Creating mock implementations for the interfaces with logging functionality
        List<?> args = Arrays.asList(
                Mocker.logging(GMBA.class, "gmba.", sw),
                Mocker.logging(GMBB.class, "gmbb.", sw),
                Mocker.logging(GMBZ.class, "gmbz.", sw)
        );

        // Creating an instance of the generated class using reflection
        GMBZ in = aClass.getDeclaredConstructor(List.class)
                .newInstance(args);

        // Calling methods on the created instance
        in.method1("method1");
        in.method2(new MyTypes());
        in.method3("method3");

        // Validating that the calls were made correctly based on the logs captured in the StringWriter
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
                "gmbz.method3[method3]\n", sw.toString().replace("\r", ""));
    }
}
