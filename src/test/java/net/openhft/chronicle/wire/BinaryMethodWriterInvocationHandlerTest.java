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
import static org.junit.Assert.assertNotNull;

public class BinaryMethodWriterInvocationHandlerTest extends WireTestCommon {

    @Test
    public void testOnClose() {
        Closeable closeable = createMock(Closeable.class);
        closeable.close();
        replay(closeable);

        MarshallableOut out = createMock(MarshallableOut.class);
        expect(out.recordHistory()).andReturn(true);
        replay(out);

        @NotNull BinaryMethodWriterInvocationHandler handler = new BinaryMethodWriterInvocationHandler(Closeable.class, false, out);
        handler.onClose(closeable);

        Class<?>[] interfaces = {Closeable.class};
        try (@NotNull Closeable close = (Closeable) Proxy.newProxyInstance(Closeable.class.getClassLoader(), interfaces, handler)) {
            assertNotNull(close);
            // and close it
        }

        verify(closeable);
    }
}
