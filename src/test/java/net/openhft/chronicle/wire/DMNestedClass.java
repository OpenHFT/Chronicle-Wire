package net.openhft.chronicle.wire;

class DMNestedClass extends SelfDescribingMarshallable {
    String str;
    int num;

    public DMNestedClass(String str, int num) {
        this.str = str;
        this.num = num;
    }
}
