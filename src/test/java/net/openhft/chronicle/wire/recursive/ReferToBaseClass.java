/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.recursive;

import java.util.ArrayList;
import java.util.List;

public class ReferToBaseClass extends ReferToSameClass {
    private final List<ReferToSameClass> list = new ArrayList<>();

    public ReferToBaseClass(String name) {
        super(name);
    }

    @Override
    public List<ReferToSameClass> list() {
        return list;
    }
}
