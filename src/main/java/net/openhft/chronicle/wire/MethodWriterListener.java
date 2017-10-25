package net.openhft.chronicle.wire;

/*
 * Created by peter.lawrey@chronicle.software on 30/07/2017
 * <p>
 * Invoked before writing out this method and args. See also MethodWriterInterceptor
 */
@FunctionalInterface
public interface MethodWriterListener {
    void onWrite(String name, Object[] args);
}
