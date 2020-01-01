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
            YamlTokeniser yn = new YamlTokeniser(bytes);
            StringBuilder sb = new StringBuilder();
            for (YamlToken t; (t = yn.next()) != YamlToken.NONE; ) {
                sb.append(t).append(' ').append(yn.text()).append('\n');
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
                        "ANCHOR SS\n" +
                        "TEXT Sammy Sosa\n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT rbi\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "ALIAS SS\n" +
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
    public void eg2_17() {
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT unicode\n" +
                        "TEXT Sosa did fine.\\u263A\n" +
                        "MAPPING_KEY \n" +
                        "TEXT control\n" +
                        "TEXT \\b1998\\t1999\\t2000\\n\n" +
                        "MAPPING_KEY \n" +
                        "TEXT hex esc\n" +
                        "TEXT \\x0d\\x0a is \\r\\n\n" +
                        "MAPPING_KEY \n" +
                        "TEXT single\n" +
                        "TEXT \"Howdy!\" he cried.\n" +
                        "MAPPING_KEY \n" +
                        "TEXT quoted\n" +
                        "TEXT  # Not a \n" +
                        "TEXT comment\n" +
                        "TEXT .\n" +
                        "MAPPING_KEY \n" +
                        "TEXT tie-fighter\n" +
                        "TEXT |\\-*-/|\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_17QuotedScalars.yaml"));
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
                        "TEXT 012345\n" +
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

    @Ignore("TODO FIX")
    @Test
    public void eg2_23() {
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
                doTest("yaml/spec/2_23VariousExplicitTags.yaml"));
    }

    @Ignore("TODO FIX")
    @Test
    public void eg2_24() {
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
                doTest("yaml/spec/2_24GlobalTags.yaml"));
    }

    @Ignore("TODO FIX")
    @Test
    public void eg2_25() {
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
                doTest("yaml/spec/2_25UnorderedSets.yaml"));
    }

    @Ignore("TODO FIX")
    @Test
    public void eg2_26() {
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
                doTest("yaml/spec/2_26OrderedMappings.yaml"));
    }

    @Ignore("TODO FIX")
    @Test
    public void eg2_27() {
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
                doTest("yaml/spec/2_27Invoice.yaml"));
    }

    @Ignore("TODO FIX")
    @Test
    public void eg2_28() {
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
                doTest("yaml/spec/2_28LogFile.yaml"));
    }


}