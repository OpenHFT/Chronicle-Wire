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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.VanillaMethodWriterBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class MethodWriterProxyTest extends MethodWriterTest {
    @Before
    public void before() {
        System.setProperty(VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN, "true");
        System.setProperty(VanillaMethodWriterBuilder.DISABLE_PROXY_REFLECTION, "false");
        expectException("Falling back to proxy method writer");
    }

    @After
    public void after() {
        System.clearProperty("disableProxyCodegen");
    }

    @Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/159")
    @Test
    public void multiOut() {
        super.multiOut();
    }

    @Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/159")
    @Test
    public void testPrimitives() {
        assumeFalse(Jvm.isMacArm());
        super.doTestPrimitives(true);
    }

    @Override
    protected void checkWriterType(Object writer) {
        assumeFalse(Jvm.isMacArm());
        assertTrue(Proxy.isProxyClass(writer.getClass()));
    }
}

