package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Rob Austin.
 */
@RunWith(value = Parameterized.class)
public class StrangeTextCombination {
    private WireType wireType;
    private Bytes bytes;

    public StrangeTextCombination(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(
                new Object[]{WireType.TEXT}
                , new Object[]{WireType.BINARY}
                , new Object[]{WireType.RAW}
        );
    }

    @Test
    public void testPrependedSpace() throws Exception {
        final String prependedSpace = " hello world";
        final Wire wire = wireFactory();
        wire.write().text(prependedSpace);

        Assert.assertEquals(prependedSpace, wire.read().text());

    }

    @Test
    public void testPostpendedSpace() throws Exception {
        final String postpendedSpace = "hello world ";
        final Wire wire = wireFactory();
        wire.write().text(postpendedSpace);

        Assert.assertEquals(postpendedSpace, wire.read().text());
    }

    @Test
    public void testSlashQuoteTest() throws Exception {
        final String expected = "\\\" ";
        final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testYaml() throws Exception {
        final String expected = "!String{chars:hello world}";
        final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testString() throws Exception {
        final String expected = "!String";
        final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testBinary() throws Exception {
        final String expected = "!binary";
        final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testBinaryWithSpace() throws Exception {
        final String expected = " !binary";
        final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }


    @Test
    public void testEmpty() throws Exception {
        final String expected = "";
        final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testNull() throws Exception {
        final String expected = null;
        final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }


    @Test
    public void testNewLine() throws Exception {
        final String expected = "\n";
        final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testUnicode() throws Exception {
        final String expected = "\u0000";
        final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testXML() throws Exception {
        final String expected = "<name>rob austin</name>";
        final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @NotNull
    private Wire wireFactory() {
        bytes = Bytes.allocateElasticDirect();
        return wireType.apply(bytes);
    }

}
