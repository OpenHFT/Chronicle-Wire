/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.easymock.EasyMock.*;

public class BinaryMethodWriterInvocationHandlerTest extends WireTestCommon {

    @Test
    public void testOnClose() throws Exception {
        // Creating a mock of the Closeable interface using some mock library (presumably EasyMock).
        Closeable closeable = createMock(Closeable.class);

        // Setting expectations on the mock: When the close() method is called, do nothing.
        closeable.close();
        // Puts the mock into replay mode, which means it's ready to be used and its behavior is now "fixed".
        replay(closeable);

        // Creating a mock of the MarshallableOut interface.
        MarshallableOut out = createMock(MarshallableOut.class);
        // Setting expectations: When the recordHistory() method is called on this mock, return true.
        expect(out.recordHistory()).andReturn(true);
        // Puts this mock into replay mode too.
        replay(out);

        // Creating an instance of BinaryMethodWriterInvocationHandler with the Closeable.class, a false flag and the mocked MarshallableOut.
        @NotNull BinaryMethodWriterInvocationHandler handler = new BinaryMethodWriterInvocationHandler(Closeable.class, false, out);

        // Calls onClose on the handler passing the mocked closeable. This may have been added for setup or verification purposes.
        handler.onClose(closeable);

        // Uses Java's Proxy class to dynamically create a new object that implements Closeable. This object uses the handler for its method invocations.
        try (@NotNull Closeable close = (Closeable) Proxy.newProxyInstance(Closeable.class.getClassLoader(), new Class[]{Closeable.class}, handler)) {
            // The try-with-resources block ensures that when we exit this block, the close() method is automatically called on the proxy.
            // This is probably done to trigger some behavior in the handler, and possibly to satisfy the expectation set on the mock.
        }

        // Verify that the methods called on the mock match the expectations that were set.
        verify(closeable);
    }
}
