package net.openhft.chronicle.wire;

class WMTwoFields extends SelfDescribingMarshallable {
    @LongConversion(WordsLongConverter.class)
    int id;
    @LongConversion(MicroTimestampLongConverter.class)
    long ts;
}
