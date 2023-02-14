package net.openhft.chronicle.wire;

import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class AbstractTimestampLongConverterPerfTest implements JLBHTask {
    static {
        System.setProperty("jvm.resource.tracing", "false");
    }

    private final LongConverter converter = NanoTimestampLongConverter.INSTANCE;
    private final StringBuilder sb = new StringBuilder();
    private JLBH jlbh;

    @Test
    public void test() {
        JLBHOptions jlbhOptions = new JLBHOptions()
                .iterations(100_100)
                .runs(5)
                .recordOSJitter(false)
                .accountForCoordinatedOmission(false)
                .warmUpIterations(10_000)
                .jlbhTask(this);
        JLBH jlbh = new JLBH(jlbhOptions);
        jlbh.start();
    }

    @Override
    public void init(JLBH jlbh) {
        this.jlbh = jlbh;
    }

    /* Following code throws, catches and swallows exception on every invocation.
    Set a breakpoint in UnsupportedTemporalTypeException constructor.
    java.time.temporal.UnsupportedTemporalTypeException: Unsupported field: OffsetSeconds
	at java.time.LocalDate.get0(LocalDate.java:680)
	at java.time.LocalDate.getLong(LocalDate.java:659)
	at java.time.LocalDateTime.getLong(LocalDateTime.java:720)
	at java.time.format.DateTimePrintContext.getValue(DateTimePrintContext.java:298)
	at java.time.format.DateTimeFormatterBuilder$OffsetIdPrinterParser.format(DateTimeFormatterBuilder.java:3346)
	at java.time.format.DateTimeFormatterBuilder$CompositePrinterParser.format(DateTimeFormatterBuilder.java:2190)
	at java.time.format.DateTimeFormatterBuilder$CompositePrinterParser.format(DateTimeFormatterBuilder.java:2190)
	at java.time.format.DateTimeFormatter.formatTo(DateTimeFormatter.java:1746)
	at net.openhft.chronicle.wire.AbstractTimestampLongConverter.append(AbstractTimestampLongConverter.java:128)
	at net.openhft.chronicle.wire.AbstractTimestampLongConverter.append(AbstractTimestampLongConverter.java:137)
	at net.openhft.chronicle.wire.AbstractTimestampLongConverterPerfTest$Task.run(AbstractTimestampLongConverterPerfTest.java:45)
	at net.openhft.chronicle.jlbh.JLBH.warmup(JLBH.java:286)
	at net.openhft.chronicle.jlbh.JLBH.start(JLBH.java:165)
	at net.openhft.chronicle.wire.AbstractTimestampLongConverterPerfTest.test(AbstractTimestampLongConverterPerfTest.java:25)
     */
    @Override
    public void run(long startTimeNS) {
        sb.setLength(0);
        converter.append(sb, 1675177572812000000L);
        jlbh.sampleNanos(System.nanoTime() - startTimeNS);
    }
}
