package net.openhft.chronicle.wire;


@FunctionalInterface
public interface MethodFilter {
    boolean shouldHandleMessage(String method, Object firstArg);
}
