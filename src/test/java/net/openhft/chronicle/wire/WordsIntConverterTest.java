package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WordsIntConverterTest {
    @Test
    public void parse() {
        IntConverter bic = new WordsIntConverter();
        for (int l : new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            String text = bic.asString(l);
            System.out.println(text);
            assertEquals(l, bic.parse(text));
        }
    }
}