/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StopCharTestersTest extends WireTestCommon {

    @Test
    @DisplayName("Query stop char testers detect delimiters")
    void queryStopCharTestersDetectDelimiters() {
        assertTrue(QueryWire.QueryStopCharTesters.QUERY_FIELD_NAME.isStopChar('&'),
                "Field names stop at the '&' delimiter");
        assertTrue(QueryWire.QueryStopCharTesters.QUERY_FIELD_NAME.isStopChar('='),
                "Field names stop at the '=' assignment");
        assertTrue(QueryWire.QueryStopCharTesters.QUERY_FIELD_NAME.isStopChar(-1),
                "Field names stop at end of input");
        assertTrue(QueryWire.QueryStopCharTesters.QUERY_VALUE.isStopChar(-1),
                "Query values stop at end of input");
        assertFalse(QueryWire.QueryStopCharTesters.QUERY_VALUE.isStopChar('a'),
                "Query values continue on normal characters");
    }

    @Test
    @DisplayName("Text stop char testers respect end of text rules")
    void textStopCharTestersRespectEndOfTextRules() {
        assertTrue(TextStopCharTesters.END_OF_TEXT.isStopChar('"'),
                "Quotes terminate text blocks");
        assertFalse(TextStopCharTesters.END_OF_TEXT.isStopChar('x'),
                "Ordinary characters do not terminate text blocks");
    }

    @Test
    @DisplayName("Text stop char testers detect end of type markers")
    void textStopCharTestersDetectEndOfTypeMarkers() {
        assertTrue(TextStopCharTesters.END_OF_TYPE.isStopChar('#'),
                "Non-identifier characters stop types");
        assertFalse(TextStopCharTesters.END_OF_TYPE.isStopChar('A'),
                "Identifier characters are not stop markers");
        assertTrue(TextStopCharTesters.END_OF_TYPE.isStopChar(200),
                "Characters outside the bit set range stop types");
    }

    @Test
    @DisplayName("Text stop chars testers respect separators")
    void textStopCharsTestersRespectSeparators() {
        assertTrue(TextStopCharsTesters.isASeparator(' '),
                "Whitespace is a separator");
        assertFalse(TextStopCharsTesters.isASeparator('a'),
                "Alphabetic characters are not separators");
        assertTrue(TextStopCharsTesters.STRICT_END_OF_TEXT.isStopChar(':', ' '),
                "Colon stops when followed by a separator");
        assertFalse(TextStopCharsTesters.STRICT_END_OF_TEXT.isStopChar(':', 'a'),
                "Colon does not stop when followed by a non-separator");
        assertTrue(TextStopCharsTesters.STRICT_END_OF_TEXT_JSON.isStopChar(':', 'a'),
                "Strict JSON end-of-text stops on separators");
        assertTrue(TextStopCharsTesters.END_EVENT_NAME.isStopChar(' ', 'a'),
                "Event names stop at whitespace");
        assertTrue(TextStopCharsTesters.END_EVENT_NAME.isStopChar(':', ' '),
                "Event names stop at strict end-of-text markers");
        assertFalse(TextStopCharsTesters.END_EVENT_NAME.isStopChar('a', 'b'),
                "Event names continue on normal characters");
    }
}
