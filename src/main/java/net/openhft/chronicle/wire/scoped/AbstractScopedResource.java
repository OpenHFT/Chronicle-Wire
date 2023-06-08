package net.openhft.chronicle.wire.scoped;

abstract class AbstractScopedResource<T> implements ScopedResource<T> {

    private final ScopedThreadLocal<T> scopedThreadLocal;

    protected AbstractScopedResource(ScopedThreadLocal<T> scopedThreadLocal) {
        this.scopedThreadLocal = scopedThreadLocal;
    }

    @Override
    public void close() {
        scopedThreadLocal.returnResource(this);
    }

    /**
     * Do anything that needs to be done before returning a resource to a caller
     */
    void preAcquire() {
        // Do nothing by default
    }

    /**
     * Close the contained resource and clear any references
     */
    abstract void closeResource();
}
