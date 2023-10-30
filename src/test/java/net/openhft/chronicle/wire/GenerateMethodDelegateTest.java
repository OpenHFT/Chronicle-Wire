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

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidName() {
        GenerateMethodDelegate gmd = new GenerateMethodDelegate();
        gmd.metaData().packageName(GenerateMethodDelegateTest.class.getPackage().getName())
                .baseClassName("GMDT-");
    }

    @Test
    public void testAcquireClass() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        GenerateMethodDelegate gmd = new GenerateMethodDelegate();
        gmd.metaData().packageName(GenerateMethodDelegateTest.class.getPackage().getName())
                .baseClassName("GMDT");

        Collections.addAll(gmd.metaData().interfaces(),
                Runnable.class,
                Consumer.class,
                Supplier.class,
                BiConsumer.class);
        Class aClass = gmd.acquireClass(GenerateMethodDelegateTest.class.getClassLoader());
        MethodDelegate md = (MethodDelegate) aClass.getDeclaredConstructor().newInstance();
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
    public void chainedDelegate() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
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
        MethodDelegate md = (MethodDelegate) aClass.getDeclaredConstructor().newInstance();
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