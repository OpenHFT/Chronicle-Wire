/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings({"deprecation", "removal"})
public abstract class AbstractPooledOuterClass<N extends Marshallable> extends SelfDescribingMarshallable {
    private final List<N> listAFree = new ArrayList<>();
    private final List<N> listA = new ArrayList<>();
    private final List<N> listBFree = new ArrayList<>();
    private final List<N> listB = new ArrayList<>();

    private final Supplier<N> factory;

    private String text;
    private WireType wireType;

    protected AbstractPooledOuterClass(Supplier<N> factory) {
        this.factory = factory;
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        wire.read(() -> "text").text(this, (t, v) -> t.text = v)
                .read(() -> "wireType").object(WireType.class, this, (t, v) -> t.wireType = v)
                .read(() -> "listA").sequence(this, (t, v) -> {
                    t.clearListA();
                    while (v.hasNextSequenceItem())
                        v.marshallable(addListA());
                });

        wire.read(() -> "listB").sequence(this, (t, v) -> {
            t.clearListB();
            while (v.hasNextSequenceItem())
                v.marshallable(addListB());
        });
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "text").text(text)
                .write(() -> "wireType").text(wireType == null ? null : wireType.name())
                .write(() -> "listA").sequence(this, (t, v) -> {
                    for (N nc : t.getListA()) {
                        v.marshallable(nc);
                    }
                })
                .write(() -> "listB").sequence(this, (t, v) -> {
                    for (N nc : t.getListB()) {
                        v.marshallable(nc);
                    }
                });

    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public WireType getWireType() {
        return wireType;
    }

    public void setWireType(WireType wireType) {
        this.wireType = wireType;
    }

    @NotNull
    public List<N> getListA() {
        return listA;
    }

    public void clearListA() {
        listA.clear();
    }

    public N addListA() {
        if (listAFree.size() <= listA.size())
            listAFree.add(factory.get());
        N nc = listAFree.get(listA.size());
        listA.add(nc);
        return nc;
    }

    @NotNull
    public List<N> getListB() {
        return listB;
    }

    public void clearListB() {
        listB.clear();
    }

    public N addListB() {
        if (listBFree.size() <= listB.size())
            listBFree.add(factory.get());
        N nc = listBFree.get(listB.size());
        listB.add(nc);
        return nc;
    }
}
