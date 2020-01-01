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

    @Ignore("TODO FIX")
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
                        "MAPPING_KEY \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT New York Yankees\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Atlanta Braves \n" +
                        "SEQUENCE_END \n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT 2001-07-02\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT 2001-08-12\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT 2001-08-14 \n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_11MappingBetweenSequences.yaml"));
    }


}