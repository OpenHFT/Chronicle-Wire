//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

// An interface that defines various methods with different parameter requirements.
interface ITop {
    // Define a method that takes a string and returns an IMid object.
    IMid mid(String name);

    // Define a method that takes a string and returns an IMid2 object.
    IMid2 mid2(String name);

    // Define a method with no arguments that returns an IMid object.
    IMid midNoArg();

    // Define a method that takes an int and a long and returns an IMid object.
    IMid midTwoArgs(int i, long l);
}
