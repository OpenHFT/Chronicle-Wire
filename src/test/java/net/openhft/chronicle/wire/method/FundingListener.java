package net.openhft.chronicle.wire.method;



public interface FundingListener {
    void funding(Funding funding);
    void fundingPrimitive(int num);
    void fundingNoArg();
}
