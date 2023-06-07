package net.openhft.chronicle.wire.scoped;

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ScopedThreadLocal<T> {

    private final Supplier<T> supplier;
    private final Consumer<T> onAcquire;
    private final ThreadLocal<SimpleStack> instancesTL;

    private static <T> void noOp(T instance) {
    }

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
        this.supplier = supplier;
        this.onAcquire = onAcquire;
        this.instancesTL = ThreadLocal.withInitial(() -> new SimpleStack(maxInstances));
    }

    public ScopedResource<T> get() {
        final SimpleStack scopedThreadLocalResources = instancesTL.get();
        ScopedResource<T> instance;
        if (scopedThreadLocalResources.isEmpty()) {
            instance = new ScopedResource<>(this, supplier.get());
        } else {
            instance = scopedThreadLocalResources.pop();
        }
        onAcquire.accept(instance.get());
        return instance;
    }

    void returnInstance(ScopedResource<T> scopedResource) {
        final SimpleStack scopedThreadLocalResources = instancesTL.get();
        scopedThreadLocalResources.push(scopedResource);
    }

    class SimpleStack {
        private final ScopedResource<T>[] instances;
        private int headIndex = -1;

        SimpleStack(int maxInstances) {
            this.instances = (ScopedResource<T>[]) Array.newInstance(ScopedResource.class, maxInstances);
        }

        ScopedResource<T> pop() {
            if (headIndex == -1) {
                throw new IllegalStateException("Can't pop an empty stack");
            }
            final ScopedResource<T> instance = instances[headIndex];
            instances[headIndex] = null;
            --headIndex;
            return instance;
        }

        void push(ScopedResource<T> instance) {
            if (headIndex < instances.length - 1) {
                instances[++headIndex] = instance;
            } else {
                Jvm.warn().on(ScopedThreadLocal.class, "Pool capacity exceeded, consider increasing maxInstances, maxInstances=" + instances.length);
            }
        }

        boolean isEmpty() {
            return headIndex == -1;
        }
    }
}
