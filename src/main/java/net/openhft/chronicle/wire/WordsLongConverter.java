package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IOTools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class WordsLongConverter implements LongConverter {
    static final Pattern NON_LETTER = Pattern.compile("\\W");
    static final String[] WORDS;
    static final Map<String, Integer> WORD_ID = new HashMap<>();

    static {
        try {
            String[] words = new String(IOTools.readFile("common-words.txt"), StandardCharsets.ISO_8859_1).split("\\s+");
            WORDS = Arrays.copyOf(words, 4096);
            for (int i = 0; i < WORDS.length; i++) {
                String word = WORDS[i];
                Integer ii = WORD_ID.put(word, i);
                assert ii == null : "Duplicate " + word;

            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private final String sep;

    public WordsLongConverter() {
        this('.');
    }

    public WordsLongConverter(char sep) {
        this.sep = Character.toString(sep);
    }

    static int sign(double d) {
        return (int) (Double.doubleToRawLongBits(d) >>> 63);
    }

    public static String latLong(double lat, double lon, double precision) {
        int lats = sign(lat);
        int lons = sign(lon);
        double lata = Math.abs(lat) / 90.0;
        double lona = Math.abs(lon) / 180.0;
        precision /= 180;
        StringBuilder ret = new StringBuilder();
        final int factor = 64;
        {
            lata *= factor / 2;
            lona *= factor / 2;
            int lati = (int) Math.floor(lata);
            int loni = (int) Math.floor(lona);
            lata -= lati;
            lona -= loni;

            lati = lati * 2 + lats;
            loni = loni * 2 + lons;
            int index = loni * factor + lati;
            ret.append(WORDS[index]);
            precision *= factor;
        }

        while (precision < 1) {
            lata *= factor;
            lona *= factor;
            int lati = (int) Math.floor(lata);
            int loni = (int) Math.floor(lona);
            lata -= lati;
            lona -= loni;
            int index = loni * factor + lati;
            ret.append(".").append(WORDS[index]);
            precision *= factor;
        }
        return ret.toString();
    }

    @Override
    public long parse(CharSequence text) {
        String[] split = NON_LETTER.split(text.toString().trim(), 0);
        long value = 0;
        int shift = 0;
        for (String s : split) {
            Integer id = WORD_ID.get(s);
            if (id == null)
                throw new IllegalArgumentException("Unknown word'" + s + "'");
            value += id.longValue() << shift;
            shift += 11;
        }
        return value;
    }

    @Override
    public void append(StringBuilder text, long value) {
        String sep = "";
        do {
            text.append(sep);
            text.append(WORDS[(int) (value & 2047)]);
            value >>>= 11;
            sep = this.sep;
        } while (value > 0);
    }
}
