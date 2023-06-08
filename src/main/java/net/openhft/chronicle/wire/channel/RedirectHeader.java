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
 * Class RedirectHeader extends the AbstractHeader class and is similar to an HTTP redirect header.
 * It maintains a list of location URLs to which the request can be redirected.
 */
public class RedirectHeader extends AbstractHeader<RedirectHeader> {

    // A list of location URLs to which the request can be redirected.
    private final List<String> locations = new ArrayList<>();

    /**
     * Constructs a new RedirectHeader instance and populates the list of
     * locations to which the request can be redirected.
     *
     * @param locations A list of location URLs.
     */
    public RedirectHeader(List<String> locations) {
        this.locations.addAll(locations);
    }

    /**
     * Retrieves an unmodifiable list of location URLs to which the request can be redirected.
     *
     * @return An unmodifiable list of location URLs.
     */
    public List<String> locations() {
        return Collections.unmodifiableList(locations);
    }
}
