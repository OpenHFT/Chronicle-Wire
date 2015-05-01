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

package net.openhft.chronicle.wire.set;

/**
 * Created by Rob Austin
 */

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StreamCorruptedException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.openhft.chronicle.wire.Wires.acquireStringBuilder;


/**
 * @author Rob Austin.
 */
public class SetWireHandlerProcessor<U> implements SetWireHandler<Set<U>, U>, Consumer<WireHandlers> {

    private Function<ValueIn, U> fromWire;
    private BiConsumer<U, ValueOut> toWire;

    private static final Logger LOG = LoggerFactory.getLogger(SetWireHandlerProcessor.class);

    public static final int SIZE_OF_SIZE = 2;

    private Wire inWire = null;
    private Wire outWire = null;

    private Set<U> underlyingSet;
    public long tid;

    private final Consumer<WireIn> metaDataConsumer = new Consumer<WireIn>() {

        @Override
        public void accept(WireIn wireIn) {

            StringBuilder sb = Wires.acquireStringBuilder();

            for (; ; ) {
                final ValueIn read = inWire.read(sb);
                if (CoreFields.tid.contentEquals(sb)) {
                    tid = read.int64();
                    break;
                }
            }


        }
    };

    private final Consumer<WireIn> dataConsumer = new Consumer<WireIn>() {

        @Override
        public void accept(WireIn wireIn) {
            final Bytes<?> outBytes = outWire.bytes();

            try {

                final StringBuilder eventName = acquireStringBuilder();
                final ValueIn valueIn = inWire.readEventName(eventName);

                outWire.writeDocument(true, wire -> outWire.write(CoreFields.tid).int64(tid));

                outWire.writeDocument(false, out -> {

                    // note :  remove on the key-set returns a boolean and on the map returns the
                    // old value
                    if (SetEventId.remove.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).bool(underlyingSet.remove(fromWire.apply(valueIn)));
                        return;
                    }

                    // note :  remove on the key-set returns a boolean and on the map returns the
                    // old value
                    if (SetEventId.iterator.contentEquals(eventName)) {
                        final ValueOut valueOut = outWire.writeEventName(CoreFields.reply);
                        underlyingSet.forEach(e -> valueOut.sequence(v -> toWire.accept(e, v)));
                        return;
                    }


                    if (SetEventId.numberOfSegments.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).int32(1);
                        return;
                    }

                    if (SetEventId.isEmpty.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).bool(underlyingSet.isEmpty());
                        return;
                    }

                    if (SetEventId.size.contentEquals(eventName)) {
                        outWire.write(CoreFields.reply).int32(underlyingSet.size());
                        return;
                    }


                    if (SetEventId.clear.contentEquals(eventName)) {
                        underlyingSet.clear();
                        return;
                    }

                    throw new IllegalStateException("unsupported event=" + eventName);
                });


            } catch (Exception e) {
                LOG.error("", e);
            } finally {


                long len = outBytes.position() - SIZE_OF_SIZE;
                if (len == 0) {
                    System.out.println("--------------------------------------------\n" +
                            "server writes:\n\n<EMPTY>");
                } else {


                    System.out.println("--------------------------------------------\n" +
                            "server writes:\n\n" +
                            //      Bytes.toDebugString(outBytes, SIZE_OF_SIZE, len));
                            Wires.fromSizePrefixedBlobs(outBytes, SIZE_OF_SIZE, len));

                }

            }

        }
    };


    @Override
    public void process(@NotNull final Wire in,
                        @NotNull final Wire out,
                        @NotNull final Set<U> set,
                        @NotNull final CharSequence csp, BiConsumer<U, ValueOut> toWire,
                        @NotNull final Function<ValueIn, U> fromWire) throws StreamCorruptedException {

        this.fromWire = fromWire;
        this.toWire = toWire;
        this.underlyingSet = set;

        try {
            this.inWire = in;
            this.outWire = out;

            inWire.readDocument(metaDataConsumer, dataConsumer);
        } catch (Exception e) {
            LOG.error("", e);
        }
    }


    @Override
    public void accept(@NotNull final WireHandlers wireHandlers) {

    }


}

