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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static junit.framework.TestCase.assertFalse;

interface MyInterface<I extends MyInterface> {
    I hello(String hello);

    void terminator();
}

public class GenericMethodsTest extends WireTestCommon {
    @Test
    public void chainedText() {
        TextWire wire = new TextWire(Bytes.allocateElasticOnHeap(128))
                .useTextDocuments();
        MyInterface top = wire.methodWriter(MyInterface.class);
        assertFalse(Proxy.isProxyClass(top.getClass()));

        top.hello("hello world").hello("hello world 2").terminator();

        Assert.assertEquals("hello: hello world\n" +
                "hello: hello world 2\n" +
                "terminator: \"\"\n" +
                "...\n", wire.toString());
    }
}
