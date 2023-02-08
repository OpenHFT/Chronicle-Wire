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
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.util.ThrowingFunction;
import net.openhft.chronicle.wire.TextMethodTester;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.YamlMethodTester;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
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

    static <T> List<Object[]> parameters(ThrowingFunction<T, Object, Throwable> builder, Class<T> outClass, String paths, YamlAgitator... agitators) {
        Function<T, Object> compFunction = ThrowingFunction.asFunction(builder);
        List<Object[]> params = new ArrayList<>();
        String[] pathArr = paths.split(",");
        for (String path : pathArr) {
            path = path.trim(); // trim without a regex
            if (path.isEmpty())
                continue;
            String setup = path + "/_setup.yaml";
            YamlTester yt =
                    new YamlMethodTester<>(path + "/in.yaml", compFunction, outClass, path + "/out.yaml")
                            .genericEvent("event")
                            .setup(setup);
            Object[] test = {path, yt};
            params.add(test);

            final boolean REGRESS_TESTS = Jvm.getBoolean("regress.tests");

            SortedSet<String> skipping = new TreeSet<>();
            if (agitators.length > 0) {
                Map<String, String> inputToNameMap = new LinkedHashMap<>();
                for (YamlAgitator agitator : agitators) {
                    Map<String, String> agitateMap = yt.agitate(agitator);
                    for (Map.Entry<String, String> entry : agitateMap.entrySet()) {
                        inputToNameMap.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                }
                for (Map.Entry<String, String> entry : inputToNameMap.entrySet()) {
                    String name = entry.getValue();
                    String output = path + "/out-" + name + ".yaml";
                    try {
                        if (!REGRESS_TESTS)
                            IOTools.urlFor(builder.getClass(), output);
                        YamlTester yta = new YamlMethodTester<>(entry.getKey(), compFunction, outClass, output)
                                .genericEvent("event")
                                .setup(setup);

                        Object[] testa = {path + "/" + name, yta};
                        params.add(testa);
                    } catch (FileNotFoundException ioe) {
                        skipping.add(path + "/" + name);
                    }
                }
            }
            if (!skipping.isEmpty())
                Jvm.debug().on(YamlTester.class, "Skipping " + skipping);
        }
        return params;
    }

    /**
     * Using this test as a template, generate more tests using this YamlAgitator
     *
     * @param agitator to use
     * @return a map of gerenated inputs to test names
     * @throws IORuntimeException if an IO error occurs
     */
    default Map<String, String> agitate(YamlAgitator agitator) throws IORuntimeException {
        @Deprecated(/* make non-default in x.25 */)
        int dummy;
        throw new UnsupportedOperationException();
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
