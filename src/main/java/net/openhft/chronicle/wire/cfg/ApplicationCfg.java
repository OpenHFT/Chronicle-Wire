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

package net.openhft.chronicle.wire.cfg;

import net.openhft.chronicle.wire.ValueIn;
import net.openhft.chronicle.wire.WireIn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created by peter on 26/08/15.
 */
public class ApplicationCfg implements MapInstallable {
    static final Logger LOGGER = LoggerFactory.getLogger(ApplicationCfg.class);
    final Map<String, MapInstallable> installableMap = new LinkedHashMap<>();
    final List<Consumer<Map<String, Object>>> toInstall = new ArrayList<>();

    @Override
    public Void install(String path, Map<String, Object> assetTree) throws Exception {
        LOGGER.info("Building Engine " + assetTree);
        for (Map.Entry<String, MapInstallable> entry : installableMap.entrySet()) {
            String path2 = entry.getKey();
            LOGGER.info("Installing " + path2 + ": " + entry.getValue());
            Object install = entry.getValue().install(path2, assetTree);
            if (install != null) {
                int pos = path2.lastIndexOf('/');
                String parent = path2.substring(0, pos);
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) assetTree.computeIfAbsent(parent,
                        s -> Collections.synchronizedMap(new LinkedHashMap<>()));
                String name = path2.substring(pos + 1);
                map.put(name, install);
            }
        }
        for (Consumer<Map<String, Object>> consumer : toInstall) {
            consumer.accept(assetTree);
        }
        return null;
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        readMarshallable("", wire);
    }

    private void readMarshallable(String path, WireIn wire) {
        StringBuilder name = new StringBuilder();
        while (wire.hasMore()) {
            ValueIn in = wire.read(name);
            long pos = wire.bytes().readPosition();
            String path2 = path + "/" + name;
            if (in.isTyped()) {
                in.marshallable(w -> this.readMarshallable(path2, w));
            } else {
                wire.bytes().readPosition(pos);
                Object o = in.typedMarshallable();
                installableMap.put(path2, (MapInstallable) o);
            }
        }
    }
}
