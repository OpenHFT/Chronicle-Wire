package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.posix.internal.core.Jvm;
import org.junit.Test;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnsafeGetLongTest {

    @Test
    public void getLongWorking() {
        dumpClassLayout(Working.class);
        Working working = new Working();
        working.a = 10;
        assertEquals(10, UnsafeMemory.unsafeGetLong(working, 16));
    }

    /**
     * Expected to break on Mac M1/M2 only in JDK17+
     */
    @Test
    public void getLongBrokenOnMacJdk17Plus() {
        dumpClassLayout(Broken.class);
        Broken broken = new Broken();
        broken.a = 10;
        assertEquals(10, UnsafeMemory.unsafeGetLong(broken, 16));
    }

    private static void dumpClassLayout(Class<?> klass) {
        System.out.println("============= Class layout start =============");
        if (Jvm.isAzul()) {
            System.out.println("JOL is not supported on non Hotspot JVMs so no class layout can be dumped");
        } else {
            System.out.println("Class: " + klass.getSimpleName());
            System.out.println("JVM major version: " + net.openhft.chronicle.core.Jvm.majorVersion());
            System.out.println("VM details: " + VM.current().details());
            System.out.println("Class layout: ");
            System.out.println(ClassLayout.parseClass(klass).toPrintable());
        }
        System.out.println("============= Class layout end ===============");
    }

    private static final class Working {
        private long a;
    }

    private static final class Broken {
        private long a, b, c, d, e, f, g, h, i;
    }

}
