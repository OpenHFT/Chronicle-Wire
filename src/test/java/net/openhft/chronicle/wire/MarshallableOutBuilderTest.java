package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.OS;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

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
        final MarshallableOut out = MarshallableOut.builder(url).get();
        ITop top = out.methodWriter(ITop.class);
        top.mid("mid")
                .next(1)
                .echo("echo-1");
        top.mid2("mid2")
                .next2("word")
                .echo("echo-2");
        final Bytes bytes = BytesUtil.readFile(file.getAbsolutePath());
        assertEquals(expected, bytes.toString());
    }
}

