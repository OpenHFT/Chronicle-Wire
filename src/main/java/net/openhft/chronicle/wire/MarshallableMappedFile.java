/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MappedFile;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;

/**
 * A memory mapped files which can be randomly accessed in chunks. It has overlapping regions to
 * avoid wasting bytes at the end of chunks.
 */
public class MarshallableMappedFile implements Marshallable {

    private MappedFile mf;

    private String filename;
    private long chunkSize;
    private long overlapSize;

    public MarshallableMappedFile(MappedFile mappedFile) {
        this.mf = mappedFile;
        this.filename = mappedFile.file().getAbsolutePath().toString();
        this.chunkSize = mappedFile.chunkSize();
        this.overlapSize = this.overlapSize;
    }


    @Override
    public void readMarshallable(@NotNull WireIn wire) throws net.openhft.chronicle.core.io.IORuntimeException {
        wire.read(() -> "filename").text(this, (t, v) -> t.filename = v);
        wire.read(() -> "chunkSize").int64(this, (t, v) -> t.chunkSize = v);
        wire.read(() -> "overlapSize").int64(this, (t, v) -> t.overlapSize = v);

        try {
            mf = MappedFile.mappedFile(filename, chunkSize, overlapSize);
        } catch (FileNotFoundException e) {
            Jvm.rethrow(e);
        }
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "filename").text(mf.file().getAbsoluteFile().toString());
        wire.write(() -> "chunkSize").int64(mf.chunkSize());
        wire.write(() -> "overlapSize").int64(mf.overlapSize());
    }

    public MappedFile mf() {
        return mf;
    }
}
