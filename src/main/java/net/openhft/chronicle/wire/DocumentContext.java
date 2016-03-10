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

import net.openhft.chronicle.core.io.Closeable;

/**
 * Created by peter on 24/12/15.
 */
public interface DocumentContext extends Closeable {

    /**
     * @return true - is the entry is of type meta data
     */
    boolean isMetaData();

    /**
     * @return true - if is a document document
     */
    boolean isPresent();

    /**
     * @return true - is the entry is of type data
     */
    default boolean isData() {
        return isPresent() && !isMetaData();
    }

    /**
     * @return the wire of the document
     */
    Wire wire();


}
