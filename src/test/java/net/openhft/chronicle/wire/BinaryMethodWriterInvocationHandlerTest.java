/*
 * Copyright 2016 higherfrequencytrading.com
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

/*
 * Created by Peter Lawrey on 23/04/16.
 */
public class BinaryMethodWriterInvocationHandlerTest {

    @Test
    public void testOnClose() throws Exception {
        Closeable closeable = createMock(Closeable.class);
        closeable.close();
        replay(closeable);

        MarshallableOut out = createMock(MarshallableOut.class);
        expect(out.recordHistory()).andReturn(true);
        replay(out);

        @NotNull BinaryMethodWriterInvocationHandler handler = new BinaryMethodWriterInvocationHandler(false, out);
        handler.onClose(closeable);

        try (@NotNull Closeable close = (Closeable) Proxy.newProxyInstance(Closeable.class.getClassLoader(), new Class[]{Closeable.class}, handler)) {
            // and close it
        }

        verify(closeable);
    }
}