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
import net.openhft.chronicle.core.onoes.ExceptionHandler;
import net.openhft.chronicle.core.util.ThrowingFunction;
import net.openhft.chronicle.wire.TextMethodTester;
import net.openhft.chronicle.wire.YamlMethodTester;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The YamlTesterParametersBuilder class facilitates the configuration of parameters for YAML-based testing.
 * This class leverages the builder pattern, enabling a fluent and intuitive setup of testing parameters.
 * Each method within this class is designed to either set a specific configuration or retrieve a particular value,
 * enhancing clarity and simplifying the process of parameter setup.
 */
@Deprecated(/* to be moved in x.27 */)
public class YamlTesterParametersBuilder<T> {

    // A function responsible for constructing the test component
    private final ThrowingFunction<T, Object, Throwable> builder;

    // Specifies the expected output type for the test
    private final Class<T> outClass;

    // List of paths indicating where the YAML files are located
    private final List<String> paths;

    // Additional output classes provided for advanced testing scenarios
    private final Set<Class> additionalOutputClasses = new LinkedHashSet<>();

    // Array of agitators used to modify or adjust the test parameters
    private YamlAgitator[] agitators = {};

    // A function that provides a customized exception handling mechanism
    private Function<T, ExceptionHandler> exceptionHandlerFunction;

    // Flag to determine whether the exception handler function should log the exception or not
    private boolean exceptionHandlerFunctionAndLog;

    // Predicate to filter which tests to execute
    private Predicate<String> testFilter = new ContainsDifferentMessageFilter();

    // A function to process and possibly modify the test input
    private Function<String, String> inputFunction;

    /**
     * Constructor that initializes the builder with a given component builder, output class, and paths specified as a comma-separated string.
     *
     * @param builder    A function responsible for constructing the test component.
     * @param outClass   Expected output type for the test.
     * @param paths      Comma-separated string indicating locations of YAML files.
     */
    public YamlTesterParametersBuilder(ThrowingFunction<T, Object, Throwable> builder, Class<T> outClass, String paths) {
        this(builder, outClass, Arrays.asList(paths.split(" *, *")));
    }

    /**
     * Constructor that initializes the builder with a given component builder, output class, and list of paths.
     *
     * @param builder    A function responsible for constructing the test component.
     * @param outClass   Expected output type for the test.
     * @param paths      List indicating locations of YAML files.
     */
    public YamlTesterParametersBuilder(ThrowingFunction<T, Object, Throwable> builder, Class<T> outClass, List<String> paths) {
        this.builder = builder;
        this.outClass = outClass;
        this.paths = paths;
    }

    /**
     * Sets the agitators used for modifying test parameters.
     * This method follows the builder pattern and returns the current instance.
     *
     * @param agitators  Array of YamlAgitator objects.
     * @return The current instance of YamlTesterParametersBuilder.
     */
    public YamlTesterParametersBuilder<T> agitators(YamlAgitator... agitators) {
        this.agitators = agitators;
        return this;
    }

    /**
     * Specifies a custom exception handler function for the test.
     * This method follows the builder pattern and returns the current instance.
     *
     * @param exceptionHandlerFunction   A function providing custom exception handling.
     * @return The current instance of YamlTesterParametersBuilder.
     */
    public YamlTesterParametersBuilder<T> exceptionHandlerFunction(Function<T, ExceptionHandler> exceptionHandlerFunction) {
        this.exceptionHandlerFunction = exceptionHandlerFunction;
        return this;
    }

