/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BinaryMethodWriterInvocationHandlerTest extends WireTestCommon {

    @Test
    @DisplayName("Closes target when proxy is closed")
    public void testOnClose() {
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

        Class<?>[] interfaces = {Closeable.class};
        try (@NotNull Closeable close = (Closeable) Proxy.newProxyInstance(Closeable.class.getClassLoader(), interfaces, handler)) {
            assertNotNull(close, "Expected proxy closeable instance");
            // and close it
        }

        // Verify that the methods called on the mock match the expectations that were set.
        verify(closeable);
    }
}
