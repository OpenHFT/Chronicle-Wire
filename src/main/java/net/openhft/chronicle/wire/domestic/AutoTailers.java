//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire.domestic;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.threads.EventHandler;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.wire.ExcerptListener;
import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.internal.InternalAutoTailers;
import net.openhft.chronicle.wire.internal.reduction.ReductionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Utility class that provides a set of functions and interfaces related to auto-tailers.
 * Auto-tailers are utilities for automatically "tailing" or tracking the end of a data source.
 * This class is designed to support operations on tailers, especially those that deal with Marshallable data.
 * <p>
 * The {@code AutoTailers} class provides static methods and inner interfaces to aid in the creation
 * and management of tailers and related event handlers.
 */
public final class AutoTailers {

    // Suppresses default constructor, ensuring non-instantiability.
    private AutoTailers() {
    }

    public interface CloseableRunnable extends Runnable, AutoCloseable {
        @Override
        void close();
    }

    public interface CloseableEventHandler extends EventHandler, AutoCloseable {
        @Override
        void close();
    }

    public static long replayOnto(@NotNull final MarshallableIn tailer,
                                  @NotNull final ExcerptListener excerptListener) throws InvalidMarshallableException {
        requireNonNull(tailer);
        requireNonNull(excerptListener);

        return ReductionUtil.accept(tailer, excerptListener);
    }

    @NotNull
    public static CloseableRunnable createRunnable(@NotNull final Supplier<? extends MarshallableIn> tailerSupplier,
                                                   @NotNull final ExcerptListener excerptListener,
                                                   @NotNull final Supplier<Pauser> pauserSupplier) {
        requireNonNull(tailerSupplier);
        requireNonNull(excerptListener);
        requireNonNull(pauserSupplier);

        return new InternalAutoTailers.RunnablePoller(tailerSupplier, excerptListener, pauserSupplier);
    }

    @NotNull
    public static CloseableEventHandler createEventHandler(@NotNull final Supplier<? extends MarshallableIn> tailerSupplier,
                                                           @NotNull final ExcerptListener excerptListener) {
        requireNonNull(tailerSupplier);
        requireNonNull(excerptListener);

        return new InternalAutoTailers.EventHandlerPoller(tailerSupplier, excerptListener);
    }
}
