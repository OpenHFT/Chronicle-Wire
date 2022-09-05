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

package net.openhft.chronicle.wire.channel.book;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.TextMethodTester;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assume.assumeFalse;
import static org.junit.jupiter.api.Assertions.*;

public class TopOfBookHandlerTest extends WireTestCommon {
    public static void test(String basename) {
        assumeFalse(Jvm.isAzulZing());
        TextMethodTester<TopOfBookListener> tester = new TextMethodTester<>(
                basename + "/in.yaml",
                out -> new EchoTopOfBookHandler().out(out),
                TopOfBookListener.class,
                basename + "/out.yaml");
        tester.setup(basename + "/setup.yaml");
        try {
            tester.run();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        assertEquals(tester.expected(), tester.actual());
    }


    @Test
    public void testTwo() {
        test("echo-tob");
    }

}