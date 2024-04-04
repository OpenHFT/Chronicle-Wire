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

import net.openhft.chronicle.core.util.Mocker;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;
import org.junit.Test;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class GenerateMethodDelegateTest extends WireTestCommon {

    // Test the validity of class naming conventions
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidName() {
        // Initialize a new GenerateMethodDelegate
        GenerateMethodDelegate gmd = new GenerateMethodDelegate();

        // Set metadata for the generated class with an invalid name
        gmd.metaData().packageName(GenerateMethodDelegateTest.class.getPackage().getName())
                .baseClassName("GMDT-");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testAcquireClass() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        // Initialize a new GenerateMethodDelegate
        GenerateMethodDelegate gmd = new GenerateMethodDelegate();

        // Set metadata for the generated class
        gmd.metaData().packageName(GenerateMethodDelegateTest.class.getPackage().getName())
                .baseClassName("GMDT");

        // Add multiple interfaces to the metadata
        Collections.addAll(gmd.metaData().interfaces(),
                Runnable.class,
                Consumer.class,
                Supplier.class,
                BiConsumer.class);
        Class<?> aClass = gmd.acquireClass(GenerateMethodDelegateTest.class.getClassLoader());
        MethodDelegate md = (MethodDelegate) aClass.getDeclaredConstructor().newInstance();

        // Create a StringWriter for logging
        StringWriter sw = new StringWriter();

        // Set delegate and invoke methods from various interfaces
        md.delegate(Mocker.logging(RCSB.class, "", sw));
        ((Runnable) md).run();
        ((Consumer) md).accept("consumer");
        ((Supplier<?>) md).get();
        ((BiConsumer) md).accept("bi", "consumer");

        // Assert the expected log output
        assertEquals("run[]\n" +
                "accept[consumer]\n" +
                "get[]\n" +
                "accept[bi, consumer]\n", sw.toString().replace("\r", ""));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void chainedDelegate() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        // Create a custom GenerateMethodDelegate with overridden methods for chaining
        GenerateMethodDelegate gmd = new GenerateMethodDelegate() {

            // Specify the delegate type
            @Override
            protected String getDelegateType() {
                return Chained.class.getName().replace("$", ".");
            }

            // Modify delegate creation with an additional chained method
            @Override
            protected SourceCodeFormatter getDelegate(SourceCodeFormatter mainCode, Method method) {
                String method2 = StringUtils.firstLowerCase(method.getDeclaringClass().getSimpleName());
                return super.getDelegate(mainCode, method).append(".").append(method2).append("(\"one\")");
            }
        };

        // Set metadata for the generated class
        gmd.metaData().packageName(GenerateMethodDelegateTest.class.getPackage().getName())
                .baseClassName("GMDTC");
        gmd.metaData().interfaces().add(Chained1.class);

        // Create a StringWriter for logging
        StringWriter sw = new StringWriter();
        Class<?> aClass = gmd.acquireClass(GenerateMethodDelegateTest.class.getClassLoader());
        MethodDelegate md = (MethodDelegate) aClass.getDeclaredConstructor().newInstance();

        // Set delegate and invoke chained methods
        md.delegate(Mocker.logging(Chained.class, "", sw));
        Chained1 c1 = (Chained1) md;
        c1.say("hello");
        c1.say("bye");

        // Assert the expected log output
        assertEquals("chained1[one]\n" +
                "say[hello]\n" +
                "chained1[one]\n" +
                "say[bye]\n", sw.toString().replace("\r", ""));

    }

    // Interface for chained delegates with two methods
    interface Chained {
        Chained1 chained1(String name);

        Chained2 chained2(String name);
    }

    // First chained interface with a single method
    interface Chained1 {
        void say(String text);
    }

    // Second chained interface with a single method
    interface Chained2 {
        void say(String text);
    }

    @SuppressWarnings("rawtypes")
    // A combined interface that extends multiple standard Java interfaces
    interface RCSB extends Runnable, Consumer, Supplier, BiConsumer {
    }
}