    /**
     * Constructs and returns a list of test parameters based on YAML configurations.
     * This method reads YAML configurations from specified paths, processes them based on various configurations, and
     * returns them as test parameters. It also takes into account any defined agitators and combinations.
     *
     * @return A list of test parameters with each entry containing a path and its associated YamlTester.
     */
    public List<Object[]> get() {
        // Convert the builder into a function that returns an object
        Function<T, Object> compFunction = ThrowingFunction.asFunction(builder);

        // List to hold the test parameters
        List<Object[]> params = new ArrayList<>();

        // Local copy of the test filter
        Predicate<String> testFilter = this.testFilter;

        // Map to store the test configurations
        Map<String, YamlTester> testers = new LinkedHashMap<>();

        // Process each path, constructing and storing the YamlTester configurations
        for (String path : paths) {
            path = path.trim(); // trim without a regex
            if (path.isEmpty())
                continue;

            // Define the setup YAML path
            String setup = path + "/_setup.yaml";

            // Construct the YamlTester for this path
            YamlTester yt =
                    new YamlMethodTester<>(path + "/in.yaml", compFunction, outClass, path + "/out.yaml")
                            .genericEvent("event")
                            .setup(setup)
                            .exceptionHandlerFunction(exceptionHandlerFunction)
                            .exceptionHandlerFunctionAndLog(exceptionHandlerFunctionAndLog)
                            .inputFunction(inputFunction)
                            .testFilter(s -> {
                                // include it
                                testFilter.test(s);
                                // always add it
                                return true;
                            });
            testers.put(path, yt);
            addOutputClasses(yt);
            Object[] test = {path, yt};
            params.add(test);
        }

        // If only base tests are to be run, return early
        if (YamlTester.BASE_TESTS)
            return params;

        // Hold paths that are being skipped due to file not found
        SortedSet<String> skipping = new TreeSet<>();

        // Process agitated tests
        for (Map.Entry<String, YamlTester> pyt : testers.entrySet()) {
            String path = pyt.getKey();
            YamlTester yt = pyt.getValue();
            String setup = path + "/_setup.yaml";

            // add agitated tests
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
                        if (!YamlTester.REGRESS_TESTS)
                            IOTools.urlFor(builder.getClass(), output);
                        YamlTester yta = new YamlMethodTester<>(entry.getKey(), compFunction, outClass, output)
                                .genericEvent("event")
                                .setup(setup)
                                .exceptionHandlerFunction(exceptionHandlerFunction)
                                .exceptionHandlerFunctionAndLog(exceptionHandlerFunctionAndLog)
                                .inputFunction(inputFunction)
                                .testFilter(testFilter());
                        addOutputClasses(yta);

                        Object[] testa = {path + "/" + name, yta};
                        params.add(testa);
                    } catch (FileNotFoundException ioe) {
                        skipping.add(path + "/" + name);
                    }
                }
            }
        }

        // Combine tests
        for (Map.Entry<String, YamlTester> pyt : testers.entrySet()) {
            String path = pyt.getKey();

            String in_yaml;
            try {
                in_yaml = new String(IOTools.readFile(outClass, path + "/in.yaml"), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }

            String _setup_yaml = "";
            try {
                _setup_yaml = new String(IOTools.readFile(outClass, path + "/_setup.yaml"), StandardCharsets.UTF_8);
            } catch (IOException e) {
                // ignored
            }

            // add combination tests
            for (String path2 : paths) {
                path2 = path2.trim(); // trim without a regex
                if (path2.isEmpty())
                    continue;
                String name = path2.replaceAll("[:/\\\\]+", "_");
                String output = path + "/out-" + name + ".yaml";

                try {
                    if (!YamlTester.REGRESS_TESTS)
                        IOTools.urlFor(builder.getClass(), output);

                    String in_yaml2;
                    try {
                        in_yaml2 = new String(IOTools.readFile(outClass, path2 + "/in.yaml"), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new IORuntimeException(e);
                    }

                    String _setup_yaml2 = "";
                    try {
                        _setup_yaml2 = new String(IOTools.readFile(outClass, path2 + "/_setup.yaml"), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        // ignored
                    }

                    String in2 = "=\n" + in_yaml + "\n...\n" + in_yaml2;
                    String setup2 = "=\n" + _setup_yaml + "\n...\n" + _setup_yaml2;
                    YamlTester yt2 =
                            new YamlMethodTester<>(in2, compFunction, outClass, output)
                                    .genericEvent("event")
                                    .setup(setup2)
                                    .exceptionHandlerFunction(exceptionHandlerFunction)
                                    .exceptionHandlerFunctionAndLog(exceptionHandlerFunctionAndLog)
                                    .inputFunction(inputFunction)
                                    .testFilter(testFilter());
                    addOutputClasses(yt2);
                    Object[] test2 = {path + "+" + path2, yt2};
                    params.add(test2);
                } catch (FileNotFoundException ioe) {
                    skipping.add(path + "/" + path + "+" + path2);
                }
            }

            // Log paths that have been skipped, if any
            if (!skipping.isEmpty())
                Jvm.debug().on(YamlTester.class, "Skipping " + skipping);
        }
        return params;
    }

    /**
     * Adds all additional output classes to the provided YamlTester.
     *
     * @param yta The YamlTester to which the output classes are to be added.
     */
    private void addOutputClasses(YamlTester yta) {
        additionalOutputClasses.forEach(((TextMethodTester<?>) yta)::addOutputClass);
    }

    /**
     * Adds a class to the set of additional output classes.
     *
     * @param outputClass The class to be added.
     * @return The current instance of YamlTesterParametersBuilder.
     */
    public YamlTesterParametersBuilder<T> addOutputClass(Class outputClass) {
        additionalOutputClasses.add(outputClass);
        return this;
    }

    /**
     * Returns the state of the exceptionHandlerFunctionAndLog flag.
     *
     * @return true if the exception handler function and log are enabled, false otherwise.
     */
    public boolean exceptionHandlerFunctionAndLog() {
        return exceptionHandlerFunctionAndLog;
    }

    /**
     * Sets the state of the exceptionHandlerFunctionAndLog flag.
     *
     * @param exceptionHandlerFunctionAndLog The new state of the flag.
     * @return The current instance of YamlTesterParametersBuilder.
     */
    public YamlTesterParametersBuilder<T> exceptionHandlerFunctionAndLog(boolean exceptionHandlerFunctionAndLog) {
        this.exceptionHandlerFunctionAndLog = exceptionHandlerFunctionAndLog;
        return this;
    }

    /**
     * Returns the current test filter predicate.
     *
     * @return The current test filter predicate.
     */
    public Predicate<String> testFilter() {
        return testFilter;
    }

    /**
     * Sets a new test filter predicate.
     *
     * @param testFilter The new test filter predicate.
     * @return The current instance of YamlTesterParametersBuilder.
     */
    public YamlTesterParametersBuilder<T> testFilter(Predicate<String> testFilter) {
        this.testFilter = testFilter;
        return this;
    }

    /**
     * Sets the input function that transforms input strings.
     *
     * @param inputFunction The function to set.
     * @return The current instance of YamlTesterParametersBuilder.
     */
    public YamlTesterParametersBuilder<T> inputFunction(Function<String, String> inputFunction) {
        this.inputFunction = inputFunction;
        return this;
    }

    /**
     * The ContainsDifferentMessageFilter class is a Predicate implementation that tests if a given string
     * contains messages that haven't been seen before. Messages are split by "...\\n".
     */
    static class ContainsDifferentMessageFilter implements Predicate<String> {
        // Set to hold unique messages
        final Set<String> msgs = new HashSet<>();

        @Override
        public boolean test(String s) {
            boolean added = false;
            for (String msg : s.split("...\\n"))
                added |= msgs.add(msg);
            return added;
        }
    }
}
