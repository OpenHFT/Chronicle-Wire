package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;

@SuppressWarnings("rawtypes")
public class YamlSpecTest extends WireTestCommon {
    static String DIR = "/yaml/spec/";

    public static void doTest(String file, String expected) {
        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + file);

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals(expected, actual);

        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void test2_1_SequenceOfScalars() {
        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_1_SequenceOfScalars.yaml");

            String actual = Marshallable.fromString(is).toString();
            Assert.assertEquals("[Mark McGwire, Sammy Sosa, Ken Griffey]", actual);

        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testMappingScalarsToScalars_2_2() {
        doTest("2_2_MappingScalarsToScalars.yaml", "{hr=65, avg=0.278, rbi=147}");
    }

    @Test
    public void test2_3_MappingScalarsToSequences() {
        doTest("2_3_MappingScalarsToSequences.yaml", "{american=[Boston Red Sox, Detroit Tigers, New York Yankees], national=[New York Mets, Chicago Cubs, Atlanta Braves]}");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_4_SequenceOfMappings() {
        doTest("2_4_SequenceOfMappings.yaml", "");
    }

    @Test
    public void test2_4_SequenceOfMappingsFixed() {
        doTest("2_4_SequenceOfMappings-fixed.yaml", "[{name=Mark McGwire, hr=65, avg=0.278}, {name=Sammy Sosa, hr=63, avg=0.288}]");
    }

    @Test
    public void test2_5_SequenceOfSequences() {
        doTest("2_5_SequenceOfSequences.yaml", "[[name, hr, avg], [Mark McGwire, 65, 0.278], [Sammy Sosa, 63, 0.288]]");
    }

    @Test
    public void test2_6_MappingOfMappings() {
        doTest("2_6_MappingOfMappings.yaml", "{Mark McGwire={hr=65, avg=0.278}, Sammy Sosa={hr=63, avg=0.288}}");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_7_TwoDocumentsInAStream() {
        doTest("2_7_TwoDocumentsInAStream.yaml", "");
    }

    @Test
    public void test2_7_TwoDocumentsInAStreamFixed() {
        doTest("2_7_TwoDocumentsInAStreamFixed.yaml", "[[Mark McGwire, Sammy Sosa, Ken Griffey], [Chicago Cubs, St Louis Cardinals]]");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_8_PlayByPlayFeed() {
        doTest("2_8_PlayByPlayFeed.yaml", "");
    }

    @Test
    public void test2_8_PlayByPlayFeedFixed() {
        doTest("2_8_PlayByPlayFeedFixed.yaml", "[{time=20:03:20, player=Sammy Sosa, action=strike (miss)}, {time=20:03:47, player=Sammy Sosa, action=grand slam}]");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_9_SingleDocumentWithTwoComments() {
        doTest("2_9_SingleDocumentWithTwoComments.yaml", "");
    }

    @Test
    public void test2_9_SingleDocumentWithTwoCommentsFixed() {
        doTest("2_9_SingleDocumentWithTwoCommentsFixed.yaml", "{hr=[Mark McGwire, Sammy Sosa], rbi=[Sammy Sosa, Ken Griffey]}");
    }

    @Ignore("TODO FIX")
    @Test
    public void test2_10_NodeAppearsTwiceInThisDocumentFixed() {
        doTest("2_10_NodeAppearsTwiceInThisDocument.yaml", "");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_10_NodeAppearsTwiceInThisDocument() {
        doTest("2_10_NodeAppearsTwiceInThisDocument.yaml", "");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_11MappingBetweenSequences() {
        doTest("2_11MappingBetweenSequences.yaml", "");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_12CompactNestedMapping() {
        doTest("2_12CompactNestedMapping.yaml", "");
    }

    @Test
    public void test2_12CompactNestedMappingFixed() {
        doTest("2_12CompactNestedMappingFixed.yaml", "[{item=Super Hoop, quantity=1}, {item=Basketball, quantity=4}, {item=Big Shoes, quantity=1}]");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_13InLiteralsNewlinesArePreserved() {
        doTest("2_13InLiteralsNewlinesArePreserved.yaml", "");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_14InThefoldedScalars() {
        doTest("2_14InThefoldedScalars.yaml", "");
    }

    @Test
    public void test2_14InThefoldedScalarsFixed() {
        doTest("2_14InThefoldedScalarsFixed.yaml", "[Mark McGwire's, year was crippled, by a knee injury.]");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_15FoldedNewlines() {
        doTest("2_15FoldedNewlines.yaml", "");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_16IndentationDeterminesScope() {
        doTest("2_16IndentationDeterminesScope.yaml", "");
    }

    @Test
    public void test2_16IndentationDeterminesScopeFixed() {
        doTest("2_16IndentationDeterminesScopeFixed.yaml", "{name=Mark McGwire, accomplishment=Mark set a major league home run record in 1998., stats=[65 Home Runs, 0.278 Batting Average]}");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_17QuotedScalars() {
        doTest("2_17QuotedScalars.yaml", "");
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
            b.releaseLast();
        }
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_18Multi_lineFlowScalars() {
        doTest("2_18Multi_lineFlowScalars.yaml", " ");
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
            b.releaseLast();
        }
    }

    @Test
    public void test2_19Integers() {
        doTest("2_19Integers.yaml", "{canonical=12345, decimal=12345, octal=0o14, hexadecimal=12}");
    }

    @Test
    public void test2_20FloatingPoint() {
        doTest("2_20FloatingPoint.yaml", "{canonical=1230.15, exponential=1230.15, fixed=1230.15, negative infinity=-.inf, not a number=.NaN}");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_21Miscellaneous() {
        doTest("2_21Miscellaneous.yaml", "");
    }

    @Test
    public void test2_21MiscellaneousFixed() {
        doTest("2_21MiscellaneousFixed.yaml", "{null=, booleans=[true, false], string=012345}");
    }

    @Test
    public void test2_22Timestamps() {
        doTest("2_22Timestamps.yaml", "{canonical=2001-12-15T02:59:43.100Z, iso8601=2001-12-14T21:59:43.100-05:00, spaced=2001-12-14 21:59:43.10 -5, date=2002-12-14}");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_23VariousExplicitTags() {
        doTest("2_23VariousExplicitTags.yaml", "");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_24GlobalTags() {
        doTest("2_24GlobalTags.yaml", "");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_25UnorderedSets() {
        doTest("2_25UnorderedSets.yaml", "");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_26OrderedMappings() {
        doTest("2_26OrderedMappings.yaml", "{Ken Griffy=58,Mark McGwire=65,Sammy Sosa=63}");
    }

    @Ignore("todo see spec http://yaml.org/spec/1.2/spec.html#comment/")
    @Test
    public void test2_27Invoice() {
        doTest("2_27Invoice.yaml", "");
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
            b.releaseLast();
        }
    }

}
