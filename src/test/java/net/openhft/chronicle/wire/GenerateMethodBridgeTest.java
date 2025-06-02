/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.Mocker;
import org.junit.Test;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
public class GenerateMethodBridgeTest extends WireTestCommon {

    @Test
    public void createBridge() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

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
                        "gmbz.method3[method3]\n",
                sw.toString().replace("\r", ""));
    }
}
