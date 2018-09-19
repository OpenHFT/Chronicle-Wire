/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.BytesUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.Parameterized;

import java.util.*;

/**
 * @author Rob Austin.
 */
public class ProjectTest {

    @NotNull
    @Rule
    public TestName name = new TestName();

    @NotNull
    @Parameterized.Parameters
    public static Collection<Object[]> data() {

        @NotNull final List<Object[]> list = new ArrayList<>();
        list.add(new Object[]{WireType.BINARY});
        list.add(new Object[]{WireType.TEXT});
        //      list.add(new Object[]{WireType.RAW});
        return list;
    }

    @Test
    public void testProject() throws Exception {
        @NotNull Dto1 dto1 = new Dto1();
        dto1.m.put("some", "data");
        dto1.anotherField = "someString";
        dto1.someValue = 1;

        Dto2 dto2 = Wires.project(Dto2.class, dto1);

        Assert.assertEquals(dto2.someValue, dto1.someValue);
        Assert.assertEquals(dto2.anotherField, dto1.anotherField);
        Assert.assertEquals(dto2.m, dto1.m);

    }

    @Test
    public void testProjectWithNestedMarshallable() {

        @NotNull final Simple simple = new Simple();
        @NotNull final Inner inner = new Inner();
        inner.name("some data");
        simple.inner(inner);
        simple.name2("hello");
        simple.name2("world");

        final Outer project = Wires.project(Outer.class, simple);
        System.out.println(project);

        Assert.assertTrue(project.inner().name().equals("some data"));
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    static class Dto1 extends AbstractMarshallable {
        @NotNull
        Map m = new HashMap<>();
        String anotherField;
        long someValue;
    }

    static class Dto2 extends AbstractMarshallable {
        long someValue;
        String anotherField;
        @NotNull
        Map m = new HashMap<>();
    }

    public static class Inner extends AbstractMarshallable {
        private String name;

        public String name() {
            return name;
        }

        @NotNull
        public Inner name(String name) {
            this.name = name;
            return this;
        }
    }

    public static class Outer extends AbstractMarshallable {
        private Inner inner;

        public Inner inner() {
            return inner;
        }

        @NotNull
        public Outer inner(Inner inner) {
            this.inner = inner;
            return this;
        }
    }

    public static class Simple extends Outer {
        private String name2;

        public String name2() {
            return name2;
        }

        @NotNull
        public Simple name2(String name2) {
            this.name2 = name2;
            return this;
        }
    }
}
