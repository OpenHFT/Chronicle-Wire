/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

// This class tests the functionalities related to the projection of wire data.
public class ProjectTest extends WireTestCommon {

    // Rule to retrieve the name of the currently-running test
    @NotNull
    public String name;

    // Test case to verify the projection functionality between two data transfer objects.
    @SuppressWarnings("unchecked")
    @Test
    public void testProject() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Initialize the first DTO with sample data
        @NotNull Dto1 dto1 = new Dto1();
        dto1.m.put("some", "data");
        dto1.anotherField = "someString";
        dto1.someValue = 1;

        // Project the data from DTO1 to DTO2
        Dto2 dto2 = Wires.project(Dto2.class, dto1);

        // Assert that the data has been correctly projected
        Assertions.assertEquals(dto2.someValue, dto1.someValue);
        Assertions.assertEquals(dto2.anotherField, dto1.anotherField);
        Assertions.assertEquals(dto2.m, dto1.m);

    }

    // Test case to verify the projection functionality with nested marshallable objects.
    @Test
    public void testProjectWithNestedMarshallable() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Initialize the simple object with a nested inner object and sample data
        @NotNull final Simple simple = new Simple();
        @NotNull final Inner inner = new Inner();
        inner.name("some data");
        simple.inner(inner);
        simple.name2("hello");
        simple.name2("world");

        // Project the data from the simple object to an outer object
        final Outer project = Wires.project(Outer.class, simple);
        Assertions.assertEquals("some data", project.inner().name());
    }

    // Data Transfer Object 1 - holds sample data for projection tests
    @SuppressWarnings("rawtypes")
    static class Dto1 extends SelfDescribingMarshallable {
        @NotNull
        final
        Map m = new HashMap<>();
        String anotherField;
        long someValue;
    }

    // Data Transfer Object 2 - target object for the projection tests
    @SuppressWarnings("rawtypes")
    static class Dto2 extends SelfDescribingMarshallable {
        long someValue;
        String anotherField;
        @NotNull
        final
        Map m = new HashMap<>();
    }

    // Inner class representing a nested marshallable object
    public static class Inner extends SelfDescribingMarshallable {
        private String name;

        String name() {
            return name;
        }

        @NotNull Inner name(String name) {
            this.name = name;
            return this;
        }
    }

    // Outer class which can potentially contain an instance of the Inner class
    public static class Outer extends SelfDescribingMarshallable {
        private Inner inner;

        Inner inner() {
            return inner;
        }

        @NotNull Outer inner(Inner inner) {
            this.inner = inner;
            return this;
        }
    }

    // Simple class extending the Outer class to demonstrate nested projections
    public static class Simple extends Outer {
        private String name2;

        public String name2() {
            return name2;
        }

        @NotNull Simple name2(String name2) {
            this.name2 = name2;
            return this;
        }
    }

    @BeforeEach
    public void setup(TestInfo testInfo) {
        Optional<Method> testMethod = testInfo.getTestMethod();
        if (testMethod.isPresent()) {
            this.name = testMethod.get().getName();
        }
    }
}
