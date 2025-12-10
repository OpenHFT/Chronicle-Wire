package net.openhft.chronicle.wire;

import static org.junit.Assert.assertEquals;

final class WireCharacterTestSupport {
    private WireCharacterTestSupport() {
    }

    static void assertCharacterRoundTrip(Wire wire, boolean clearBetween) {
        for (char ch : new char[]{0, '!', 'a', Character.MAX_VALUE}) {
            if (clearBetween) {
                wire.clear();
            }
            wire.write().object(ch);
            char ch2 = wire.read().object(char.class);
            assertEquals(ch, ch2);
        }
    }
}
