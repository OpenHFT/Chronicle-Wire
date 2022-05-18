package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.core.time.SetTimeProvider;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class YamlTesterTest {
    @Before
    public void setUp() {
        SystemTimeProvider.CLOCK = new SetTimeProvider("2022-05-17T20:26:00")
                .autoIncrement(1, TimeUnit.MICROSECONDS);
    }

    @After
    public void tearDown() {
        SystemTimeProvider.CLOCK = SystemTimeProvider.INSTANCE;
    }

    @Test
    public void t1() {
        final YamlTester yt = YamlTester.runTest(TestImpl.class, "yaml-tester/t1");
        assertEquals(yt.expected(), yt.actual());
    }

    @Test
    public void t2() {
        final YamlTester yt = YamlTester.runTest(TestImpl::new, TestOut.class, "yaml-tester/t2");
        assertEquals(yt.expected(), yt.actual());
    }
}
