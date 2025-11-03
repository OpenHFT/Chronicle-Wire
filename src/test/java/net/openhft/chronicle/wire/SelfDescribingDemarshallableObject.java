/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.annotation.UsedViaReflection;

class SelfDescribingDemarshallableObject extends SelfDescribingMarshallable implements Demarshallable {

    String name = null;
    double value = Double.NaN;

    public SelfDescribingDemarshallableObject(String name, double value) {
        this.name = name;
        this.value = value;
    }

    @SuppressWarnings("this-escape")
    @UsedViaReflection
    public SelfDescribingDemarshallableObject(WireIn wire) {
        readMarshallable(wire);
    }
}
