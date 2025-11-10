//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.recursive;

import net.openhft.chronicle.wire.AbstractEventCfg;

class Base extends AbstractEventCfg<Base> {
    private final String name;

    Base(String name) {
         this.name = name;
    }

    public String name() {
        return name;
    }
}
