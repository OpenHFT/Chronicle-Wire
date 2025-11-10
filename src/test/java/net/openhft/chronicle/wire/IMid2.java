//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

// Another interface defining methods to get the next interface `ILast` and to handle DTO operations
interface IMid2 {
    ILast next2(String a);

    void dto(DMOuterClass dmoc);
}
