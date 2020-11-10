package net.openhft.chronicle.wire;

class WMTwoFields extends SelfDescribingMarshallable {
    @IntConversion(WordsIntConverter.class)
    int id;
    @LongConversion(MicroTimestampLongConverter.class)
    long ts;
}
