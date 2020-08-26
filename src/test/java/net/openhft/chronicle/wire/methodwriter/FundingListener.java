package net.openhft.chronicle.wire.methodwriter;



public interface FundingListener {
    void funding(Funding funding);
    void fundingPrimitive(int num);
    void fundingNoArg();
}
