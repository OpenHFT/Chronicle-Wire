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
import net.openhft.chronicle.wire.YamlMethodTester;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class YamlTesterParametersBuilder<T> {
    private final ThrowingFunction<T, Object, Throwable> builder;
    private final Class<T> outClass;
    private final String paths;
    private YamlAgitator[] agitators = {};
    private Function<T, ExceptionHandler> exceptionHandlerFunction;

    public YamlTesterParametersBuilder(ThrowingFunction<T, Object, Throwable> builder, Class<T> outClass, String paths) {
        this.builder = builder;
        this.outClass = outClass;
        this.paths = paths;
    }

    public YamlTesterParametersBuilder<T> agitators(YamlAgitator[] agitators) {
        this.agitators = agitators;
        return this;
    }

    public YamlTesterParametersBuilder<T> exceptionHandlerFunction(Function<T, ExceptionHandler> exceptionHandlerFunction) {
        this.exceptionHandlerFunction = exceptionHandlerFunction;
        return this;
    }

    public List<Object[]> get() {
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
                            .setup(setup)
                            .exceptionHandlerFunction(exceptionHandlerFunction);
            Object[] test = {path, yt};
            params.add(test);

            if (YamlTester.BASE_TESTS)
                continue;

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

            SortedSet<String> skipping = new TreeSet<>();
            // add combination tests
            for (String path2 : pathArr) {
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
                                    .exceptionHandlerFunction(exceptionHandlerFunction);
                    Object[] test2 = {path + "+" + path2, yt2};
                    params.add(test2);
                } catch (FileNotFoundException ioe) {
                    skipping.add(path + "/" + path + "+" + path2);
                }
            }

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
                                .exceptionHandlerFunction(exceptionHandlerFunction);

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
}
