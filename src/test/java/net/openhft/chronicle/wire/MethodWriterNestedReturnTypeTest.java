/*
 * Copyright 2016-2020 chronicle.software
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
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertFalse;

/**
 * This class tests the capability of MethodWriter to handle methods with nested return types.
 * It extends the WireTestCommon for common test setup and utilities.
 */
public class MethodWriterNestedReturnTypeTest extends WireTestCommon {

    /**
     * This test ensures that MethodWriter can handle methods with nested return types without resorting to proxy classes.
     */
    @Test
    public void testNestedReturnTypeIsSupportedInGeneratedWriter() {
        // Initialization of the wire
        BinaryWire binaryWire = new BinaryWire(Bytes.allocateElasticOnHeap(128));

        final Start writer = binaryWire.methodWriter(Start.class);

        // Check if the generated writer is not a proxy class
        // A proxy would indicate a fallback mechanism, suggesting the compilation of the generated writer failed.
        assertFalse(Proxy.isProxyClass(writer.getClass()));
    }

    /**
     * An interface with a method that returns another interface (nested return type).
     */
    interface Start {
        End start();
    }

    /**
     * A simple interface defining an end method.
     */
    interface End {
        void end();
    }
}
