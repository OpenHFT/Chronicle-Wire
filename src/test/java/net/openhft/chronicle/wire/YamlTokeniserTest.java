/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class YamlTokeniserTest extends WireTestCommon {

    // Utility function to tokenize YAML from a given resource file
    public static String doTest(String resource) {
        try {
            // Reads the file into a Bytes object
            Bytes<?> bytes = BytesUtil.readFile(resource);

            // Uncomment to remove carriage return characters
            // bytes = Bytes.from(bytes.toString().replace("\r", ""));

            // Initialize a new YamlTokeniser with the Bytes object
            YamlTokeniser yt = new YamlTokeniser(bytes);

            // StringBuilder to collect tokens
            StringBuilder sb = new StringBuilder();

            // Tokenize the YAML, but limit to 100 tokens for safety
            int i = 0;
            while (yt.next(Integer.MIN_VALUE) != YamlToken.STREAM_END) {
                sb.append(yt).append('\n');
                if (++i >= 100) {
                    sb.append(".......\n");
                    break;
                }
            }
            return sb.toString();
        } catch (IOException e) {
            // If any IOException occurs, throw an AssertionError
            throw new AssertionError(e);
        }
    }

    // Test to verify the tokenization of a specific YAML file representing Morse code
    @Test
    public void morseCode() {
        // The expected tokenized representation of the morse-code.yaml file
        assertEquals("" +
                        "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT A\n" +
                        "TEXT .-\n" +
                        "MAPPING_KEY \n" +
                        "TEXT B\n" +
                        "TEXT -...\n" +
                        "MAPPING_KEY \n" +
                        "TEXT C\n" +
                        "TEXT -.-.\n" +
                        "MAPPING_KEY \n" +
                        "TEXT D\n" +
                        "TEXT -..\n" +
                        "MAPPING_KEY \n" +
                        "TEXT E\n" +
                        "TEXT .\n" +
                        "MAPPING_KEY \n" +
                        "TEXT F\n" +
                        "TEXT ..-.\n" +
                        "MAPPING_KEY \n" +
                        "TEXT G\n" +
                        "TEXT --.\n" +
                        "MAPPING_KEY \n" +
                        "TEXT H\n" +
                        "TEXT ....\n" +
                        "MAPPING_KEY \n" +
                        "TEXT I\n" +
                        "TEXT ..\n" +
                        "MAPPING_KEY \n" +
                        "TEXT J\n" +
                        "TEXT .---\n" +
                        "MAPPING_KEY \n" +
                        "TEXT K\n" +
                        "TEXT -.-\n" +
                        "MAPPING_KEY \n" +
                        "TEXT L\n" +
                        "TEXT .-..\n" +
                        "MAPPING_KEY \n" +
                        "TEXT M\n" +
                        "TEXT --\n" +
                        "MAPPING_KEY \n" +
                        "TEXT N\n" +
                        "TEXT -.\n" +
                        "MAPPING_KEY \n" +
                        "TEXT O\n" +
                        "TEXT ---\n" +
                        "MAPPING_KEY \n" +
                        "TEXT P\n" +
                        "TEXT .--.\n" +
                        "MAPPING_KEY \n" +
                        "TEXT Q\n" +
                        "TEXT --.-\n" +
                        "MAPPING_KEY \n" +
                        "TEXT R\n" +
                        "TEXT .-.\n" +
                        "MAPPING_KEY \n" +
                        "TEXT S\n" +
                        "TEXT ...\n" +
                        "MAPPING_KEY \n" +
                        "TEXT T\n" +
                        "TEXT \" -\n" +
                        "MAPPING_KEY \n" +
                        "TEXT U\n" +
                        "TEXT ..-\n" +
                        "MAPPING_KEY \n" +
                        "TEXT V\n" +
                        "TEXT ...-\n" +
                        "MAPPING_KEY \n" +
                        "TEXT W\n" +
                        "TEXT .--\n" +
                        "MAPPING_KEY \n" +
                        "TEXT X\n" +
                        "TEXT -..-\n" +
                        "MAPPING_KEY \n" +
                        "TEXT Y\n" +
                        "TEXT -.--\n" +
                        "MAPPING_KEY \n" +
                        "TEXT Z\n" +
                        "TEXT --..\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("morse-code.yaml")); // Invoke the tokenization utility and verify the output
    }

 // Test case for tokenizing YAML content with mixed quotes
    @Test
    public void mixedQuotes() {
        // The expected tokenized representation of the mixed-quotes.yaml file
        assertEquals("" +
                        "DIRECTIVES_END \n" +
                        "SEQUENCE_START \n" +
                        "TEXT \" \\\"\n" +
                        "TEXT ' \"\n" +
                        "TEXT ' \\\n" +
                        "TEXT ' \"\n" +
                        "TEXT \" \\\"'\n" +
                        "TEXT \" \\\"\n" +
                        "TEXT \" \\\"\\\"\n" +
                        "TEXT \" \\'\\'\n" +
                        "SEQUENCE_END \n" +
                        "DOCUMENT_END \n",
                doTest("mixed-quotes.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing YAML content that describes an exception
    @Test
    public void exception() {
        // The expected tokenized representation of the exception.yaml file
        assertEquals("DIRECTIVES_END \n" +
                        "TAG !data\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT exception\n" +
                        "TAG java.security.InvalidAlgorithmParameterException\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT message\n" +
                        "TEXT Reference cannot be null\n" +
                        "MAPPING_KEY \n" +
                        "TEXT stackTrace\n" +
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT class\n" +
                        "TEXT net.openhft.chronicle.wire.YamlWireTest\n" +
                        "MAPPING_KEY \n" +
                        "TEXT method\n" +
                        "TEXT testException\n" +
                        "MAPPING_KEY \n" +
                        "TEXT file\n" +
                        "TEXT YamlWireTest.java\n" +
                        "MAPPING_KEY \n" +
                        "TEXT line\n" +
                        "TEXT 783\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT class\n" +
                        "TEXT net.openhft.chronicle.wire.YamlWireTest\n" +
                        "MAPPING_KEY \n" +
                        "TEXT method\n" +
                        "TEXT runTestException\n" +
                        "MAPPING_KEY \n" +
                        "TEXT file\n" +
                        "TEXT YamlWireTest.java\n" +
                        "MAPPING_KEY \n" +
                        "TEXT line\n" +
                        "TEXT 73\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_ENTRY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT class\n" +
                        "TEXT sun.reflect.NativeMethodAccessorImpl\n" +
                        "MAPPING_KEY \n" +
                        "TEXT method\n" +
                        "TEXT invoke0\n" +
                        "MAPPING_KEY \n" +
                        "TEXT file\n" +
                        "TEXT NativeMethodAccessorImpl.java\n" +
                        "MAPPING_KEY \n" +
                        "TEXT line\n" +
                        "TEXT -2\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_END \n" +
                        "MAPPING_END \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("exception.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing YAML content with incomplete int mapping
    @Test
    public void intMappingIncomplete() {
        // The expected tokenized representation of the int-mapping-incomplete.yaml
        assertEquals("DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT example\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TAG int\n" +
                        "MAPPING_END \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/int-mapping-incomplete.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing YAML content with a complete int mapping
    @Test
    public void intMapping() {
        // The expected tokenized representation of the int-mapping.yaml file
        assertEquals("DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT example\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TAG int\n" +
                        "TEXT 1\n" +
                        "TAG int\n" +
                        "TEXT 11\n" +
                        "MAPPING_KEY \n" +
                        "TAG int\n" +
                        "TEXT 2\n" +
                        "TAG int\n" +
                        "TEXT 2\n" +
                        "MAPPING_KEY \n" +
                        "TAG int\n" +
                        "TEXT 3\n" +
                        "TAG int\n" +
                        "TEXT 3\n" +
                        "MAPPING_END \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/int-mapping.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing YAML content with a complex mapping structure
    @Test
    public void complexMapping() {
        // The expected tokenized representation of the complex-mapping.yaml file
        assertEquals("DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT MyField\n" +
                        "TEXT parent\n" +
                        "MAPPING_KEY \n" +
                        "TEXT Id\n" +
                        "TEXT 1\n" +
                        "MAPPING_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TAG net.openhft.chronicle.wire.MyMarshallable\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT MyField\n" +
                        "TEXT key1\n" +
                        "MAPPING_KEY \n" +
                        "TEXT Id\n" +
                        "TEXT 1\n" +
                        "MAPPING_END \n" +
                        "TEXT value1\n" +
                        "MAPPING_KEY \n" +
                        "TAG net.openhft.chronicle.wire.MyMarshallable\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT MyField\n" +
                        "TEXT key2\n" +
                        "MAPPING_KEY \n" +
                        "TEXT Id\n" +
                        "TEXT 2\n" +
                        "MAPPING_END \n" +
                        "TEXT value2\n" +
                        "MAPPING_END \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/complex-mapping.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML sequence of scalar values
    @Test
    public void eg2_1() {
        // The expected tokenized representation of the 2_1_SequenceOfScalars.yaml file
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
                doTest("yaml/spec/2_1_SequenceOfScalars.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML mapping of scalar to scalar values
    @Test
    public void eg2_2() {
        // The expected tokenized representation of the 2_2_MappingScalarsToScalars.yaml file
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
                doTest("yaml/spec/2_2_MappingScalarsToScalars.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML mapping of scalar to sequence values
    @Test
    public void eg2_3() {
        // The expected tokenized representation of the 2_3_MappingScalarsToSequences.yaml file
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
                doTest("yaml/spec/2_3_MappingScalarsToSequences.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML sequence of mappings
    @Test
    public void eg2_4() {
        // The expected tokenized representation of the 2_4_SequenceOfMappings.yaml file
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
                doTest("yaml/spec/2_4_SequenceOfMappings.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for verifying the tokenized output of an alternative version of 2_4_SequenceOfMappings.yaml
    @Test
    public void eg2_4out() {
        // The expected tokenized representation of the 2_4_SequenceOfMappings.out.yaml file
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
                doTest("yaml/spec/2_4_SequenceOfMappings.out.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Another test case for verifying the tokenized output of the 2_4_SequenceOfMappings.yaml
    @Test
    public void eg2_4B() {
        // The expected tokenized representation of the 2_4_SequenceOfMappings.yaml file
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
                doTest("yaml/spec/2_4_SequenceOfMappings.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML sequence of sequences
    @Test
    public void eg2_5() {
        // The expected tokenized representation of the 2_5_SequenceOfSequences.yaml file
        assertEquals("DIRECTIVES_END \n" +
                        "SEQUENCE_START \n" +
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
                        "SEQUENCE_START \n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT Mark McGwire\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT 65\n" +
                        "SEQUENCE_ENTRY \n" +
                        "TEXT 0.278\n" +
                        "SEQUENCE_END \n" +
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
                doTest("yaml/spec/2_5_SequenceOfSequences.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML mapping of mappings structure
    @Test
    public void eg2_6() {
        // The expected tokenized representation of the 2_6_MappingOfMappings.yaml file
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
                doTest("yaml/spec/2_6_MappingOfMappings.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file with two separate documents in a single stream
    @Test
    public void eg2_7() {
        // The expected tokenized representation of the 2_7_TwoDocumentsInAStream.yaml file
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
                doTest("yaml/spec/2_7_TwoDocumentsInAStream.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file containing play-by-play actions in a sports event
    @Test
    public void eg2_8() {
        // The expected tokenized representation of the 2_8_PlayByPlayFeed.yaml file
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
                doTest("yaml/spec/2_8_PlayByPlayFeed.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file with comments indicating 1998 HR and RBI rankings
    @Test
    public void eg2_9() {
        // The expected tokenized representation of the 2_9_SingleDocumentWithTwoComments.yaml file
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
                doTest("yaml/spec/2_9_SingleDocumentWithTwoComments.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file featuring node anchors and aliases
    @Test
    public void eg2_10() {
        // The expected tokenized representation of the 2_10_NodeAppearsTwiceInThisDocument.yaml file
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
                doTest("yaml/spec/2_10_NodeAppearsTwiceInThisDocument.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file with complex mappings between sequences
    @Test
    public void eg2_11() {
        // The expected tokenized representation of the 2_11MappingBetweenSequences.yaml file
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
                doTest("yaml/spec/2_11MappingBetweenSequences.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file featuring a compact nested mapping of purchased items and their quantities
    @Test
    public void eg2_12() {
        // The expected tokenized representation of the 2_12CompactNestedMapping.yaml file
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
                doTest("yaml/spec/2_12CompactNestedMapping.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file that preserves newlines within literal blocks (ASCII Art)
    @Test
    public void eg2_13() {
        // The expected tokenized representation of the 2_13InLiteralsNewlinesArePreserved.yaml file
        assertEquals(
                "COMMENT ASCII Art\n" +
                        "DIRECTIVES_END \n" +
                        "LITERAL \\//||\\/||\n" +
                        "// ||  ||__\n\n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_13InLiteralsNewlinesArePreserved.yaml").replace("\r", "")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file that uses folded scalars to preserve newlines only at the end of double-indented lines
    @Test
    public void eg2_14() {
        // The expected tokenized representation of the 2_14InThefoldedScalars.yaml file
        assertEquals(
                "DIRECTIVES_END \n" +
                        "LITERAL Mark McGwire's year was crippled by a knee injury.\n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_14InThefoldedScalars.yaml").replace("\r", "")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file featuring folded newlines
    @Test
    public void eg2_15() {
        // The expected tokenized representation of the 2_15FoldedNewlines.yaml file
        assertEquals(
                "LITERAL Sammy Sosa completed another fine season with great stats.   63 Home Runs   0.288 Batting Average What a year!\n",
                doTest("yaml/spec/2_15FoldedNewlines.yaml").replace("\r", "")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file that relies on indentation to determine scope
    @Test
    public void eg2_16() {
        // The expected tokenized representation of the 2_16IndentationDeterminesScope.yaml file
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT name\n" +
                        "TEXT Mark McGwire\n" +
                        "MAPPING_KEY \n" +
                        "TEXT accomplishment\n" +
                        "LITERAL Mark set a major league home run record in 1998.\n" +
                        "MAPPING_KEY \n" +
                        "TEXT stats\n" +
                        "LITERAL 65 Home Runs\n" +
                        "0.278 Batting Average\n" +
                        "\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_16IndentationDeterminesScope.yaml").replace("\r", "")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file that contains various types of quoted scalars including Unicode, control, hex escapes, and single-quoted text
    @Test
    public void eg2_17() {
        // The expected tokenized representation of the 2_17QuotedScalars.yaml file
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
                doTest("yaml/spec/2_17QuotedScalars.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file that includes multi-line flow scalars, both plain and quoted
    @Ignore("TODO FIX")  // This test is currently ignored and needs fixing
    @Test
    public void eg2_18() {
        // The expected tokenized representation of the 2_18Multi_lineFlowScalars.yaml file
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
                doTest("yaml/spec/2_18Multi_lineFlowScalars.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file that contains various representations of integers
    @Test
    public void eg2_19() {
        // The expected tokenized representation of the 2_19Integers.yaml file
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT canonical\n" +
                        "TEXT 12345\n" +
                        "MAPPING_KEY \n" +
                        "TEXT decimal\n" +
                        "TEXT +12_345\n" +
                        "MAPPING_KEY \n" +
                        "TEXT sexagesimal\n" +
                        "TEXT 3:25:45\n" +
                        "MAPPING_KEY \n" +
                        "TEXT octal\n" +
                        "TEXT 0o14\n" +
                        "MAPPING_KEY \n" +
                        "TEXT hexadecimal\n" +
                        "TEXT 0xC\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_19Integers.yaml")); // Invoke the tokenization utility and verify the output
    }

    @Test
    public void eg2_20() {
        // The expected tokenized representation of the 2_20FloatingPoint.yaml file
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
                        "TEXT sexagesimal\n" +
                        "TEXT 20:30.15\n" +
                        "MAPPING_KEY \n" +
                        "TEXT fixed\n" +
                        "TEXT 1_230.15\n" +
                        "MAPPING_KEY \n" +
                        "TEXT negative infinity\n" +
                        "TEXT -.inf\n" +
                        "MAPPING_KEY \n" +
                        "TEXT not a number\n" +
                        "TEXT .NaN\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_20FloatingPoint.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file that contains miscellaneous types like null, boolean, and string
    @Test
    public void eg2_21() {
        // The expected tokenized representation of the 2_21Miscellaneous.yaml file
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
                doTest("yaml/spec/2_21Miscellaneous.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file that contains various representations of timestamps
    @Test
    public void eg2_22() {
        // The expected tokenized representation of the 2_22Timestamps.yaml file
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
                doTest("yaml/spec/2_22Timestamps.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file that contains various explicit tags including application-specific tags
    @Test
    public void eg2_23() {
        // The expected tokenized representation of the 2_23VariousExplicitTags.yaml file
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
                        "LITERAL R0lGODlhDAAMAIQAAP//9/X\n" +
                        "17unp5WZmZgAAAOfn515eXv\n" +
                        "Pz7Y6OjuDg4J+fn5OTk6enp\n" +
                        "56enmleECcgggoBADs=\n" +
                        "\n" +
                        "\n" +
                        "MAPPING_KEY \n" +
                        "TEXT application specific tag\n" +
                        "TAG something\n" +
                        "LITERAL The semantics of the tag\n" +
                        "above may be different for\n" +
                        "different documents.\n" +
                        "\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_23VariousExplicitTags.yaml")
                        .replace("\r", "")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file that contains globally-defined tags
    @Test
    public void eg2_24() {
        // The expected tokenized representation of the 2_24GlobalTags.yaml file
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
                        "ANCHOR ORIGIN\n" +
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
                        "ALIAS ORIGIN\n" +
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
                        "ALIAS ORIGIN\n" +
                        "MAPPING_KEY \n" +
                        "TEXT color\n" +
                        "TEXT 0xFFEEBB\n" +
                        "MAPPING_KEY \n" +
                        "TEXT text\n" +
                        "TEXT Pretty vector drawing.\n" +
                        "MAPPING_END \n" +
                        "SEQUENCE_END \n" +
                        "DOCUMENT_END \n",
                doTest("yaml/spec/2_24GlobalTags.yaml")); // Invoke the tokenization utility and verify the output
    }

    @Ignore("TODO FIX")
    // Test case for tokenizing a YAML file representing an unordered set
    @Test
    public void eg2_25() {
        // Expected tokenized representation of the 2_25UnorderedSets.yaml file
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
                doTest("yaml/spec/2_25UnorderedSets.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file representing an ordered mapping
    @Test
    public void eg2_26() {
        // Expected tokenized representation of the 2_26OrderedMappings.yaml file
        assertEquals(
                "COMMENT ordered maps are represented as\n" +
                        "COMMENT a sequence of mappings, with\n" +
                        "COMMENT each mapping having one key\n" +
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
                doTest("yaml/spec/2_26OrderedMappings.yaml")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a complex YAML file representing an invoice
    @Test
    public void eg2_27() {
        // Expected tokenized representation of the 2_27Invoice.yaml file
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
                        "ANCHOR id001\n" +
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
                        "LITERAL 458 Walkman Dr.\n" +
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
                        "ALIAS id001\n" +
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
                        .replace("\r", "")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file representing log files with multiple documents
    @Test
    public void eg2_28() {
        // Expected tokenized representation of the 2_28LogFile.yaml file
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
                        "LITERAL x = MoreObject(\"345\\n\")\n" +
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
                doTest("yaml/spec/2_28LogFile.yaml").replace("\r", "")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file representing a sample configuration
    @Test
    public void sample1() {
        // Expected tokenized representation of the sample1.yaml file
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
                doTest("yaml/sample1.yaml").replace("\r", "")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML file containing metadata and a custom object type
    @Test
    public void sample2() {
        // Expected tokenized representation of the sample2.yaml file
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
                doTest("yaml/sample2.yaml").replace("\r", "")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML string containing an empty custom object type
    @Test
    public void sample3() {
        // Expected tokenized representation of the sample3.yaml string
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
                doTest("=A: !net.openhft.chronicle.wire.DemarshallableObject{}")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML string containing multiple types of data
    @Test
    public void sample4() {
        // Expected tokenized representation of the sample4.yaml string
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
                doTest("=a: !type \"[B\", b: !type \"String[]\", c: hi")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML string containing a mapping of mappings
    @Test
    public void sample5() {
        // Expected tokenized representation of the sample5.yaml string
        assertEquals(
                "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT A\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT b\n" +
                        "TEXT 1234\n" +
                        "MAPPING_KEY \n" +
                        "TEXT c\n" +
                        "TEXT hi\n" +
                        "MAPPING_KEY \n" +
                        "TEXT d\n" +
                        "TEXT abc\n" +
                        "MAPPING_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT B\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT c\n" +
                        "TEXT lo\n" +
                        "MAPPING_KEY \n" +
                        "TEXT d\n" +
                        "TEXT xyz\n" +
                        "MAPPING_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT C\n" +
                        "TEXT see\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest(
                        "=A : \n" +
                                "  b  : 1234\n" +
                                "  c\t: hi\n" +
                                "  d: abc\n" +
                                "B: \n" +
                                "  c: lo\n" +
                                "  d: xyz\n" +
                                "C: see\n"));  // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML string containing nested mappings and an empty mapping
    @Test
    public void sample6() {
        // Expected tokenized representation of the sample6.yaml string
        assertEquals(
                "COMMENT \n" +
                        "DIRECTIVES_END \n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT b\n" +
                        "TEXT AA\n" +
                        "MAPPING_KEY \n" +
                        "TEXT c\n" +
                        "MAPPING_START \n" +
                        "MAPPING_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT d\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT A\n" +
                        "TEXT 1\n" +
                        "MAPPING_KEY \n" +
                        "TEXT B\n" +
                        "TEXT 2\n" +
                        "MAPPING_END \n" +
                        "MAPPING_KEY \n" +
                        "TEXT e\n" +
                        "TEXT end\n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest(
                        "=" + "#\nb: AA\nc: {}\nd: \n  A: 1\n  B: 2\ne: end")); // Invoke the tokenization utility and verify the output
    }

    // Test case for tokenizing a YAML string containing a tag and various field types
    @Test
    public void sample7() {
        // Expected tokenized representation of the sample7.yaml string
        assertEquals(
                "DIRECTIVES_END \n" +
                        "TAG Data\n" +
                        "MAPPING_START \n" +
                        "MAPPING_KEY \n" +
                        "TEXT name\n" +
                        "TEXT NAME\n" +
                        "MAPPING_KEY \n" +
                        "TEXT time\n" +
                        "TEXT 12:34:45\n" +
                        "MAPPING_KEY \n" +
                        "TEXT empty\n" +
                        "TEXT ' \n" +
                        "MAPPING_END \n" +
                        "DOCUMENT_END \n",
                doTest(
                        "=!Data {\n" +
                                "  name : NAME,\n" +
                                "  time\t: 12:34:45,\n" +
                                "  empty  : ''\n" +
                                "}\n")); // Invoke the tokenization utility and verify the output
    }
}
