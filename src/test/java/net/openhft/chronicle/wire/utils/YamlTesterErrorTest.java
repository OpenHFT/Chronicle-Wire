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

import net.openhft.chronicle.core.StackTrace;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(Parameterized.class)
public class YamlTesterErrorTest extends WireTestCommon {
    static final String paths = "" +
            "yaml-tester/errors";

    final String name;
    final YamlTester tester;

    public YamlTesterErrorTest(String name, YamlTester tester) {
        this.name = name;
        this.tester = tester;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() {
        return new YamlTesterParametersBuilder<>(ErrorsImpl::new, ErrorsOut.class, paths)
                .exceptionHandlerFunction(out -> (log, msg, thrown) -> ((ErrorListener) out).jvmError(thrown == null ? msg : (msg + " " + thrown)))
                .exceptionHandlerFunctionAndLog(true)
                .addOutputClass(ErrorListener.class)
                .get();
    }

    @After
    public void tearDown() {
        SystemTimeProvider.CLOCK = SystemTimeProvider.INSTANCE;
    }

    @Test
    public void runTester() {
        ignoreException(" to the classpath");
        expectException("Unknown method-name='unknownMethod' ");
        expectException((ExceptionKey ek) -> ek.throwable instanceof StackTrace, "StackTrace");
        expectException((ExceptionKey ek) -> ek.throwable instanceof ErrorsImpl.MyAssertionError, "MyAssertionError");

        expectException("warning one");
        expectException("error one");
        expectException("exception one");
        expectException("warnings done");
        assertEquals(tester.expected(), tester.actual());
    }
}
