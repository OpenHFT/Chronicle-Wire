/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class EnumSetMarshallingTest {
    private static final String FULL_SET_SERIALISED_FORM =
            "--- !!data #binary\n" +
                    "key: {\n" +
                    "  f: [\n" +
                    "    TIMED_WAITING,\n" +
                    "    WAITING,\n" +
                    "    BLOCKED,\n" +
                    "    RUNNABLE,\n" +
                    "    TERMINATED,\n" +
                    "    NEW\n" +
                    "  ]\n" +
                    "}\n";

    private static final String EMPTY_SET_SERIALISED_FORM =
            "--- !!data #binary\n" +
                    "key: {\n" +
                    "  f: [  ]\n" +
                    "}\n";

    @Test
    public void shouldMarshallEmptySet() {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final Foo written = new Foo(EnumSet.noneOf(Thread.State.class));
        final Foo read = new Foo(EnumSet.allOf(Thread.State.class));

        @NotNull Wire tw = new BinaryWire(bytes);
        tw.writeDocument(false, w -> {
            w.write(() -> "key").marshallable(written);
        });

        assertThat(Wires.fromSizePrefixedBlobs(bytes), is(EMPTY_SET_SERIALISED_FORM));
        tw.readingDocument().wire().read("key").marshallable(read);

        assertThat(read.f, is(written.f));
        bytes.release();
    }

    @Test
    public void shouldMarshallFullSet() throws Exception {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final Foo written = new Foo(EnumSet.allOf(Thread.State.class));
        final Foo read = new Foo(EnumSet.noneOf(Thread.State.class));

        @NotNull Wire tw = new BinaryWire(bytes);
        tw.writeDocument(false, w -> {
            w.write(() -> "key").marshallable(written);
        });

        assertThat(Wires.fromSizePrefixedBlobs(bytes), is(FULL_SET_SERIALISED_FORM));
        tw.readingDocument().wire().read("key").marshallable(read);

        assertThat(read.f, is(written.f));
        bytes.release();
    }

    @Test
    public void shouldUnmarshallToContainerWithNullValue() throws Exception {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final Foo written = new Foo(EnumSet.allOf(Thread.State.class));
        final Foo read = new Foo(EnumSet.noneOf(Thread.State.class));
        // this forces the framework to allocate a new instance of EnumSet
        read.f = null;

        @NotNull Wire tw = new BinaryWire(bytes);
        tw.writeDocument(false, w -> {
            w.write(() -> "key").marshallable(written);
        });

        assertThat(Wires.fromSizePrefixedBlobs(bytes), is(FULL_SET_SERIALISED_FORM));
        tw.readingDocument().wire().read("key").marshallable(read);

        assertThat(read.f, is(written.f));
        bytes.release();
    }

    @Test
    public void shouldAllowMultipleInstancesInObjectGraph() throws Exception {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final Container written = new Container();
        final Container read = new Container();

        @NotNull Wire tw = new BinaryWire(bytes);
        tw.writeDocument(false, w -> {
            w.write(() -> "key").marshallable(written);
        });

        tw.readingDocument().wire().read("key").marshallable(read);

        assertThat(read.f1.get(0).f, is(not(read.f2.get(0).f)));
        bytes.release();
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    private static final class Container extends AbstractMarshallable {
        private List<Foo> f1 = new ArrayList<>(Arrays.asList(new Foo(EnumSet.allOf(Thread.State.class))));
        private List<Foo> f2 = new ArrayList<>(Arrays.asList(new Foo(EnumSet.noneOf(Thread.State.class))));
    }

    private static final class Foo extends AbstractMarshallable {
        private EnumSet<Thread.State> f = EnumSet.noneOf(Thread.State.class);

        private Foo(final EnumSet<Thread.State> membership) {
            f = membership;
        }
    }
}
