package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.EnumInterner;

enum CcyPair {
    EURUSD, GBPUSD, EURCHF;

    static final EnumInterner<CcyPair> INTERNER = new EnumInterner<>(CcyPair.class);
}
