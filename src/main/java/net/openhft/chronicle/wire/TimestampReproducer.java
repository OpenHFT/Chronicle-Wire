package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.ServicesTimestampLongConverter;

public class TimestampReproducer {
    public static void main(String[] args) {
        // Print out the configured time unit.
        System.out.println("Configured time unit: " + ServicesTimestampLongConverter.timeUnit());

        String isoTimestamp = "2025-03-04T09:58:14.201392128";
        long parsedIso = ServicesTimestampLongConverter.INSTANCE.parse(isoTimestamp);
        System.out.println("Parsed ISO timestamp: " + parsedIso + " (" + formatTimestamp(parsedIso) + ")");

        // Example 2: A full numeric timestamp as seen before.
        String numericTimestamp = "201392128";
        long parsedNumeric = ServicesTimestampLongConverter.INSTANCE.parse(numericTimestamp);
        System.out.println("Parsed full numeric timestamp: " + parsedNumeric + " (" + formatTimestamp(parsedNumeric) + ")");

        // Example 3: A truncated numeric timestamp (simulating missing leading zeros).
        String truncatedTimestamp = "201392";
        long parsedTruncated = ServicesTimestampLongConverter.INSTANCE.parse(truncatedTimestamp);
        System.out.println("Parsed truncated numeric timestamp: " + parsedTruncated + " (" + formatTimestamp(parsedTruncated) + ")");
    }

    /**
     * Helper method to convert the parsed timestamp (in the configured time unit) to a human-readable date.
     * This example assumes that the configured unit is nanoseconds.
     */
    private static String formatTimestamp(long timestamp) {
        long millis = timestamp;
        if (ServicesTimestampLongConverter.timeUnit().equals(java.util.concurrent.TimeUnit.NANOSECONDS)) {
            millis = timestamp / 1_000_000;
        } else if (ServicesTimestampLongConverter.timeUnit().equals(java.util.concurrent.TimeUnit.MICROSECONDS)) {
            millis = timestamp / 1_000;
        }
        return new java.util.Date(millis).toString();
    }
}
