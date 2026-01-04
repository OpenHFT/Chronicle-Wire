/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.Closeable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class WireResetTest extends WireTestCommon {
    @BeforeEach
    void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for reset tests");
    }

    @Test
    @DisplayName("Reset keeps event open after reset")
    //https://github.com/OpenHFT/Chronicle-Wire/issues/225
    void test() {
        Event event = new Event();
        assertFalse(event.isClosed(), "event should not be closed after initialization");

        event.reset();
        assertFalse(event.isClosed(), "event should remain open after reset operation");
    }

    @Test
    @DisplayName("Reset keeps AbstractCloseable event open after reset")
    //https://github.com/OpenHFT/Chronicle-Wire/issues/225
    void testEventAbstractCloseable() {
        try (EventAbstractCloseable event = new EventAbstractCloseable()) {
            assertFalse(event.isClosed(), "abstract closeable event should not be closed after initialization");

            event.reset();
            assertFalse(event.isClosed(), "abstract closeable event should remain open after reset operation");
        }
    }

    @Test
    @DisplayName("Deep reset clears nested fields safely")
    //https://github.com/OpenHFT/Chronicle-Wire/issues/732
    void testDeepReset() {
        Event event1 = new Event();
        final Identifier identifier1 = event1.identifier;
        event1.identifier.id = "id";
        event1.identifier.parent = new Identifier("parent_id1");
        event1.identifier.permissions.put("uid1", "r");
        event1.ids.add(new Identifier("id1_2"));
        event1.payload = "payload1";
        event1.close();

        event1.reset();

        assertFalse(event1.isClosed(), "event should not be closed after reset following explicit close");
        assertSame(identifier1, event1.identifier, "identifier instance should remain same object after deep reset");
        assertNull(event1.identifier.id, "identifier id should be null after deep reset");
        assertTrue(event1.identifier.permissions.isEmpty(), "identifier permissions map should be empty after deep reset");
        assertNull(event1.identifier.parent, "identifier parent should be null after deep reset");
        assertTrue(event1.ids.isEmpty(), "ids collection should be empty after deep reset");
        assertNull(event1.payload, "payload should be null after deep reset");

        Event event2 = new Event();
        Identifier identifier2 = event2.identifier;

        event2.reset();

        assertSame(identifier2, event2.identifier, "identifier instance should remain same object after reset on fresh event");
        assertNull(event2.identifier.parent, "identifier parent should be null after reset on fresh event");

        event2.identifier.id = "id2";
        event2.identifier.parent = new Identifier();
        event2.identifier.permissions.put("uid2", "rw");
        event2.ids.add(new Identifier("id2_2"));
        event2.payload = "payload2";

        assertFalse(event1.isClosed(), "first event should remain unclosed after populating second event");
        assertSame(identifier1, event1.identifier, "first event identifier should remain same instance after populating second event");
        assertNull(event1.identifier.id, "first event identifier id should remain null after populating second event");
        assertTrue(event1.identifier.permissions.isEmpty(), "first event permissions should remain empty after populating second event");
        assertNull(event1.identifier.parent, "first event parent should remain null after populating second event");
        assertTrue(event1.ids.isEmpty(), "first event ids collection should remain empty after populating second event");
        assertNull(event1.payload, "first event payload should remain null after populating second event");

    }

    /**
     * Reproduction of issue 745 with LocalDate fields.
     */
    @Test
    @DisplayName("Deep reset clears LocalDate fields safely")
    void canDeepResetOnDtosContainingLocalDates() {
        Event e = new Event();
        e.someDate = LocalDate.now();
        e.reset();
        assertNull(e.someDate, "LocalDate field should be null after deep reset operation");
    }

    public static class Event extends SelfDescribingMarshallable implements Closeable {

        final Identifier identifier = new Identifier();
        final Collection<Identifier> ids = new LinkedList<>();
        String payload;
        LocalDate someDate;
        private boolean isClosed;

        @Override
        public void close() {
            isClosed = true;
        }

        @Override
        public boolean isClosed() {
            return isClosed;
        }
    }

    static class EventAbstractCloseable extends AbstractCloseable implements Marshallable {
        @Override
        protected void performClose() {
        }
    }

    static class Identifier extends SelfDescribingMarshallable {
        String id;
        Identifier parent;
        final Map<String, String> permissions = new HashMap<>();

        Identifier() {
        }

        Identifier(String id) {
            this.id = id;
        }
    }
}
