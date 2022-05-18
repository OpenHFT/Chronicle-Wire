package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.TextMethodTester;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.function.Function;

public interface YamlTester {
    /**
     * Test a microservice implemented in a class using in.yaml comparing with out.yaml,
     * with optionally setup.yaml to initialise it.
     *
     * @param implClass of the implementation
     * @param path      where the yaml files can be found
     * @return the results for comparison
     * @throws AssertionError if anything went wrong
     */
    static YamlTester runTest(Class<?> implClass, String path) throws AssertionError {
        for (Constructor<?> cons : implClass.getDeclaredConstructors()) {
            if (cons.getParameterCount() == 1) {
                final Class<?>[] parameterTypes = cons.getParameterTypes();
                if (parameterTypes[0].isInterface()) {
                    return runTest((Object out) -> {
                        try {
                            return cons.newInstance(out);
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    }, (Class) parameterTypes[0], path);
                }
            }
        }
        throw new IllegalArgumentException("Unable to find a constructor with one interface as an argument");
    }

    /**
     * Test a microservice implemented in a class using in.yaml comparing with out.yaml,
     * with optionally setup.yaml to initialise it.
     *
     * @param builder  to construct a component to be tested
     * @param outClass the interface of output
     * @param path     where the yaml files can be found
     * @return the results for comparison
     * @throws AssertionError if anything went wrong
     */
    static <T> YamlTester runTest(Function<T, Object> builder, Class<T> outClass, String path) throws AssertionError {
        try {
            return new TextMethodTester<T>(
                    path + "/in.yaml",
                    builder,
                    outClass,
                    path + "/out.yaml")
                    .setup(path + "/setup.yaml")
                    .run();
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }
    }

    /**
     * @return the expected String
     */
    String expected();

    /**
     * @return the actual String
     */
    String actual();
}
