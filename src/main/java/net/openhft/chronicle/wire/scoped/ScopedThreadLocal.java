package net.openhft.chronicle.wire.scoped;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.threads.CleaningThreadLocal;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A scoped thread-local resource.
 * <p>
 * This is used for small, tightly-scoped thread-local resource "pools", a safer alternative
 * to a thread-local singleton.
 * <p>
 * Example of use:
 * <pre>{@code
 *   try (ScopedResource<Wire> sharedWire = sharedWireScopedThreadLocal.get()) {
 *     Wire wire = sharedWire.get();
 *     // ... do something with the wire
 *   } // it is returned for use by inner scopes here
 * }</pre>
 *
 * @param <T>
 */
public class ScopedThreadLocal<T> {

    private final Supplier<T> supplier;
    private final Consumer<T> onAcquire;
    private final CleaningThreadLocal<SimpleStack> instancesTL;
    private final boolean useWeakReferences;

    /**
     * Constructor
     *
     * @param supplier     The supplier of new instances
     * @param maxInstances The maximum number of instances that will be retained for re-use
     */
    public ScopedThreadLocal(Supplier<T> supplier, int maxInstances) {
        this(supplier, ScopedThreadLocal::noOp, maxInstances);
    }

    /**
     * Constructor
     *
     * @param supplier     The supplier of new instances
     * @param onAcquire    A function to run on each instance upon it's acquisition
     * @param maxInstances The maximum number of instances that will be retained for re-use
     */
    public ScopedThreadLocal(@NotNull Supplier<T> supplier, @NotNull Consumer<T> onAcquire, int maxInstances) {
        this(supplier, onAcquire, maxInstances, false);
    }

    /**
     * Constructor
     *
     * @param supplier          The supplier of new instances
     * @param onAcquire         A function to run on each instance upon it's acquisition
     * @param maxInstances      The maximum number of instances that will be retained for re-use
     * @param useWeakReferences Whether to allow resources to be garbage collected when they're not in use
     */
    public ScopedThreadLocal(@NotNull Supplier<T> supplier, @NotNull Consumer<T> onAcquire, int maxInstances, boolean useWeakReferences) {
        this.supplier = supplier;
        this.onAcquire = onAcquire;
        this.instancesTL = CleaningThreadLocal.withCloseQuietly(() -> new SimpleStack(maxInstances));
        this.useWeakReferences = useWeakReferences;
    }

    /**
     * Get a scoped instance of the shared resource
     *
     * @return the {@link ScopedResource}, to be closed once it is finished being used
     */
    public ScopedResource<T> get() {
        final SimpleStack scopedThreadLocalResources = instancesTL.get();
        AbstractScopedResource<T> instance;
        if (scopedThreadLocalResources.isEmpty()) {
            instance = createNewResource();
        } else {
            instance = scopedThreadLocalResources.pop();
        }
        instance.preAcquire();
        onAcquire.accept(instance.get());
        return instance;
    }

    private AbstractScopedResource<T> createNewResource() {
        if (useWeakReferences)
            return new WeakReferenceScopedResource<>(this, supplier);
        else
            return new StrongReferenceScopedResource<>(this, supplier.get());
    }

    /**
     * Return a {@link ScopedResource} to the "pool"
     *
     * @param scopedResource The resource to return
     */
    void returnResource(AbstractScopedResource<T> scopedResource) {
        final SimpleStack scopedThreadLocalResources = instancesTL.get();
        scopedThreadLocalResources.push(scopedResource);
    }

    /**
     * The default onAcquire function
     */
    private static <T> void noOp(T instance) {
    }

    /**
     * A simple array-based stack for managing retained {@link ScopedResource}s
     */
    class SimpleStack extends AbstractCloseable {

        private final AbstractScopedResource<T>[] instances;
        private int headIndex = -1;

        SimpleStack(int maxInstances) {
            this.instances = (AbstractScopedResource<T>[]) Array.newInstance(AbstractScopedResource.class, maxInstances);
        }

        AbstractScopedResource<T> pop() {
            if (headIndex == -1) {
                throw new IllegalStateException("Can't pop an empty stack");
            }
            final AbstractScopedResource<T> instance = instances[headIndex];
            instances[headIndex] = null;
            --headIndex;
            return instance;
        }

        void push(AbstractScopedResource<T> instance) {
            if (headIndex < instances.length - 1) {
                instances[++headIndex] = instance;
            } else {
                Jvm.warn().on(ScopedThreadLocal.class, "Pool capacity exceeded, consider increasing maxInstances, maxInstances=" + instances.length);
            }
        }

        boolean isEmpty() {
            return headIndex == -1;
        }

        @Override
        protected void performClose() throws IllegalStateException {
            for (int i = 0; i < instances.length; i++) {
                if (instances[i] != null) {
                    instances[i].closeResource();
                    instances[i] = null;
                }
            }
        }
    }
}
