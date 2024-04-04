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

package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.TextMethodTester;
import net.openhft.chronicle.wire.WireOut;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Function;

/**
 * The YamlTester interface provides utilities to perform testing on components using YAML configurations.
 * It offers methods to run tests on given implementations using YAML files, compare them, and report any discrepancies.
 * Two system properties can be set to influence the behavior of the tests:
 *
 * <ul>
 *     <li><b>regress.tests</b>: If set to true, it overwrites the output YAML file instead of checking it.
 *         Though the test will still fail on an exception. This is useful to verify the reasonableness
 *         of the output changes while committing.</li>
 *     <li><b>base.tests</b>: If set to true, only the base tests are executed, skipping the generated tests.</li>
 * </ul>
 */
public interface YamlTester {
    /**
     * System property to determine whether to overwrite the output YAML file.
     */
    boolean REGRESS_TESTS = Jvm.getBoolean("regress.tests");
    /**
     * System property to determine whether to run only base tests.
     */
    boolean BASE_TESTS = Jvm.getBoolean("base.tests");

    /**
     * Executes tests for a given implementation class using the specified YAML files. The method
     * searches for a constructor in the implementation class that accepts a single interface argument
     * and uses it to create an instance of the component.
     *
     * @param implClass Class of the implementation to be tested.
     * @param path      Directory path where the YAML files (in.yaml, out.yaml, and setup.yaml) are located.
     * @return YamlTester instance containing the test results for comparison.
     * @throws AssertionError If the test encounters issues or if the constructor doesn't match the expected format.
     */
    @SuppressWarnings("unchecked")
    static YamlTester runTest(Class<?> implClass, String path) throws AssertionError {
        for (Constructor<?> cons : implClass.getDeclaredConstructors()) {
            if (cons.getParameterCount() == 1) {
                final Class<?>[] parameterTypes = cons.getParameterTypes();
                if (parameterTypes[0].isInterface()) {
                    return runTest((Object out) -> {
                        try {
                            return cons.newInstance(out);
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    }, (Class<Object>) parameterTypes[0], path);
                }
            }
        }
        throw new IllegalArgumentException("Unable to find a constructor with one interface as an argument");
    }

    /**
     * Executes tests for a component built using the provided builder function. This method allows
     * for more flexibility in constructing the component to be tested.
     *
     * @param builder  Function that builds the component to be tested.
     * @param outClass Interface that represents the expected output type.
     * @param path     Directory path where the YAML files (in.yaml, out.yaml, and setup.yaml) are located.
     * @param <T>      Type parameter representing the type of the input to the builder function.
     * @return YamlTester instance containing the test results for comparison.
     * @throws AssertionError If the test encounters issues.
     */
    static <T> YamlTester runTest(Function<T, Object> builder, Class<T> outClass, String path) throws AssertionError {
        try {
            return new TextMethodTester<>(
                    path + "/in.yaml",
                    builder,
                    outClass,
                    path + "/out.yaml")
                    .setup(path + "/setup.yaml")
                    .run();
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }
    }

    /**
     * Executes tests on a component constructed by the given builder function and outputs the result using
     * the provided outFunction. The input and expected results are sourced from `in.yaml` and `out.yaml` files,
     * respectively. An optional `setup.yaml` file can also be provided to initialize the testing environment.
     *
     * @param builder     Function to create the component under test.
     * @param outFunction Interface function that represents the expected output type.
     * @param path        Directory path containing the YAML files (in.yaml, out.yaml, and optionally setup.yaml).
     * @param <T>         Type parameter indicating the type accepted by the builder function.
     * @return YamlTester instance containing the test results for comparison.
     * @throws AssertionError if any discrepancy is found or if an exception occurs during test execution.
     */
    static <T> YamlTester runTest(Function<T, Object> builder, Function<WireOut, T> outFunction, String path) throws AssertionError {
        try {
            return new TextMethodTester<>(
                    path + "/in.yaml",
                    builder,
                    outFunction,
                    path + "/out.yaml")
                    .setup(path + "/setup.yaml")
                    .run();
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }
    }

    /**
     * Generates additional tests based on the current test configuration by using a specified YamlAgitator.
     * These agitated tests can be used to validate component behavior under specific, possibly edge-case, scenarios.
     *
     * @param agitator YamlAgitator instance used to create additional test cases.
     * @return A map where each key represents an agitated test input and the corresponding value is its test name.
     * @throws IORuntimeException if an I/O error occurs during test generation.
     */
    Map<String, String> agitate(YamlAgitator agitator);

    /**
     * Retrieves the expected result string after test execution.
     *
     * @return Expected result string.
     */
    String expected();

    /**
     * Retrieves the actual result string obtained after test execution.
     *
     * @return Actual result string.
     */
    String actual();
}
