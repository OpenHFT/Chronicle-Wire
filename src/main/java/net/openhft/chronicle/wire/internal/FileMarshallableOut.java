package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileMarshallableOut implements MarshallableOut {
    private final URL url;
    private final FMOOptions options = new FMOOptions();
    private Wire wire;
    private final DocumentContextHolder dcHolder = new DocumentContextHolder() {
        @Override
        public void close() {
            if (chainedElement())
                return;
            super.close();
            if (wire.bytes().isEmpty())
                return;

            final String path = url.getPath();
            final String path0 = options.append ? path : (path + ".tmp");
            try (FileOutputStream out = new FileOutputStream(path0, options.append)) {
                final Bytes<byte[]> bytes = (Bytes<byte[]>) wire.bytes();
                out.write(bytes.underlyingObject(), 0, (int) bytes.readLimit());
            } catch (IOException ioe) {
                throw new IORuntimeException(ioe);
            }
            try {
                if (!options.append)
                    Files.move(Paths.get(path0), Paths.get(path), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ioe) {
                throw new IORuntimeException(ioe);
            }
            wire.clear();
        }
    };

    public FileMarshallableOut(MarshallableOutBuilder builder, WireType wIreType) {
        this.url = builder.url();
        assert url.getProtocol().equals("file");
        final String query = url.getQuery();
        if (query != null) {
            QueryWire queryWire = new QueryWire(Bytes.from(query));
            options.readMarshallable(queryWire);
        }
        this.wire = wIreType.apply(Bytes.allocateElasticOnHeap());
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

    static class FMOOptions extends SelfDescribingMarshallable {
        boolean append;
    }
}
