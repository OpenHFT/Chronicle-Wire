package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AbstractCommonMarshallableTest {

    @Test
    void doesNotUseSelfDescribingMessagesByDefault() {
        assertFalse(new AbstractCommonMarshallable() {
        }.usesSelfDescribingMessage());
    }
}