#!/bin/bash
mvn clean  test-compile exec:java -Dexec.mainClass="net.openhft.chronicle.wire.TriviallyCopyableJLBH" -Dexec.classpathScope=test