/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire.channel.impl.internal;

import net.openhft.chronicle.core.Jvm;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * An extension of {@link URLStreamHandler} that facilitates custom handling for specific URL protocols.
 * This class ensures that its package is registered as a URL protocol handler, specifically for handling
 * URLs with the pattern "JavaFxCss:/path".
 */
public class Handler extends URLStreamHandler {

    // Static block that initializes the package as an URL protocol handler.
    static {
        addMyPackage();
    }

    /**
     * Initiates the static initializer for this class. Though the method body is empty, invoking this method ensures
     * that the static block is executed.
     */
    @SuppressWarnings("EmptyMethod")
    public static void init() {
        // call static initialiser
    }

    /**
     * Adds the package containing this class as a URL protocol handler.
     * Ensures that "JavaFxCss:/path" styled CSS files are recognized and handled by this package.
     */
    private static void addMyPackage() {
        // Ensure that we are registered as an url protocol handler for JavaFxCss:/path css files.
        String was = System.getProperty("java.protocol.handler.pkgs", "");

        // Get the package name for this class
        String pkg = Jvm.getPackageName(Handler.class);
        int ind = pkg.lastIndexOf('.');

        // Ensure that the package isn't the base package
        assert ind != -1 : "You can't add url handlers in the base package";

        // Register the package as an URL protocol handler
        System.setProperty("java.protocol.handler.pkgs",
                pkg.substring(0, ind) + (was.isEmpty() ? "" : "|" + was));
    }

    @Override
    protected URLConnection openConnection(URL u) {
        throw new UnsupportedOperationException();
    }
}
