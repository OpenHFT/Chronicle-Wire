/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.threads.EventGroup;
import net.openhft.chronicle.threads.EventGroupBuilder;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.threads.PauserMode;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Test class to validate the behavior of the marshalling process within an EventGroup context.
 */
class MarshallingEventGroupTest extends WireTestCommon {

    /**
     * Test method to evaluate the serialization of the EventGroup object.
     */
    @Test
    void test() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Skip this test if JVM is running in debug mode, as it could lead to longer timeouts
        assumeFalse(Jvm.isDebug());

        // Using the EventGroupBuilder, create an instance of EventGroup and use try-with-resources to ensure it's closed
        try (final EventGroup eg =
                     EventGroupBuilder.builder()
                             .withName("test")
                             .withBinding("none")
                             .withPauser(Pauser.sleepy())
                             .withConcurrentPauserSupplier(PauserMode.sleepy)
                             .withBlockingPauserSupplier(PauserMode.sleepy)
                             .withConcurrentThreadsNum(1)
                             .build()) {

            // Convert the EventGroup object to its TEXT representation
            final String actual = WireType.TEXT.asString(eg);

            // An updated expected serialized string for the EventGroup object
            String expected = "" +
                    "!net.openhft.chronicle.threads.EventGroup {\n" +
                    "  referenceId: 0,\n" +
                    "  lifecycle: !net.openhft.chronicle.threads.EventLoopLifecycle NEW,\n" +
                    "  name: test,\n" +
                    "  privateGroup: false,\n" +
                    "  counter: 0,\n" +
                    "  monitor: !net.openhft.chronicle.threads.MonitorEventLoop {\n" +
                    "    referenceId: 0,\n" +
                    "    lifecycle: !net.openhft.chronicle.threads.EventLoopLifecycle NEW,\n" +
                    "    name: test/~monitortest/event~loop~monitor,\n" +
                    "    privateGroup: false,\n" +
                    "    handlers: [\n" +
                    "      !net.openhft.chronicle.threads.MonitorEventLoop$IdempotentLoopStartedEventHandler { handler: NOOP_PAUSER_MONITOR, loopStarted: false }\n" +
                    "    ],\n" +
                    "    pauser: !net.openhft.chronicle.threads.MilliPauser { pausing: false, pauseTimeMS: 10, timePaused: 0, countPaused: 0, pauseUntilMS: 0 }\n" +
                    "  },\n" +
                    "  core: !net.openhft.chronicle.threads.VanillaEventLoop {\n" +
                    "    referenceId: 0,\n" +
                    "    lifecycle: !net.openhft.chronicle.threads.EventLoopLifecycle NEW,\n" +
                    "    name: test/core-event-loop,\n" +
                    "    privateGroup: false,\n" +
                    "    mediumHandlers: [    ],\n" +
                    "    newHandlers: [    ],\n" +
                    "    pauser: !net.openhft.chronicle.threads.LongPauser { minPauseTimeNS: 500000, maxPauseTimeNS: 20000000, pausing: false, minBusyNS: 0, minYieldNS: 50000, firstPauseNS: 9223372036854775807, pauseTimeNS: 500000, timePaused: 0, countPaused: 0, yieldStart: 0, pauseUntilNS: 0 },\n" +
                    "    daemon: true,\n" +
                    "    binding: none,\n" +
                    "    mediumHandlersArray: [ ],\n" +
                    "    highHandler: !net.openhft.chronicle.threads.EventHandlers NOOP,\n" +
                    "    loopStartNS: 9223372036854775807,\n" +
                    "    thread: !!null \"\",\n" +
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
                    "    name: test/blocking-event-loop,\n" +
                    "    privateGroup: false,\n" +
                    "    handlers: [    ],\n" +
                    "    runners: [    ],\n" +
                    "    threadFactory: test/blocking-event-loop,\n" +
                    "    pauserSupplier: !net.openhft.chronicle.threads.PauserMode sleepy\n" +
                    "  },\n" +
                    "  pauser: !net.openhft.chronicle.threads.LongPauser {\n" +
                    "    minPauseTimeNS: 500000,\n" +
                    "    maxPauseTimeNS: 20000000,\n" +
                    "    pausing: false,\n" +
                    "    minBusyNS: 0,\n" +
                    "    minYieldNS: 50000,\n" +
                    "    firstPauseNS: 9223372036854775807,\n" +
                    "    pauseTimeNS: 500000,\n" +
                    "    timePaused: 0,\n" +
                    "    countPaused: 0,\n" +
                    "    yieldStart: 0,\n" +
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
                    "  daemon: true,\n" +
                    "  replicationPauser: !!null \"\",\n" +
                    "  replication: !!null \"\"\n" +
                    "}\n";

            // Assert that the actual serialized string matches the updated expected string
            assertEquals(expected, actual);
        }
    }
}
