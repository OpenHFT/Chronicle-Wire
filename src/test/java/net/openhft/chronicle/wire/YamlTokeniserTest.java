package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class YamlTokeniserTest {
    public static String doTest(String resource) {
        try {
            Bytes bytes = BytesUtil.readFile(resource);
//            bytes = Bytes.from(bytes.toString().replace("\r", ""));
            YamlTokeniser yt = new YamlTokeniser(bytes);
            StringBuilder sb = new StringBuilder();
            while (yt.next() != YamlToken.STREAM_END) {
                sb.append(yt).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void eg2_1() {
        assertEquals("DIRECTIVES_END \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Mark McGwire\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Sammy Sosa\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Ken Griffey\n" +
                        "SEQUENCE_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_1_SequenceOfScalars.yaml"));
    }

    @Test
    public void eg2_2() {
        assertEquals("DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT hr\n" +
                        "TEXT 65\n" +
                        "COMMENT Home runs\n" +
                        "MAPPING_KEY \n" +
                        "TEXT avg\n" +
                        "TEXT 0.278\n" +
                        "COMMENT Batting average\n" +
                        "MAPPING_KEY \n" +
                        "TEXT rbi\n" +
                        "TEXT 147\n" +
                        "COMMENT Runs Batted In\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_2_MappingScalarsToScalars.yaml"));
    }

    @Test
    public void eg2_3() {
        assertEquals("DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT american\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Boston Red Sox\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Detroit Tigers\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT New York Yankees\n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT national\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT New York Mets\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Chicago Cubs\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Atlanta Braves\n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_3_MappingScalarsToSequences.yaml"));
    }

    @Test
    public void eg2_4() {
        assertEquals("DIRECTIVES_END \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT name\n" +
                        "TEXT Mark McGwire\n" +
                        "MAPPING_KEY \n" +
                        "TEXT hr\n" +
                        "TEXT 65\n" +
                        "MAPPING_KEY \n" +
                        "TEXT avg\n" +
                        "TEXT 0.278\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT name\n" +
                        "TEXT Sammy Sosa\n" +
                        "MAPPING_KEY \n" +
                        "TEXT hr\n" +
                        "TEXT 63\n" +
                        "MAPPING_KEY \n" +
                        "TEXT avg\n" +
                        "TEXT 0.288\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_4_SequenceOfMappings.yaml"));
    }

    @Test
    public void eg2_4B() {
        assertEquals("DIRECTIVES_END \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT name\n" +
                        "TEXT Mark McGwire\n" +
                        "MAPPING_KEY \n" +
                        "TEXT hr\n" +
                        "TEXT 65\n" +
                        "MAPPING_KEY \n" +
                        "TEXT avg\n" +
                        "TEXT 0.278\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT name\n" +
                        "TEXT Sammy Sosa\n" +
                        "MAPPING_KEY \n" +
                        "TEXT hr\n" +
                        "TEXT 63\n" +
                        "MAPPING_KEY \n" +
                        "TEXT avg\n" +
                        "TEXT 0.288\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_4_SequenceOfMappings-fixed.yaml"));
    }

    @Ignore("TODO Handle properly")
    @Test
    public void eg2_5() {
        assertEquals("DIRECTIVES_END \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "SEQUENCE_ENTRY \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT name\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT hr\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT avg\n" +
                        "SEQUENCE_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "SEQUENCE_ENTRY \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Mark McGwire\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT 65\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT 0.278\n" +
                        "SEQUENCE_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "SEQUENCE_ENTRY \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Sammy Sosa\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT 63\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT 0.288\n" +
                        "SEQUENCE_END \n" +
                        "SEQUENCE_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_5_SequenceOfSequences.yaml"));
    }

    @Test
    public void eg2_6() {
        assertEquals("DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT Mark McGwire\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT hr\n" +
                        "TEXT 65\n" +
                        "MAPPING_KEY \n" +
                        "TEXT avg\n" +
                        "TEXT 0.278\n" +
                        "MAPPING_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT Sammy Sosa\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT hr\n" +
                        "TEXT 63\n" +
                        "MAPPING_KEY \n" +
                        "TEXT avg\n" +
                        "TEXT 0.288\n" +
                        "MAPPING_END \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_6_MappingOfMappings.yaml"));
    }

    @Test
    public void eg2_7() {
        assertEquals("COMMENT Ranking of 1998 home runs\n" +
                        "DIRECTIVES_END \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Mark McGwire\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Sammy Sosa\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Ken Griffey\n" +
                        "COMMENT Team ranking\n" +
                        "SEQUENCE_END \n" +
                        "DOCUMENT_END \n" +
                        "DIRECTIVES_END \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Chicago Cubs\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT St Louis Cardinals\n" +
                        "SEQUENCE_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_7_TwoDocumentsInAStream.yaml"));
    }

    @Test
    public void eg2_8() {
        assertEquals("DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT time\n" +
                        "TEXT 20:03:20\n" +
                        "MAPPING_KEY \n" +
                        "TEXT player\n" +
                        "TEXT Sammy Sosa\n" +
                        "MAPPING_KEY \n" +
                        "TEXT action\n" +
                        "TEXT strike (miss)\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n" +
                        "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT time\n" +
                        "TEXT 20:03:47\n" +
                        "MAPPING_KEY \n" +
                        "TEXT player\n" +
                        "TEXT Sammy Sosa\n" +
                        "MAPPING_KEY \n" +
                        "TEXT action\n" +
                        "TEXT grand slam\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_8_PlayByPlayFeed.yaml"));
    }

    @Test
    public void eg2_9() {
        assertEquals("DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT hr\n" +
                        "COMMENT 1998 hr ranking\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Mark McGwire\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Sammy Sosa\n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT rbi\n" +
                        "COMMENT 1998 rbi ranking\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Sammy Sosa\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Ken Griffey\n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_9_SingleDocumentWithTwoComments.yaml"));
    }

    @Test
    public void eg2_10() {
        assertEquals("DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT hr\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Mark McGwire\n" +
                        "COMMENT Following node labeled SS\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT &SS Sammy Sosa\n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT rbi\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT *SS\n" +
                        "COMMENT Subsequent occurrence\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Ken Griffey\n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_10_NodeAppearsTwiceInThisDocument.yaml"));
    }

    @Test
    public void eg2_11() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Detroit Tigers\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Chicago cubs\n" +
                        "SEQUENCE_END \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT 2001-07-23\n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_KEY \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT New York Yankees\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Atlanta Braves\n" +
                        "SEQUENCE_END \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT 2001-07-02\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT 2001-08-12\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT 2001-08-14\n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_11MappingBetweenSequences.yaml"));
    }

    @Test
    public void eg2_12() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "COMMENT Products purchased\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT item\n" +
                        "TEXT Super Hoop\n" +
                        "MAPPING_KEY \n" +
                        "TEXT quantity\n" +
                        "TEXT 1\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT item\n" +
                        "TEXT Basketball\n" +
                        "MAPPING_KEY \n" +
                        "TEXT quantity\n" +
                        "TEXT 4\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT item\n" +
                        "TEXT Big Shoes\n" +
                        "MAPPING_KEY \n" +
                        "TEXT quantity\n" +
                        "TEXT 1\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_12CompactNestedMapping.yaml"));
    }

    @Test
    public void eg2_13() {
        assertEquals(
                "COMMENT ASCII Art\n" +
                        "DIRECTIVES_END \n" +
                        "TEXT \\//||\\/||\n" +
                        "// ||  ||__\n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_13InLiteralsNewlinesArePreserved.yaml").replace("\r", ""));
    }

    @Test
    public void eg2_14() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "TEXT Mark McGwire's year was crippled by a knee injury.\n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_14InThefoldedScalars.yaml").replace("\r", ""));
    }

    @Test
    public void eg2_15() {
        assertEquals(
                "TEXT Sammy Sosa completed another fine season with great stats.   63 Home Runs   0.288 Batting Average What a year!\n",
                doTest("yaml/spec/2_15FoldedNewlines.yaml").replace("\r", ""));
    }

    @Test
    public void eg2_16() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT name\n" +
                        "TEXT Mark McGwire\n" +
                        "MAPPING_KEY \n" +
                        "TEXT accomplishment\n" +
                        "TEXT Mark set a major league home run record in 1998. \n" +
                        "MAPPING_KEY \n" +
                        "TEXT stats\n" +
                        "TEXT 65 Home Runs\n" +
                        "0.278 Batting Average\n" +
                        "\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_16IndentationDeterminesScope.yaml").replace("\r", ""));
    }

    @Test
    public void eg2_17() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT unicode\n" +
                        "TEXT \" Sosa did fine.\\u263A\n" +
                        "MAPPING_KEY \n" +
                        "TEXT control\n" +
                        "TEXT \" \\b1998\\t1999\\t2000\\n\n" +
                        "MAPPING_KEY \n" +
                        "TEXT hex esc\n" +
                        "TEXT \" \\x0d\\x0a is \\r\\n\n" +
                        "MAPPING_KEY \n" +
                        "TEXT single\n" +
                        "TEXT ' \"Howdy!\" he cried.\n" +
                        "MAPPING_KEY \n" +
                        "TEXT quoted\n" +
                        "TEXT '  # Not a ''comment''.\n" +
                        "MAPPING_KEY \n" +
                        "TEXT tie-fighter\n" +
                        "TEXT ' |\\-*-/|\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_17QuotedScalars.yaml"));
    }

    @Ignore("TODO FIX")
    @Test
    public void eg2_18() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT plain\\nThis unquoted scalar\\nspans many lines.\n" +
                        "MAPPING_KEY \n" +
                        "TEXT quoted\n" +
                        "TEXT So does this\n" +
                        "  quoted scalar.\\n\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_18Multi_lineFlowScalars.yaml"));
    }

    @Test
    public void eg2_19() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT canonical\n" +
                        "TEXT 12345\n" +
                        "MAPPING_KEY \n" +
                        "TEXT decimal\n" +
                        "TEXT +12345\n" +
                        "MAPPING_KEY \n" +
                        "TEXT octal\n" +
                        "TEXT 0o14\n" +
                        "MAPPING_KEY \n" +
                        "TEXT hexadecimal\n" +
                        "TEXT 0xC\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_19Integers.yaml"));
    }

    @Test
    public void eg2_20() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT canonical\n" +
                        "TEXT 1.23015e+3\n" +
                        "MAPPING_KEY \n" +
                        "TEXT exponential\n" +
                        "TEXT 12.3015e+02\n" +
                        "MAPPING_KEY \n" +
                        "TEXT fixed\n" +
                        "TEXT 1230.15\n" +
                        "MAPPING_KEY \n" +
                        "TEXT negative infinity\n" +
                        "TEXT -.inf\n" +
                        "MAPPING_KEY \n" +
                        "TEXT not a number\n" +
                        "TEXT .NaN\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_20FloatingPoint.yaml"));
    }

    @Test
    public void eg2_21() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT null\n" +
                        "MAPPING_KEY \n" +
                        "TEXT booleans\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT true\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT false\n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT string\n" +
                        "TEXT ' 012345\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_21Miscellaneous.yaml"));
    }

    @Test
    public void eg2_22() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT canonical\n" +
                        "TEXT 2001-12-15T02:59:43.1Z\n" +
                        "MAPPING_KEY \n" +
                        "TEXT iso8601\n" +
                        "TEXT 2001-12-14t21:59:43.10-05:00\n" +
                        "MAPPING_KEY \n" +
                        "TEXT spaced\n" +
                        "TEXT 2001-12-14 21:59:43.10 -5\n" +
                        "MAPPING_KEY \n" +
                        "TEXT date\n" +
                        "TEXT 2002-12-14\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_22Timestamps.yaml"));
    }

    @Test
    public void eg2_23() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT not-date\n" +
                        "TAG !str\n" +
                        "TEXT 2002-04-28\n" +
                        "MAPPING_KEY \n" +
                        "TEXT picture\n" +
                        "TAG !binary\n" +
                        "TEXT R0lGODlhDAAMAIQAAP//9/X\n" +
                        "17unp5WZmZgAAAOfn515eXv\n" +
                        "Pz7Y6OjuDg4J+fn5OTk6enp\n" +
                        "56enmleECcgggoBADs=\n" +
                        "\n" +
                        "\n" +
                        "MAPPING_KEY \n" +
                        "TEXT application specific tag\n" +
                        "TAG something\n" +
                        "TEXT The semantics of the tag\n" +
                        "above may be different for\n" +
                        "different documents.\n" +
                        "\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_23VariousExplicitTags.yaml")
                        .replace("\r", ""));
    }

    @Test
    public void eg2_24() {
        assertEquals(
                "DIRECTIVE TAG ! tag:clarkevans.com,2002\n" +
                        "DIRECTIVES_END \n" +
                        "TAG shape\n" +
                        "COMMENT Use the ! handle for presenting\n" +
                        "COMMENT tag:clarkevans.com,2002:circle\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TAG circle\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT center\n" +
                        "TEXT &ORIGIN\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT x\n" +
                        "TEXT 73\n" +
                        "MAPPING_KEY \n" +
                        "TEXT y\n" +
                        "TEXT 129\n" +
                        "MAPPING_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT radius\n" +
                        "TEXT 7\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TAG line\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT start\n" +
                        "TEXT *ORIGIN\n" +
                        "MAPPING_KEY \n" +
                        "TEXT finish\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT x\n" +
                        "TEXT 89\n" +
                        "MAPPING_KEY \n" +
                        "TEXT y\n" +
                        "TEXT 102\n" +
                        "MAPPING_END \n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TAG label\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT start\n" +
                        "TEXT *ORIGIN\n" +
                        "MAPPING_KEY \n" +
                        "TEXT color\n" +
                        "TEXT 0xFFEEBB\n" +
                        "MAPPING_KEY \n" +
                        "TEXT text\n" +
                        "TEXT Pretty vector drawing.\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_24GlobalTags.yaml"));
    }

    @Test
    public void eg2_25() {
        assertEquals(
                "COMMENT Sets are represented as a\n" +
                        "COMMENT Mapping where each key is\n" +
                        "COMMENT associated with a null value\n" +
                        "DIRECTIVES_END \n" +
                        "TAG !set\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT Mark McGwire\n" +
                        "MAPPING_KEY \n" +
                        "TEXT Sammy Sosa\n" +
                        "MAPPING_KEY \n" +
                        "TEXT Ken Griff\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_25UnorderedSets.yaml"));
    }

    @Test
    public void eg2_26() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "TAG !omap\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT Mark McGwire\n" +
                        "TEXT 65\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT Sammy Sosa\n" +
                        "TEXT 63\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT Ken Griffy\n" +
                        "TEXT 58\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_26OrderedMappings.yaml"));
    }

    @Test
    public void eg2_27() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "TAG tag:clarkevans.com,2002:invoice\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT invoice\n" +
                        "TEXT 34843\n" +
                        "MAPPING_KEY \n" +
                        "TEXT date\n" +
                        "TEXT 2001-01-23\n" +
                        "MAPPING_KEY \n" +
                        "TEXT bill-to\n" +
                        "TEXT &id001\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT given\n" +
                        "TEXT Chris\n" +
                        "MAPPING_KEY \n" +
                        "TEXT family\n" +
                        "TEXT Dumars\n" +
                        "MAPPING_KEY \n" +
                        "TEXT address\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT lines\n" +
                        "TEXT 458 Walkman Dr.\n" +
                        "Suite #292\n" +
                        "\n" +
                        "MAPPING_KEY \n" +
                        "TEXT city\n" +
                        "TEXT Royal Oak\n" +
                        "MAPPING_KEY \n" +
                        "TEXT state\n" +
                        "TEXT MI\n" +
                        "MAPPING_KEY \n" +
                        "TEXT postal\n" +
                        "TEXT 48046\n" +
                        "MAPPING_END \n" +
                        "MAPPING_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT ship-to\n" +
                        "TEXT *id001\n" +
                        "MAPPING_KEY \n" +
                        "TEXT product\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT sku\n" +
                        "TEXT BL394D\n" +
                        "MAPPING_KEY \n" +
                        "TEXT quantity\n" +
                        "TEXT 4\n" +
                        "MAPPING_KEY \n" +
                        "TEXT description\n" +
                        "TEXT Basketball\n" +
                        "MAPPING_KEY \n" +
                        "TEXT price\n" +
                        "TEXT 450.00\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT sku\n" +
                        "TEXT BL4438H\n" +
                        "MAPPING_KEY \n" +
                        "TEXT quantity\n" +
                        "TEXT 1\n" +
                        "MAPPING_KEY \n" +
                        "TEXT description\n" +
                        "TEXT Super Hoop\n" +
                        "MAPPING_KEY \n" +
                        "TEXT price\n" +
                        "TEXT 2392.00\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT tax\n" +
                        "TEXT 251.42\n" +
                        "MAPPING_KEY \n" +
                        "TEXT total\n" +
                        "TEXT 4443.52\n" +
                        "MAPPING_KEY \n" +
                        "TEXT comments\n" +
                        "TEXT Late afternoon is best.\n" +
                        "TEXT Backup contact is Nancy\n" +
                        "TEXT Billsmer @ 338-4338.\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_27Invoice.yaml")
                        .replace("\r", ""));
    }

    @Test
    public void eg2_28() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT Time\n" +
                        "TEXT 2001-11-23 15:01:42 -5\n" +
                        "MAPPING_KEY \n" +
                        "TEXT User\n" +
                        "TEXT ed\n" +
                        "MAPPING_KEY \n" +
                        "TEXT Warning\n" +
                        "TEXT This is an error message\n" +
                        "TEXT for the log file\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n" +
                        "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT Time\n" +
                        "TEXT 2001-11-23 15:02:31 -5\n" +
                        "MAPPING_KEY \n" +
                        "TEXT User\n" +
                        "TEXT ed\n" +
                        "MAPPING_KEY \n" +
                        "TEXT Warning\n" +
                        "TEXT A slightly different error\n" +
                        "TEXT message.\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n" +
                        "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT Date\n" +
                        "TEXT 2001-11-23 15:03:17 -5\n" +
                        "MAPPING_KEY \n" +
                        "TEXT User\n" +
                        "TEXT ed\n" +
                        "MAPPING_KEY \n" +
                        "TEXT Fatal\n" +
                        "TEXT Unknown variable \"bar\"\n" +
                        "MAPPING_KEY \n" +
                        "TEXT Stack\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT file\n" +
                        "TEXT TopClass.py\n" +
                        "MAPPING_KEY \n" +
                        "TEXT line\n" +
                        "TEXT 23\n" +
                        "MAPPING_KEY \n" +
                        "TEXT code\n" +
                        "TEXT x = MoreObject(\"345\\n\")\n" +
                        "\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT file\n" +
                        "TEXT MoreClass.py\n" +
                        "MAPPING_KEY \n" +
                        "TEXT line\n" +
                        "TEXT 58\n" +
                        "MAPPING_KEY \n" +
                        "TEXT code\n" +
                        "TEXT |-\n" +
                        "TEXT foo = bar\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_28LogFile.yaml").replace("\r", ""));
    }

    @Test
    public void sample1() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT ladderDefinitions\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TAG LadderDefinition\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT ccyPair\n" +
                        "TEXT NZDUSD\n" +
                        "MAPPING_KEY \n" +
                        "TEXT name\n" +
                        "TEXT NZDUSD-CNX\n" +
                        "MAPPING_KEY \n" +
                        "TEXT ecns\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT CNX\n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT maxPriceMoveInPipsFromTopOfBook\n" +
                        "TEXT 400\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n" +
                        "DIRECTIVES_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/sample1.yaml").replace("\r", ""));
    }

    @Test
    public void sample2() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "TAG !meta-data\n" +
                        "TAG net.openhft.chronicle.wire.DemarshallableObject\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT name\n" +
                        "TEXT test\n" +
                        "MAPPING_KEY \n" +
                        "TEXT value\n" +
                        "TEXT 12345\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/sample2.yaml").replace("\r", ""));
    }

    @Test
    public void sample3() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT A\n" +
                        "TAG net.openhft.chronicle.wire.DemarshallableObject\n" +
                        "MAPPING_START \n" +
                        "MAPPING_END \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/sample3.yaml").replace("\r", ""));
    }

    @Test
    public void sample4() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT a\n" +
                        "TAG type\n" +
                        "TEXT \" [B\n" +
                        "MAPPING_KEY \n" +
                        "TEXT b\n" +
                        "TAG type\n" +
                        "TEXT \" String[]\n" +
                        "MAPPING_KEY \n" +
                        "TEXT c\n" +
                        "TEXT hi\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/sample4.yaml").replace("\r", ""));
    }
}