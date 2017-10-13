package net.openhft.chronicle.wire;

@FunctionalInterface
public interface MethodFilterOnFirstArg<T> {
    /**
     * For multi-argument method calls, this gives the option to not read the rest of the arguments and ignore the method
     *
     * @param methodName name of the method
     * @param firstArg   the first argument which can be used for filtering
     * @return true if it should be ignored.
     */
    boolean ignoreMethodBasedOnFirstArg(String methodName, T firstArg);
}
