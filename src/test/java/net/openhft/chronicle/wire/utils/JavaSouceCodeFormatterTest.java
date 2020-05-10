package net.openhft.chronicle.wire.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaSouceCodeFormatterTest {

    @Test
    public void testAppend() throws IOException {
        JavaSouceCodeFormatter codeFormatter = new JavaSouceCodeFormatter(new AtomicInteger());
        Assert.assertEquals("public Appendable append(final CharSequence csq) {\n" +
                "    return sb.append(replaceNewLine(csq, 0, csq.length() - 1));\n" +
                "}", codeFormatter.append("public Appendable append(final CharSequence csq) {\n" +
                "return sb.append(replaceNewLine(csq, 0, csq.length() - 1));\n" +
                "}").toString());
    }
}