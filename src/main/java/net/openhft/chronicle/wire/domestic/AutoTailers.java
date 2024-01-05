/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private AutoTailers() {
    }

    /**
     * Represents a closeable {@link Runnable}. Combines the behavior of a {@link Runnable} with
     * the ability to be closed using {@link AutoCloseable}.
     */
    public interface CloseableRunnable extends Runnable, AutoCloseable {
        @Override
        void close();
    }

    /**
     * Represents an event handler that can be closed. Combines the behavior of an {@link EventHandler}
     * with the ability to be closed using {@link AutoCloseable}.
     */
    public interface CloseableEventHandler extends EventHandler, AutoCloseable {
        @Override
        void close();
    }

    /**
     * Replays the tailer content onto the provided excerpt listener.
     *
     * @param tailer          The {@link MarshallableIn} instance representing the tailer.
     * @param excerptListener Listener that consumes the excerpts from the tailer.
     * @return A long value resulting from the acceptance of the tailer by the listener.
     * @throws InvalidMarshallableException If the tailer contains invalid marshallable data.
     */
    public static long replayOnto(@NotNull final MarshallableIn tailer,
                                  @NotNull final ExcerptListener excerptListener) throws InvalidMarshallableException {
        requireNonNull(tailer);
        requireNonNull(excerptListener);

        return ReductionUtil.accept(tailer, excerptListener);
    }

    /**
     * Creates and returns a closeable runnable tailored for the provided tailer supplier and excerpt listener.
     *
     * @param tailerSupplier    Supplier providing instances of {@link MarshallableIn}.
     * @param excerptListener   Listener that consumes the excerpts from the tailer.
     * @param pauserSupplier    Supplier providing instances of {@link Pauser}.
     * @return A {@link CloseableRunnable} instance for polling the tailer and processing its excerpts.
     */
    @NotNull
    public static CloseableRunnable createRunnable(@NotNull final Supplier<? extends MarshallableIn> tailerSupplier,
                                                   @NotNull final ExcerptListener excerptListener,
                                                   @NotNull final Supplier<Pauser> pauserSupplier) {
        requireNonNull(tailerSupplier);
        requireNonNull(excerptListener);
        requireNonNull(pauserSupplier);

        return new InternalAutoTailers.RunnablePoller(tailerSupplier, excerptListener, pauserSupplier);
    }

    /**
     * Creates and returns a closeable event handler tailored for the provided tailer supplier and excerpt listener.
     *
     * @param tailerSupplier  Supplier providing instances of {@link MarshallableIn}.
     * @param excerptListener Listener that consumes the excerpts from the tailer.
     * @return A {@link CloseableEventHandler} instance for processing the tailer's excerpts.
     */
    @NotNull
    public static CloseableEventHandler createEventHandler(@NotNull final Supplier<? extends MarshallableIn> tailerSupplier,
                                                           @NotNull final ExcerptListener excerptListener) {
        requireNonNull(tailerSupplier);
        requireNonNull(excerptListener);

        return new InternalAutoTailers.EventHandlerPoller(tailerSupplier, excerptListener);
    }
}
