package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.WireKey;

/**
 * Created by Rob Austin
 */
public enum CoreFields implements WireKey {
    tid,
    csp,
    cid,
    reply,
}
