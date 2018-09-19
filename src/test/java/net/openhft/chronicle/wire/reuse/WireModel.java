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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.wire.AbstractMarshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.Nullable;

/**
 * @author gadei
 */
public class WireModel extends AbstractMarshallable {
    private long id;
    private int revision;
    @Nullable
    private String key;

    public WireModel() {
    }

    public WireModel(long id, int revision, String key) {
        this.id = id;
        this.revision = revision;
        this.key = key;
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        this.id = wire.read(ModelKeys.id).int64();
        this.revision = wire.read(ModelKeys.revision).int32();
        this.key = wire.read(ModelKeys.key).text();
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        wire.write(ModelKeys.id).int64(id)
                .write(ModelKeys.revision).int32(revision)
                .write(ModelKeys.key).text(key);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    @Nullable
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}

