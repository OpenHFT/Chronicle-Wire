package net.openhft.chronicle.wire;

final class MyTypesCustomTestSupport {
    private MyTypesCustomTestSupport() {
    }

    static MyTypesCustom createA() {
        MyTypesCustom mtA = new MyTypesCustom();
        mtA.flag = true;
        mtA.d = 123.456;
        mtA.i = -12345789;
        mtA.s = (short) 12345;
        mtA.text.append("Hello World");
        return mtA;
    }

    static MyTypesCustom createB() {
        MyTypesCustom mtB = new MyTypesCustom();
        mtB.flag = false;
        mtB.d = 123.4567;
        mtB.i = -123457890;
        mtB.s = (short) 1234;
        mtB.text.append("Bye now");
        return mtB;
    }
}
