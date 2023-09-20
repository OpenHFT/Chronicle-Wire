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


// A generic interface which allows for method chaining and a terminator method
interface MyInterface<I extends MyInterface> {
    I hello(String hello);

    void terminator();
}

// Test class to validate the behavior of generic methods in the context of Wire operations
public class GenericMethodsTest extends WireTestCommon {

    // Test for chained method calls with TextWire
    @Test
    public void chainedText() {

        // Create a new TextWire instance with elastic byte allocation
        TextWire wire = new TextWire(Bytes.allocateElasticOnHeap(128))
                .useTextDocuments();

        // Generate a method writer for the MyInterface
        MyInterface top = wire.methodWriter(MyInterface.class);

        // Ensure the created object is not a proxy class
        assertFalse(Proxy.isProxyClass(top.getClass()));

        // Chain multiple method calls and terminate
        top.hello("hello world").hello("hello world 2").terminator();

        // Assert the expected output from the wire after the method calls
        Assert.assertEquals("hello: hello world\n" +
                "hello: hello world 2\n" +
                "terminator: \"\"\n" +
                "...\n", wire.toString());
    }
}
