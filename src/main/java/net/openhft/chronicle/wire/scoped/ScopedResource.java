package net.openhft.chronicle.wire.scoped;

import java.io.Closeable;

public class ScopedResource<T> implements Closeable {

    private final ScopedThreadLocal<T> scopedThreadLocal;
    private final T resource;

    ScopedResource(ScopedThreadLocal<T> scopedThreadLocal, T resource) {
        this.scopedThreadLocal = scopedThreadLocal;
        this.resource = resource;
    }

    public T get() {
        return resource;
    }

    @Override
    public void close() {
        scopedThreadLocal.returnInstance(this);
    }
}

