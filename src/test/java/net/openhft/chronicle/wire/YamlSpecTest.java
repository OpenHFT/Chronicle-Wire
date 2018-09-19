package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;

/**
 * Created by Rob Austin
 */
public class YamlSpecTest {
    static String DIR = "/yaml/spec/";

    @Test
    public void test2_1_SequenceOfScalars() {

        Bytes b = Bytes.elasticByteBuffer();
        try {

            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_1_SequenceOfScalars.yaml");

            String actual = Marshallable.fromString(is).toString();
            Assert.assertEquals("[Mark McGwire, Sammy Sosa, Ken Griffey]", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void testMappingScalarsToScalars_2_2() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_2_MappingScalarsToScalars.yaml");

            Object o = Marshallable.fromString(is);
            String actual = o.toString();
            Assert.assertEquals("{hr=65, avg=0.278, rbi=147}", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void test2_3_MappingScalarsToSequences() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_3_MappingScalarsToSequences.yaml");

            Object o = Marshallable.fromString(is);
            String actual = o.toString();
            Assert.assertEquals("{american=[Boston Red Sox, Detroit Tigers, New York Yankees], national=[New York Mets, Chicago Cubs, Atlanta Braves]}", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_4_SequenceOfMappings() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_4_SequenceOfMappings.yaml");

            Object o = Marshallable.fromString(is);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void test2_4_SequenceOfMappingsFixed() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_4_SequenceOfMappings-fixed.yaml");

            Object o = Marshallable.fromString(is);
            String actual = o.toString();
            Assert.assertEquals("[{name=Mark McGwire, hr=65, avg=0.278}, {name=Sammy Sosa, hr=63, avg=0.288}]", actual);

        } finally {
            b.release();
        }

    }



    @Test
    public void test2_5_SequenceOfSequences() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_5_SequenceOfSequences.yaml");

            Object o = Marshallable.fromString(is);
            String actual = o.toString();
            Assert.assertEquals("[[name, hr, avg], [Mark McGwire, 65, 0.278], [Sammy Sosa, 63, 0.288]]", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void test2_6_MappingOfMappings() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_6_MappingOfMappings.yaml");

            Object o = Marshallable.fromString(is);
            String actual = o.toString();
            Assert.assertEquals("{Mark McGwire={hr=65, avg=0.278}, Sammy Sosa={hr=63, avg=0.288}}", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_7_TwoDocumentsInAStream() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_7_TwoDocumentsInAStream.yaml");

            Object o = Marshallable.fromString(is);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void test2_7_TwoDocumentsInAStreamFixed() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_7_TwoDocumentsInAStreamFixed.yaml");

            Object o = Marshallable.fromString(is);
            String actual = o.toString();
            Assert.assertEquals("[[Mark McGwire, Sammy Sosa, Ken Griffey], [Chicago Cubs, St Louis Cardinals]]", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_8_PlayByPlayFeed() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_8_PlayByPlayFeed.yaml");

            Object o = Marshallable.fromString(is);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void test2_8_PlayByPlayFeedFixed() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_8_PlayByPlayFeedFixed.yaml");

            Object o = Marshallable.fromString(is);
            String actual = o.toString();
            Assert.assertEquals("[{time=20:03:20, player=Sammy Sosa, action=strike (miss)}, {time=20:03:47, player=Sammy Sosa, action=grand slam}]", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_9_SingleDocumentWithTwoComments() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_9_SingleDocumentWithTwoComments.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void test2_9_SingleDocumentWithTwoCommentsFixed() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_9_SingleDocumentWithTwoCommentsFixed.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("{hr=[Mark McGwire, Sammy Sosa], rbi=[Sammy Sosa, Ken Griffey]}", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("TODO FIX")
    @Test
    public void test2_10_NodeAppearsTwiceInThisDocumentFixed() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_10_NodeAppearsTwiceInThisDocument.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }


    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_10_NodeAppearsTwiceInThisDocument() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_10_NodeAppearsTwiceInThisDocument.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_11MappingBetweenSequences() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_11MappingBetweenSequences.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_12CompactNestedMapping() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_12CompactNestedMapping.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void test2_12CompactNestedMappingFixed() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_12CompactNestedMappingFixed.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("[{item=Super Hoop, quantity=1}, {item=Basketball, quantity=4}, {item=Big Shoes, quantity=1}]", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_13InLiteralsNewlinesArePreserved() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_13InLiteralsNewlinesArePreserved.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_14InThefoldedScalars() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_14InThefoldedScalars.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void test2_14InThefoldedScalarsFixed() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_14InThefoldedScalarsFixed.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("[Mark McGwire's, year was crippled, by a knee injury.]", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_15FoldedNewlines() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_15FoldedNewlines.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_16IndentationDeterminesScope() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_16IndentationDeterminesScope.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void test2_16IndentationDeterminesScopeFixed() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_16IndentationDeterminesScopeFixed.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("{name=Mark McGwire, accomplishment=Mark set a major league home run record in 1998., stats=[65 Home Runs, 0.278 Batting Average]}", actual);

        } finally {
            b.release();
        }

    }


    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_17QuotedScalars() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_17QuotedScalars.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("TODO FIX")
    @Test
    public void test2_17QuotedScalarsFixed() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_17QuotedScalarsFixed.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            String expected = "{unicode: \"Sosa did fine.☺\", " +
                    "control: \"\\b1998\\t1999\\t2000\\n\", " +
                    "hex esc: \"\\x0d\\x0a is \\r\\n\", " +
                    "single: \"Howdy! he cried.\", " +
                    "quoted: \" # Not a ''comment''.\", " +
                    "tie-fighter: '|\\-*-/|'}";
            Assert.assertEquals(expected, actual);

        } finally {
            b.release();
        }

    }


    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_18Multi_lineFlowScalars() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_18Multi_lineFlowScalars.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals(" ", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void test2_18Multi_lineFlowScalarsFixed() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_18Multi_lineFlowScalarsFixed.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("{plain=\n" +
                    "  This unquoted scalar\n" +
                    "  spans many lines., quoted=So does this\n" +
                    "  quoted scalar.\n" +
                    "}", actual.replaceAll("\r", ""));

        } finally {
            b.release();
        }

    }


    @Test
    public void test2_19Integers() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_19Integers.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("{canonical=12345, decimal=12345, octal=0o14, hexadecimal=12}", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void test2_20FloatingPoint() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_20FloatingPoint.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("{canonical=1230.15, exponential=1230.15, fixed=1230.15, negative infinity=-.inf, not a number=.NaN}", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_21Miscellaneous() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_21Miscellaneous.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void test2_21MiscellaneousFixed() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_21MiscellaneousFixed.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("{null=, booleans=[true, false], string=012345}", actual);

        } finally {
            b.release();
        }

    }

    @Test
    public void test2_22Timestamps() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_22Timestamps.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("{canonical=2001-12-15T02:59:43.100Z, iso8601=2001-12-14T21:59:43.100-05:00, spaced=2001-12-14 21:59:43.10 -5, date=2002-12-14}", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_23VariousExplicitTags() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_23VariousExplicitTags.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_24GlobalTags() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_24GlobalTags.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_25UnorderedSets() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_25UnorderedSets.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_26OrderedMappings() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_26OrderedMappings.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("{Ken Griffy=58,Mark McGwire=65,Sammy Sosa=63}", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_27Invoice() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_27Invoice.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("", actual);

        } finally {
            b.release();
        }

    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_28LogFile() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_28LogFile.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();

        } finally {
            b.release();
        }

    }

}
