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

package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * This is the FileMarshallableOut class implementing the MarshallableOut interface.
 * It facilitates writing to a file using the MarshallableOut specification and takes advantage
 * of options and configurations set during instantiation.
 * The class is designed to handle output operations to a file represented by a URL and manages
 * the lifecycle of the associated Wire data structure.
 */
public class FileMarshallableOut implements MarshallableOut {

    private final URL url; // The URL pointing to the file being written to
    private final FMOOptions options = new FMOOptions(); // Options for controlling file output behavior
    private Wire wire; // The underlying Wire data structure managing the data

    // DocumentContextHolder for managing document context lifecycle and output operations
    private final DocumentContextHolder dcHolder = new DocumentContextHolder() {
        @Override
        public void close() {
            // Return if the element is chained
            if (chainedElement())
                return;
            super.close();

            // Exit if the wire doesn't have any data
            if (wire.bytes().isEmpty())
                return;

            // Determine the file path based on append option
            final String path = url.getPath();
            final String path0 = options.append ? path : (path + ".tmp");
            try (FileOutputStream out = new FileOutputStream(path0, options.append)) {
                final Bytes<byte[]> bytes = Jvm.uncheckedCast(wire.bytes());
                out.write(bytes.underlyingObject(), 0, (int) bytes.readLimit());
            } catch (IOException ioe) {
                throw new IORuntimeException(ioe);
            }

            // Rename the temp file to the actual file if not in append mode
            try {
                if (!options.append)
                    Files.move(Paths.get(path0), Paths.get(path), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ioe) {
                throw new IORuntimeException(ioe);
            }

            // Clear the wire after writing to file
            wire.clear();
        }
    };

    /**
     * Constructs an instance of FileMarshallableOut using the provided builder and WireType.
     *
     * @param builder  The builder containing configuration details
     * @param wireType The WireType determining the type of wire to be used
     * @throws InvalidMarshallableException if invalid input is encountered during instantiation
     */
    public FileMarshallableOut(MarshallableOutBuilder builder, WireType wireType) throws InvalidMarshallableException {
        this.url = builder.url();
        assert url.getProtocol().equals("file"); // Ensure the protocol is "file"

        // If there's a query in the URL, parse and set the options
        final String query = url.getQuery();
        if (query != null) {
            QueryWire queryWire = new QueryWire(Bytes.from(query));
            options.readMarshallable(queryWire);
        }

        // Initialize the wire with appropriate memory allocation
        this.wire = wireType.apply(Bytes.allocateElasticOnHeap());
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        dcHolder.documentContext(wire.writingDocument(metaData));
        return dcHolder;
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        dcHolder.documentContext(wire.acquireWritingDocument(metaData));
        return dcHolder;
    }

    @Override
    public void rollbackIfNotComplete() {
        DocumentContext dc = dcHolder.documentContext();
        if (dc != null)
            dc.rollbackIfNotComplete();
    }

    /**
     * The FMOOptions class encapsulates configuration options specific to the FileMarshallableOut class.
     * Currently, it includes options for controlling file append behavior.
     */
    static class FMOOptions extends SelfDescribingMarshallable {
        boolean append; // Indicates if data should be appended to the existing file
    }
}
