package net.openhft.chronicle.wire;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.OS;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MarshallableOutBuilderTest {
    @Test
    public void fileAppend() throws IOException {
        final String expected = "" +
                "mid: mid\n" +
                "next: 1\n" +
                "echo: echo-1\n" +
                "...\n" +
                "mid2: mid2\n" +
                "next2: word\n" +
                "echo: echo-2\n" +
                "...\n";
        file("?append=true", expected);
    }

    @Test
    public void file() throws IOException {
        final String expected = "" +
                "mid2: mid2\n" +
                "next2: word\n" +
                "echo: echo-2\n" +
                "...\n";
        file("", expected);
    }

    public void file(String query, String expected) throws IOException {
        final File file = new File(OS.getTarget(), "tmp-" + System.nanoTime());
        final URL url = new URL("file://" + file.getAbsolutePath() + query);
        writeMessages(url);
        final Bytes bytes = BytesUtil.readFile(file.getAbsolutePath());
        assertEquals(expected, bytes.toString());
    }

    private void writeMessages(URL url) {
        writeMessages(url, null);
    }

    private void writeMessages(URL url, WireType wireType) {
        final MarshallableOut out = MarshallableOut.builder(url).wireType(wireType).get();
        ITop top = out.methodWriter(ITop.class);
        top.mid("mid")
                .next(1)
                .echo("echo-1");
        top.mid2("mid2")
                .next2("word")
                .echo("echo-2");
    }

    @Test
    public void http() throws IOException, InterruptedException {
        int port = 65432;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        server.createContext("/echo", new Handler(queue));
        server.start();
        final URL url = new URL("http://localhost:" + port + "/echo");
        writeMessages(url);
        assertEquals(
                "{\"mid\":\"mid\",\"next\":1,\"echo\":\"echo-1\"}\n",
                queue.poll(1, TimeUnit.SECONDS));
        assertEquals(
                "{\"mid2\":\"mid2\",\"next2\":\"word\",\"echo\":\"echo-2\"}\n",
                queue.poll(1, TimeUnit.SECONDS));
        assertNull(queue.poll(1, TimeUnit.MILLISECONDS));
        server.stop(1);
    }

    static class Handler implements HttpHandler {
        private final BlockingQueue<String> queue;

        public Handler(BlockingQueue<String> queue) {
            this.queue = queue;
        }

        @Override
        public void handle(HttpExchange xchg) throws IOException {
            StringBuilder sb = new StringBuilder();
            char ch;
            for (InputStream is = xchg.getRequestBody(); (ch = (char) is.read()) != (char) -1; )
                sb.append(ch);
            queue.add(sb.toString());
            xchg.sendResponseHeaders(202, 0);
        }
    }
}

