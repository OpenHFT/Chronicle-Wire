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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Created by peter on 30/06/15.
 */
public class StreamMain {
    public static void main(String[] args) {
        ClassAliasPool.CLASS_ALIASES.addAlias(FileFormat.class);
        for (WireType wt : WireType.values()) {
            Bytes b = NativeBytes.nativeBytes();
            Wire w = wt.apply(b);
            w.writeDocument(true, w2 -> w2.write(() -> "header")
                    .typedMarshallable(new FileFormat()));
            w.writeDocument(false, w2 -> w2.write(() -> "data")
                    .typedMarshallable("MyData", w3 -> w3.write(() -> "field1").int32(1)
                            .write(() -> "feild2").int32(2)));
            boolean isText = b.readByte(4) >= ' ';
            System.out.println("### " + wt + " Format");
            System.out.println("```" + (isText ? "yaml" : ""));
            System.out.print(isText ? b.toString().replaceAll("\u0000", "\\\\0") : b.toHexString());
            System.out.println("```\n");
        }
    }
}

class FileFormat implements Marshallable {
    int version = 100;
    ZonedDateTime createdTime = ZonedDateTime.now();
    String creator = System.getProperty("user.name");
    UUID identity = UUID.randomUUID();
    WireType wireType;

    @Override
    public void readMarshallable(WireIn wire) throws IllegalStateException {
        wire.read(() -> "version").int32(s -> version = s)
                .read(() -> "createdTime").zonedDateTime(z -> createdTime = z)
                .read(() -> "creator").text(s -> creator = s)
                .read(() -> "identity").uuid(u -> identity = u)
                .read(() -> "wireType").object(WireType.class, wt -> wireType = wt);
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        wire.write(() -> "version").int32(version)
                .write(() -> "createdTime").zonedDateTime(createdTime)
                .write(() -> "creator").text(creator)
                .write(() -> "identity").uuid(identity)
                .write(() -> "wireType").object(wireType);
    }
}

