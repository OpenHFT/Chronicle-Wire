//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.recursive;

import java.util.ArrayList;
import java.util.List;

class ReferToSameClass extends Base {
    private final List<ReferToSameClass> list = new ArrayList<>();

    public ReferToSameClass(String name) {
        super(name);
    }

    public List<ReferToSameClass> list() {
        return list;
    }
}
