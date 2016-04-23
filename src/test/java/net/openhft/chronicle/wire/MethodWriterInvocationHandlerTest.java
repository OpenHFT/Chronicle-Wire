package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.Closeable;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.easymock.EasyMock.*;

/**
 * Created by peter on 23/04/16.
 */
public class MethodWriterInvocationHandlerTest {

    @Test
    public void testOnClose() throws Exception {
        Closeable closeable = createMock(Closeable.class);
        closeable.close();
        replay(closeable);

        MarshallableOut out = createMock(MarshallableOut.class);
        expect(out.recordHistory()).andReturn(true);
        replay(out);

        MethodWriterInvocationHandler handler = new MethodWriterInvocationHandler(out);
        handler.onClose(closeable);

        try (Closeable close = (Closeable) Proxy.newProxyInstance(Closeable.class.getClassLoader(), new Class[]{Closeable.class}, handler)) {
            // and close it
        }

        verify(closeable);
    }
}