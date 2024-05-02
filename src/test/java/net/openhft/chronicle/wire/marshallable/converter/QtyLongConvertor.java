package net.openhft.chronicle.wire.marshallable.converter;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.LongConverter;


import static java.lang.Math.pow;
import static java.util.stream.IntStream.range;


/**
 * The QtyLongConvertor class is used to convert a quantity value represented as a long data type to and from a
 * human-readable string format with units (trillions, billions, millions, or thousands).
 * The class implements the LongConverter interface from the net.openhft.chronicle.wire package.
 */
public class QtyLongConvertor implements LongConverter {



    public static QtyLongConvertor INSTANCE = new QtyLongConvertor();
    private static final long[] POWERS_OF_TEN = range(0, 16).mapToLong(i -> (long) pow(10, i)).toArray();


    public static void main(String[] args) {
        System.out.println(POWERS_OF_TEN[3]);
    }

    private static final long K = POWERS_OF_TEN[3];
    private static final long M = POWERS_OF_TEN[6];
    private static final long B = POWERS_OF_TEN[9];
    private static final long T = POWERS_OF_TEN[12];
    private final Bytes<?> b = Bytes.elasticHeapByteBuffer();

    /**
     * Parses a CharSequence object (i.e., a string) representing a quantity value with units and returns a long value.
     *
     * @param text the input CharSequence to be parsed
     * @return the parsed long value, with appropriate units (trillions, billions, millions, or thousands)
     */
    @Override
    public long parse(CharSequence text) {
        b.clear().append(text);

        char lastChar = text.charAt(text.length() - 1);

        switch (lastChar) {
            case 'T':
                return (long) (b.parseDouble() * T);
            case 'B':
                return (long) (b.parseDouble() * B);
            case 'M':
                return (long) (b.parseDouble() * M);
            case 'K':
                return (long) (b.parseDouble() * K);
        }
        return (long) b.parseDouble();
    }

    /**
     * Appends a human-readable string representation of a long value with units (trillions, billions, millions, or
     * thousands) to a StringBuilder object.
     *
     * @param text  the StringBuilder object to which the string representation is appended
     * @param value the long value to be converted to a string representation
     */
    @Override
    public void append(StringBuilder text, long value) {
        if (Math.abs(value) < K) {
            text.append(value);
            return;
        } else if (value / T == (double) value / T) {
            text.append(value / T);
            text.append("T");
        } else if (value / B == (double) value / B) {
            text.append(value / B);
            text.append("B");
            return;
        } else if (value / M == (double) value / M) {
            text.append(value / M);
            text.append("M");
            return;
        } else if (value / K == (double) value / K) {
            text.append(value / K);
            text.append("K");
            return;
        }

        text.append(value);
    }
}
