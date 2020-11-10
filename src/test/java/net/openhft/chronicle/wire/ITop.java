package net.openhft.chronicle.wire;

interface ITop {
    IMid mid(String name);

    IMid2 mid2(String name);

    IMid midNoArg();

    IMid midTwoArgs(int i, long l);
}
