/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.wire.IMid;

import java.util.List;

/**
 * Interface defining a set of mock methods for testing purposes.
 * It includes methods with various argument types and one that returns another interface.
 */
public interface MockMethods {
    // Method with a single MockDto argument
    void method1(MockDto dto);

    // Another method with a single MockDto argument
    void method2(MockDto dto);

    // Method with a list of MockDto objects as an argument
    void method3(List<MockDto> dtos);

    // Method with a list of strings as an argument
    void list(List<String> strings);

    // Method designed to throw an exception
    void throwException(String s);

    // Method that returns an instance of the IMid interface
    IMid mid(String text);
}
