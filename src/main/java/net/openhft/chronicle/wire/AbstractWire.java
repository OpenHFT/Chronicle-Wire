package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.util.UTF8StringInterner;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.threads.BusyPauser;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;

/**
 * Created by peter on 10/03/16.
 */
public abstract class AbstractWire implements Wire, InternalWire {
    protected static final UTF8StringInterner UTF8_INTERNER = new UTF8StringInterner(512);
    protected final Bytes<?> bytes;
    protected final boolean use8bit;
    protected Pauser pauser = BusyPauser.INSTANCE;
    protected ClassLookup classLookup = ClassAliasPool.CLASS_ALIASES;

    public AbstractWire(Bytes bytes, boolean use8bit) {
        this.bytes = bytes;
        this.use8bit = use8bit;
    }

    @Override
    public Pauser pauser() {
        return pauser;
    }

    @Override
    public void pauser(Pauser pauser) {
        this.pauser = pauser;
    }

    @Override
    public void clear() {
        bytes.clear();
    }

    @Override
    public void classLookup(ClassLookup classLookup) {
        this.classLookup = classLookup;
    }

    @Override
    public ClassLookup classLookup() {
        return classLookup;
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        return bytes;
    }

    @Override
    public boolean hasMore() {
        consumePadding();
        return bytes.readRemaining() > 0;
    }
}
