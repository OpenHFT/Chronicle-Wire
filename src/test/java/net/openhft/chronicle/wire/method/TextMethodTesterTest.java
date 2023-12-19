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

package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.wire.TextMethodTester;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.YamlMethodTester;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TextMethodTesterTest extends WireTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    public void run() throws IOException {
        TextMethodTester test = new TextMethodTester<>(
                "tmtt/methods-in.yaml",
                MockMethodsImpl::new,
                MockMethods.class,
                "tmtt/methods-out.yaml")
                .setup("tmtt/methods-out.yaml") // calls made here are not validated in the output.
                .run();
        compareResults(test);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void runToLower() throws IOException {
        TextMethodTester test = new TextMethodTester<>(
                "tmtt/upper-methods-in.yaml",
                MockMethodsImpl::new,
                MockMethods.class,
                "tmtt/lower-methods-out.yaml")
                .setup("tmtt/methods-out.yaml") // calls made here are not validated in the output.
                .inputFunction(String::toLowerCase)
                .run();
        compareResults(test);
    }

    @Test
    public void runTestEmptyOut() throws IOException {
        TextMethodTester test = new TextMethodTester<>(
                "tmtt/methods-in.yaml",
                NoopMockMethods::new,
                MockMethods.class,
                "tmtt/methods-out-empty.yaml")
                .setup("tmtt/methods-out.yaml") // calls made here are not validated in the output.
                .run();
        compareResults(test);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void runYaml() throws IOException {
        TextMethodTester test = new YamlMethodTester<>(
                "tmtt/methods-in.yaml",
                MockMethodsImpl::new,
                MockMethods.class,
                "tmtt/methods-out.yaml")
                .setup("tmtt/methods-out.yaml") // calls made here are not validated in the output.
                .run();
        compareResults(test);
    }

    @Test
    public void checkExceptionsProvidedToHandler() throws IOException {
        List<Exception> exceptions = new ArrayList<>();
        TextMethodTester test = new TextMethodTester<>(
                "tmtt/methods-in-exception.yaml",
                MockMethodsImpl::new,
                MockMethods.class,
                "tmtt/methods-out-empty.yaml")
                .onInvocationException(exceptions::add)
                .run();
        compareResults(test);
        assertEquals(4, exceptions.size());
    }

    private void compareResults(TextMethodTester test) {
        assertEquals(test.expected().replaceAll("\\s+#", " #"),
                test.actual().replaceAll("\\s+#", " #"));
    }
}

