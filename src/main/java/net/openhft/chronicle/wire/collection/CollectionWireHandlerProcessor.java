/*
 * Copyright 2014 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.collection;

/**
 * Created by Rob Austin
 */

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StreamCorruptedException;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.openhft.chronicle.wire.Wires.acquireStringBuilder;


/**
 * @author Rob Austin.
 */
public class CollectionWireHandlerProcessor<U, C extends Collection<U>> implements
        CollectionWireHandler<U, C>,
        Consumer<WireHandlers> {

    private Function<ValueIn, U> fromWire;
    private BiConsumer<ValueOut, U> toWire;

    private static final Logger LOG = LoggerFactory.getLogger(CollectionWireHandlerProcessor.class);

    public static final int SIZE_OF_SIZE = 4;

    private Wire inWire = null;
    private Wire outWire = null;

    private C underlyingCollection;
    private long tid;

    private Supplier<C> factory;

    private final Consumer<WireIn> dataConsumer = new Consumer<WireIn>() {

        @Override
        public void accept(WireIn wireIn) {
            final Bytes<?> outBytes = outWire.bytes();

            try {

                final StringBuilder eventName = acquireStringBuilder();
                final ValueIn valueIn = inWire.readEventName(eventName);

                outWire.writeDocument(true, wire -> outWire.write(CoreFields.tid).int64
                        (CollectionWireHandlerProcessor.this.tid));

                outWire.writeDocument(false, out -> {

                    // note :  remove on the key-set returns a boolean and on the map returns the
                    // old value
                    if (SetEventId.remove.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).bool(underlyingCollection.remove(fromWire.apply(valueIn)));
                        return;
                    }

                    // note :  remove on the key-set returns a boolean and on the map returns the
                    // old value
                    if (SetEventId.iterator.contentEquals(eventName)) {
                        final ValueOut valueOut = outWire.writeEventName(CoreFields.reply);
                        underlyingCollection.forEach(e -> valueOut.sequence(v -> toWire.accept(v, e)));
                        return;
                    }

                    if (SetEventId.numberOfSegments.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).int32(1);
                        return;
                    }

                    if (SetEventId.isEmpty.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).bool(underlyingCollection.isEmpty());
                        return;
                    }

                    if (SetEventId.size.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).int32(underlyingCollection.size());
                        return;
                    }

                    if (SetEventId.clear.contentEquals(eventName)) {
                        underlyingCollection.clear();
                        return;
                    }

                    if (SetEventId.contains.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).bool(
                                underlyingCollection.contains(fromWire.apply(valueIn)));
                        return;
                    }

                    if (SetEventId.add.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).bool(
                                underlyingCollection.add(fromWire.apply(valueIn)));
                        return;
                    }

                    if (SetEventId.remove.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).bool(
                                underlyingCollection.remove(fromWire.apply(valueIn)));
                        return;
                    }

                    if (SetEventId.containsAll.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).bool(
                                underlyingCollection.remove(collectionFromWire()));
                        return;
                    }

                    if (SetEventId.addAll.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).bool(
                                underlyingCollection.addAll(collectionFromWire()));
                        return;
                    }

                    if (SetEventId.removeAll.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).bool(
                                underlyingCollection.removeAll(collectionFromWire()));
                        return;
                    }

                    if (SetEventId.retainAll.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).bool(
                                underlyingCollection.retainAll(collectionFromWire()));
                        return;
                    }

                    throw new IllegalStateException("unsupported event=" + eventName);

                });


            } catch (Exception e) {
                LOG.error("", e);
            } finally {

                if (YamlLogging.showServerWrites) {
                    long len = outBytes.position() - SIZE_OF_SIZE;
                    if (len == 0) {
                        System.out.println(
                                "\n\nserver writes:\n\n<EMPTY>");
                    } else {


                        System.out.println(
                                "server writes:\n\n" +
                                        //      Bytes.toDebugString(outBytes, SIZE_OF_SIZE, len));
                                        Wires.fromSizePrefixedBlobs(outBytes, SIZE_OF_SIZE, len));

                    }
                }
            }

        }
    };


    private C collectionFromWire() {

        C c = factory.get();
        final ValueIn valueIn = outWire.getValueIn();
        while (valueIn.hasNextSequenceItem()) {
            c.add(fromWire.apply(valueIn));
        }
        return c;

    }

    @Override
    public void process(@NotNull Wire in,
                        @NotNull Wire out,
                        @NotNull C collection,
                        @NotNull CharSequence csp,
                        @NotNull BiConsumer<ValueOut, U> toWire,
                        @NotNull Function<ValueIn, U> fromWire,
                        @NotNull Supplier<C> factory,
                        long tid) throws StreamCorruptedException {


        this.fromWire = fromWire;
        this.toWire = toWire;
        this.underlyingCollection = collection;
        this.factory = factory;

        try {
            this.inWire = in;
            this.outWire = out;
            this.tid = tid;
            dataConsumer.accept(in);
        } catch (Exception e) {
            LOG.error("", e);
        }
    }


    @Override
    public void accept(@NotNull final WireHandlers wireHandlers) {

    }


}

