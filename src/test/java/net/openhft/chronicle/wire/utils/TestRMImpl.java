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

package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.utils.api.TestRMIn;
import net.openhft.chronicle.wire.utils.dto.ReadMarshallableDTO;

/**
 * The class TestRMImpl implements the TestRMIn interface, primarily acting as a proxy or intermediary
 * for processing ReadMarshallableDTO objects.
 */
public class TestRMImpl implements TestRMIn {
    private final TestRMIn out;

    /**
     * Constructs a new instance of TestRMImpl.
     *
     * @param out An instance of TestRMIn which this class delegates to for processing.
     */
    public TestRMImpl(TestRMIn out) {
        this.out = out;
    }

    /**
     * Processes a ReadMarshallableDTO object by delegating to the 'out' instance.
     *
     * @param dto The ReadMarshallableDTO object to be processed.
     */
    @Override
    public void rm(ReadMarshallableDTO dto) {
        out.rm(dto);
    }
}
