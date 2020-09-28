package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.core.annotation.DontChain;

import java.util.List;

// TODO to be removed before merge
public class TestClassesStorage {
    public interface MyInterface1 {
        void call1();

        void call2(String s);

        void call3(@LongConversion(MilliTimestampLongConverter.class) long l, @IntConversion(Base32IntConverter.class) int i);

        void call4(byte[] arr, double d, List<Boolean> list);

        @MethodId(value = 5)
        void call5(Object o);

        void call6(MyCustomType1 marshallable, MyCustomType2 type3, int x);

        DoNotChain call7();

        DoChain call8(MyCustomType1 marshallable);
    }

    public interface MyInterface2 extends MethodFilterOnFirstArg {
        void call9(boolean b, MyCustomType2 type2);
    }

    public interface DoChain {
        DoChain2 call10(boolean b);

        ClassesAreNotChained call14();
    }

    @DontChain
    public interface DoNotChain {
        void call11(int i);
    }

    public interface DoChain2 {
        void call12();
    }

    interface MyCustomInterface1 extends Marshallable {

    }

    public static class MyCustomType1 implements MyCustomInterface1 {
        int x;
        String s;
        MyCustomType2 b;

        public MyCustomType1() {
            this.x = 421;
            this.s = "few";
            this.b = new MyCustomType2();
            b.d = 0.321e-4;
        }
    }

    public static class MyCustomType2 {
        double d;
    }

    public static class ClassesAreNotChained {
        void call13() {
            System.out.println(1);
        }
    }
}
