package net.openhft.chronicle.wire.scoped;

/**
 * A {@link ScopedResource} that will always retain a strong reference to the
 * contained resource, even when not "in use"
 *
 * @param <T> The type of the contained resource
 */
public class StrongReferenceScopedResource<T> extends AbstractScopedResource<T> {

    private final T resource;

    StrongReferenceScopedResource(ScopedThreadLocal<T> scopedThreadLocal, T resource) {
        super(scopedThreadLocal);
        this.resource = resource;
    }

    public T get() {
        return resource;
    }
}

