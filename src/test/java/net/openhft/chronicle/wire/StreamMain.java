/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.UUID;

/*
 * Created by Peter Lawrey on 30/06/15.
 */
public class StreamMain {
    public static void main(String[] args) {
        ClassAliasPool.CLASS_ALIASES.addAlias(FileFormat.class);
        for (@NotNull WireType wt : WireType.values()) {
            if (wt == WireType.CSV)
                continue;

            @NotNull Bytes b = Bytes.allocateElasticDirect();
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

class FileFormat extends AbstractMarshallable {
    int version = 100;
    ZonedDateTime createdTime = ZonedDateTime.now();
    String creator = System.getProperty("user.name");
    UUID identity = UUID.randomUUID();
    WireType wireType;

    @Override
    public void readMarshallable(WireIn wire) throws IllegalStateException {
        wire.read(() -> "version").int32(this, (o, s) -> o.version = s)
                .read(() -> "createdTime").zonedDateTime(this, (o, z) -> o.createdTime = z)
                .read(() -> "creator").text(this, (o, s) -> o.creator = s)
                .read(() -> "identity").uuid(this, (o, u) -> o.identity = u)
                .read(() -> "wireType").object(WireType.class, this, (o, wt) -> o.wireType = wt);
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "version").int32(version)
                .write(() -> "createdTime").zonedDateTime(createdTime)
                .write(() -> "creator").text(creator)
                .write(() -> "identity").uuid(identity)
                .write(() -> "wireType").object(wireType);
    }
}

