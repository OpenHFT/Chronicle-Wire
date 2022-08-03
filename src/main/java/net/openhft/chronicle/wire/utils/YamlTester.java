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

import net.openhft.chronicle.wire.TextMethodTester;
import net.openhft.chronicle.wire.WireOut;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.function.Function;

public interface YamlTester {
    /**
     * Test a component implemented in a class using in.yaml comparing with out.yaml,
     * with optionally setup.yaml to initialise it.
     *
     * @param implClass of the implementation
     * @param path      where the yaml files can be found
     * @return the results for comparison
     * @throws AssertionError if anything went wrong
     */
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
                    }, (Class) parameterTypes[0], path);
                }
            }
        }
        throw new IllegalArgumentException("Unable to find a constructor with one interface as an argument");
    }

    /**
     * Test a component implemented in a class using in.yaml comparing with out.yaml,
     * with optionally setup.yaml to initialise it.
     *
     * @param builder  to construct a component to be tested
     * @param outClass the interface of output
     * @param path     where the yaml files can be found
     * @return the results for comparison
     * @throws AssertionError if anything went wrong
     */
    static <T> YamlTester runTest(Function<T, Object> builder, Class<T> outClass, String path) throws AssertionError {
        try {
            return new TextMethodTester<T>(
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
     * Test a component implemented in a class using in.yaml comparing with out.yaml,
     * with optionally setup.yaml to initialise it.
     *
     * @param builder     to construct a component to be tested
     * @param outFunction the interface of output
     * @param path        where the yaml files can be found
     * @return the results for comparison
     * @throws AssertionError if anything went wrong
     */
    static <T> YamlTester runTest(Function<T, Object> builder, Function<WireOut, T> outFunction, String path) throws AssertionError {
        try {
            return new TextMethodTester<T>(
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
     * @return the expected String
     */
    String expected();

    /**
     * @return the actual String
     */
    String actual();
}
