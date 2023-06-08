package net.openhft.chronicle.wire.scoped;

public abstract class AbstractScopedResource<T> implements ScopedResource<T> {

    private final ScopedThreadLocal<T> scopedThreadLocal;

    protected AbstractScopedResource(ScopedThreadLocal<T> scopedThreadLocal) {
        this.scopedThreadLocal = scopedThreadLocal;
    }

    /**
     * Do anything that needs to be done before returning a resource to a caller
     */
    void preAcquire() {
        // Do nothing by default
    }

    @Override
    public void close() {
        scopedThreadLocal.returnResource(this);
    }
}
