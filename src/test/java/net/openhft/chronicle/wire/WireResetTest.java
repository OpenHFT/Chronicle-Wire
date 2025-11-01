/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.Closeable;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class WireResetTest extends WireTestCommon {
    @Before
    public void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @Test
    //https://github.com/OpenHFT/Chronicle-Wire/issues/225
    public void test() {
        Event event = new Event();
        assertFalse(event.isClosed());

        event.reset();
        assertFalse(event.isClosed());
    }

    @Test
    //https://github.com/OpenHFT/Chronicle-Wire/issues/225
    public void testEventAbstractCloseable() {
        try (EventAbstractCloseable event = new EventAbstractCloseable()) {
            assertFalse(event.isClosed());

            event.reset();
            assertFalse(event.isClosed());
        }
    }

    @Test
    //https://github.com/OpenHFT/Chronicle-Wire/issues/732
    public void testDeepReset() {
        Event event1 = new Event();
        Identifier identifier1 = event1.identifier;
        event1.identifier.id = "id";
        event1.identifier.parent = new Identifier("parent_id1");
        event1.identifier.permissions.put("uid1", "r");
        event1.ids.add(new Identifier("id1_2"));
        event1.payload = "payload1";
        event1.close();

        event1.reset();

        assertFalse(event1.isClosed());
        assertSame(identifier1, event1.identifier);
        assertNull(event1.identifier.id);
        assertTrue(event1.identifier.permissions.isEmpty());
        assertNull(event1.identifier.parent);
        assertTrue(event1.ids.isEmpty());
        assertNull(event1.payload);

        Event event2 = new Event();
        Identifier identifier2 = event2.identifier;

        event2.reset();

        assertSame(identifier2, event2.identifier);
        assertNull(event2.identifier.parent);

        event2.identifier.id = "id2";
        event2.identifier.parent = new Identifier();
        event2.identifier.permissions.put("uid2", "rw");
        event2.ids.add(new Identifier("id2_2"));
        event2.payload = "payload2";

        assertFalse(event1.isClosed());
        assertSame(identifier1, event1.identifier);
        assertNull(event1.identifier.id);
        assertTrue(event1.identifier.permissions.isEmpty());
        assertNull(event1.identifier.parent);
        assertTrue(event1.ids.isEmpty());
        assertNull(event1.payload);

    }

    /**
     * Reproduction of <a href="https://github.com/OpenHFT/Chronicle-Wire/issues/745">this issue</a>
     */
    @Test
    public void canDeepResetOnDtosContainingLocalDates() {
        Event e = new Event();
        e.someDate = LocalDate.now();
        e.reset();
    }

    public static class Event extends SelfDescribingMarshallable implements Closeable {

        private boolean isClosed;

        Identifier identifier = new Identifier();
        Collection<Identifier> ids = new LinkedList<>();
        String payload;
        LocalDate someDate;

        @Override
        public void close() {
            isClosed = true;
        }
        @Override
        public boolean isClosed() {
            return isClosed;
        }
    }

    public static class EventAbstractCloseable extends AbstractCloseable implements Marshallable {
        @Override
        protected void performClose() {
        }
    }

    static class Identifier extends SelfDescribingMarshallable {
        String id;
        Identifier parent;
        Map<String, String> permissions = new HashMap<>();

        Identifier() {
        }

        Identifier(String id) {
            this.id = id;
        }
    }
}
