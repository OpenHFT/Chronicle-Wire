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

package net.openhft.chronicle.wire.channel.impl.tcp;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * An extension of {@link URLStreamHandler} designed to provide custom handling for specific URL protocols.
 * Currently, this class does not support opening connections and will throw an {@link UnsupportedOperationException}
 * if an attempt is made to open a connection.
 */
public class Handler extends URLStreamHandler {

    /**
     * Attempts to open a connection to the provided URL. In the current implementation, this operation
     * is unsupported and will always throw an exception.
     *
     * @param u The URL to which a connection should be opened.
     * @return This method does not return a value; instead, it throws an exception.
     * @throws UnsupportedOperationException always, as this operation isn't supported.
     */
    @Override
    protected URLConnection openConnection(URL u) {
        throw new UnsupportedOperationException();
    }
}
