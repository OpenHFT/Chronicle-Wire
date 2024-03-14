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

package net.openhft.chronicle.wire.channel.book;

import net.openhft.chronicle.bytes.MethodId;

/**
 * A listener interface defining contract for receiving top of book updates.
 */
public interface TopOfBookListener {

    /**
     * Method to receive a top-of-book update.
     *
     * @param topOfBook The updated top of book object. This contains the latest market data related to the top of book.
     */
    @MethodId('t')
    void topOfBook(TopOfBook topOfBook);
}
