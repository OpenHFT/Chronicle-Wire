package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.threads.EventGroup;
import net.openhft.chronicle.threads.EventGroupBuilder;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.threads.PauserMode;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MarshallingEventGroupTest extends WireTestCommon {
    @Test
    public void test() {
        try (final EventGroup eg =
                     EventGroupBuilder.builder()
                             .withName("test")
                             .withBinding("none")
                             .withPauser(Pauser.sleepy())
                             .withConcurrentPauserSupplier(PauserMode.sleepy)
                             .withBlockingPauserSupplier(PauserMode.sleepy)
                             .withConcurrentThreadsNum(1)
                             .build()) {
            final String actual = WireType.TEXT.asString(eg);
//            System.out.println(actual);
            assertEquals("" +
                    "!net.openhft.chronicle.threads.EventGroup {\n" +
                    "  referenceId: 0,\n" +
                    "  lifecycle: !net.openhft.chronicle.threads.EventLoopLifecycle NEW,\n" +
                    "  name: test,\n" +
                    "  counter: 0,\n" +
                    "  monitor: !net.openhft.chronicle.threads.MonitorEventLoop {\n" +
                    "    referenceId: 0,\n" +
                    "    lifecycle: !net.openhft.chronicle.threads.EventLoopLifecycle NEW,\n" +
                    "    name: test~monitortest/event~loop~monitor,\n" +
                    "    handlers: [\n" +
                    "      !net.openhft.chronicle.threads.MonitorEventLoop$IdempotentLoopStartedEventHandler { referenceId: 0, loopStarted: false }\n" +
                    "    ],\n" +
                    "    pauser: !net.openhft.chronicle.threads.MilliPauser { pausing: false, pauseTimeMS: 10, timePaused: 0, countPaused: 0, pauseUntilMS: 0 }\n" +
                    "  },\n" +
                    "  core: !net.openhft.chronicle.threads.VanillaEventLoop {\n" +
                    "    referenceId: 0,\n" +
                    "    lifecycle: !net.openhft.chronicle.threads.EventLoopLifecycle NEW,\n" +
                    "    name: testcore-event-loop,\n" +
                    "    mediumHandlers: [    ],\n" +
                    "    newHandler: !!null \"\",\n" +
                    "    pauser: !net.openhft.chronicle.threads.LongPauser { minPauseTimeNS: 100000, maxPauseTimeNS: 20000000, pausing: false, minBusy: 0, minCount: 100, count: 0, pauseTimeNS: 100000, timePaused: 0, countPaused: 0, thread: !!null \"\", yieldStart: 0, timeOutStart: 9223372036854775807, pauseUntilNS: 0 },\n" +
                    "    daemon: true,\n" +
                    "    binding: none,\n" +
                    "    mediumHandlersArray: [ ],\n" +
                    "    highHandler: !net.openhft.chronicle.threads.EventHandlers NOOP,\n" +
                    "    loopStartNS: 9223372036854775807,\n" +
                    "    thread: !!null \"\",\n" +
                    "    exceptionThrownByHandler: !net.openhft.chronicle.threads.ExceptionHandlerStrategy$LogDontRemove { },\n" +
                    "    timerHandlers: [    ],\n" +
                    "    daemonHandlers: [    ],\n" +
                    "    timerIntervalMS: 1,\n" +
                    "    priorities: [\n" +
                    "      HIGH,\n" +
                    "      MEDIUM,\n" +
                    "      TIMER,\n" +
                    "      DAEMON,\n" +
                    "      MONITOR,\n" +
                    "      BLOCKING,\n" +
                    "      REPLICATION,\n" +
                    "      REPLICATION_TIMER,\n" +
                    "      CONCURRENT\n" +
                    "    ]\n" +
                    "  },\n" +
                    "  blocking: !net.openhft.chronicle.threads.BlockingEventLoop {\n" +
                    "    referenceId: 0,\n" +
                    "    lifecycle: !net.openhft.chronicle.threads.EventLoopLifecycle NEW,\n" +
                    "    name: testblocking-event-loop,\n" +
                    "    handlers: [    ],\n" +
                    "    threadFactory: testblocking-event-loop,\n" +
                    "    pauser: !net.openhft.chronicle.threads.LongPauser { minPauseTimeNS: 100000, maxPauseTimeNS: 20000000, pausing: false, minBusy: 0, minCount: 100, count: 0, pauseTimeNS: 100000, timePaused: 0, countPaused: 0, thread: !!null \"\", yieldStart: 0, timeOutStart: 9223372036854775807, pauseUntilNS: 0 }\n" +
                    "  },\n" +
                    "  pauser: !net.openhft.chronicle.threads.LongPauser {\n" +
                    "    minPauseTimeNS: 100000,\n" +
                    "    maxPauseTimeNS: 20000000,\n" +
                    "    pausing: false,\n" +
                    "    minBusy: 0,\n" +
                    "    minCount: 100,\n" +
                    "    count: 0,\n" +
                    "    pauseTimeNS: 100000,\n" +
                    "    timePaused: 0,\n" +
                    "    countPaused: 0,\n" +
                    "    thread: !!null \"\",\n" +
                    "    yieldStart: 0,\n" +
                    "    timeOutStart: 9223372036854775807,\n" +
                    "    pauseUntilNS: 0\n" +
                    "  },\n" +
                    "  concPauserSupplier: !net.openhft.chronicle.threads.PauserMode sleepy,\n" +
                    "  concBinding: none,\n" +
                    "  bindingReplication: none,\n" +
                    "  priorities: [\n" +
                    "    HIGH,\n" +
                    "    MEDIUM,\n" +
                    "    TIMER,\n" +
                    "    DAEMON,\n" +
                    "    MONITOR,\n" +
                    "    BLOCKING,\n" +
                    "    REPLICATION,\n" +
                    "    REPLICATION_TIMER,\n" +
                    "    CONCURRENT\n" +
                    "  ],\n" +
                    "  concThreads: [\n" +
                    "    !!null \"\"\n" +
                    "  ],\n" +
                    "  milliPauser: !net.openhft.chronicle.threads.MilliPauser {\n" +
                    "    pausing: false,\n" +
                    "    pauseTimeMS: 50,\n" +
                    "    timePaused: 0,\n" +
                    "    countPaused: 0,\n" +
                    "    pauseUntilMS: 0\n" +
                    "  },\n" +
                    "  daemon: true,\n" +
                    "  replicationPauser: !!null \"\",\n" +
                    "  blockingPauserSupplier: !net.openhft.chronicle.threads.PauserMode sleepy,\n" +
                    "  replication: !!null \"\"\n" +
                    "}\n", actual);
        }
    }
}
