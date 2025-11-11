#!/bin/bash
#
# Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
#

mvn clean  test-compile exec:java -Dexec.mainClass="net.openhft.chronicle.wire.TriviallyCopyableJLBH" -Dexec.classpathScope=test
