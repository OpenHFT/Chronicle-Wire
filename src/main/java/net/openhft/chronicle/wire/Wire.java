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

import java.io.IOException;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream.
 * <p>
 * Created by peter.lawrey on 12/01/15.
 */
public interface Wire extends WireIn, WireOut {
    static Wire fromFile(String name) throws IOException {
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
            case "csv":
                return CSVWire.fromFile(name);
            case "yaml":
                return TextWire.fromFile(name);
            default:
                throw new IllegalArgumentException("Unknown file type " + name);
        }
    }

}
