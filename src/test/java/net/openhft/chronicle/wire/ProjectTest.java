/*
 * Copyright 2016-2020 chronicle.software
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

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ProjectTest extends WireTestCommon {

    @NotNull
    @Rule
    public TestName name = new TestName();

    @NotNull
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        Object[][] list = {
                {WireType.BINARY},
                {WireType.TEXT},
                {WireType.YAML_ONLY},
                {WireType.JSON}
        };
        return Arrays.asList(list);
    }

    @SuppressWarnings("unchecked")
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
        Assert.assertEquals("some data", project.inner().name());
    }

    @SuppressWarnings("rawtypes")
    static class Dto1 extends SelfDescribingMarshallable {
        @NotNull
        Map m = new HashMap<>();
        String anotherField;
        long someValue;
    }

    @SuppressWarnings("rawtypes")
    static class Dto2 extends SelfDescribingMarshallable {
        long someValue;
        String anotherField;
        @NotNull
        Map m = new HashMap<>();
    }

    public static class Inner extends SelfDescribingMarshallable {
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

    public static class Outer extends SelfDescribingMarshallable {
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
