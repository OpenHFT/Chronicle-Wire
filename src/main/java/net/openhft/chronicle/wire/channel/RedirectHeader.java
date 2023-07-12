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

package net.openhft.chronicle.wire.channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A specialized version of {@link AbstractHeader}, acting similar to an HTTP redirect header.
 * It encapsulates a list of URL locations that a request can be redirected to.
 *
 * <p>This class is useful in scenarios where a request needs to be redirected to multiple potential locations.
 */
public class RedirectHeader extends AbstractHeader<RedirectHeader> {

    // A list of location URLs to which the request can be redirected.
    private final List<String> locations = new ArrayList<>();

    /**
     * Constructs a new RedirectHeader instance, populating its internal list of
     * URL locations with the provided list.
     *
     * @param locations A list of URLs to which the request can be redirected. This list is copied to prevent external modification.
     */
    public RedirectHeader(List<String> locations) {
        this.locations.addAll(locations);
    }

    /**
     * Retrieves an unmodifiable view of the list of URL locations to which the request can be redirected.
     * This prevents modifications to the list of locations outside of this class.
     *
     * @return An unmodifiable list of URL locations.
     */
    public List<String> locations() {
        return Collections.unmodifiableList(locations);
    }
}
