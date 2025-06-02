/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A utility class for handling reflection-based tasks.
 * This class provides utility methods related to class reflection and
 * name generation, especially concerning interfaces and package names.
 * <p>
 * Note: This class is not intended for instantiation.
 */
public final class ReflectionUtil {

    /**
     * System property ({@code wire.method.prependPackage}) flag. If true,
     * {@link #generatedPackageName(String)} will always prepend
     * {@link #PACKAGE_PREFIX} to the package name.
     */
    private static final boolean PREPEND_PACKAGE =
            Jvm.getBoolean("wire.method.prependPackage");

    /**
     * The base package prefix ({@code net.openhft.chronicle.wire.method}) used
     * by {@link #generatedPackageName(String)} when prepending is active.
     */
    private static final String PACKAGE_PREFIX =
            "net.openhft.chronicle.wire.method";

    // Private constructor to prevent instantiation
    private ReflectionUtil() {
    }

    /**
     * Creates and returns a new {@link List} of all interfaces implemented by
     * the provided {@code oClass} and all its super classes.
     *
     * @param oClass The class whose implemented interfaces (including inherited ones) are to be retrieved.
     * @return A new {@link List} of all unique interfaces implemented by {@code oClass} and its superclass hierarchy.
     */
    public static List<Class<?>> interfaces(@NotNull final Class<?> oClass) {
        final List<Class<?>> list = new ArrayList<>();
        interfaces(oClass, list);
        return list;
    }

    /**
     * Recursively populates the {@code list} with interfaces implemented by
     * {@code oClass} and its superclasses, stopping at {@code java.lang.Object}.
     */
    private static void interfaces(final Class<?> oClass, final List<Class<?>> list) {
        final Class<?> baseClass = oClass.getSuperclass();
        if (baseClass == null)
            // We have reached java.lang.Object
            return;
        list.addAll(Arrays.asList(oClass.getInterfaces()));
        interfaces(baseClass, list);
    }

    /**
     * Generates a package name based on the supplied class name. If the class
     * resides in a reserved package such as {@code java.} or if
     * {@link #PREPEND_PACKAGE} is true, the resulting name will be prefixed with
     * {@link #PACKAGE_PREFIX}.
     *
     * @param classFullName The fully qualified name of the class for which to
     *                      generate a potentially modified package name (e.g.,
     *                      for generated proxy classes).
     * @return The generated package name.
     */
    @NotNull
    public static String generatedPackageName(String classFullName) {
        int lastDot = classFullName.lastIndexOf('.');

        if (lastDot != -1) {
            String packageName = classFullName.substring(0, lastDot);

            if (PREPEND_PACKAGE || classFullName.startsWith("java.") || classFullName.startsWith("javax.")
                    || classFullName.startsWith("com.sun.") || classFullName.startsWith("jdk.")) {
                return PACKAGE_PREFIX + "." + packageName;
            }
            return packageName;
        }

        return PREPEND_PACKAGE ? PACKAGE_PREFIX : "";
    }
}
