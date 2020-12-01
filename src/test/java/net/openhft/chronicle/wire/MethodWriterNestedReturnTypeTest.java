/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertFalse;

public class MethodWriterNestedReturnTypeTest {
    @Test
    public void testNestedReturnTypeIsSupportedInGeneratedWriter() {
        BinaryWire binaryWire = new BinaryWire(Bytes.allocateElasticOnHeap(128));

        final Start writer = binaryWire.methodWriter(Start.class);

        // Proxy method writer is constructed in case compilation of generated writer failed.
        assertFalse(Proxy.isProxyClass(writer.getClass()));
    }

    interface Start {
        End start();
    }

    interface End {
        void end();
    }
}
