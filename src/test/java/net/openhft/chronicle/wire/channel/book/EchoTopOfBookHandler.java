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

// Generated code added here as there is an issue with Java 18.

package net.openhft.chronicle.wire.channel.book;

import static net.openhft.chronicle.wire.channel.book.PerfTopOfBookMain.ONE__NEW_OBJECT;

/**
 * Handler class designed to echo received TopOfBook objects, where the primary workload resides
 * in the deserialization and serialization processes.
 */
public class EchoTopOfBookHandler implements ITopOfBookHandler {

    // Member to hold a reference to a TopOfBookListener which will consume echoed messages
    private TopOfBookListener topOfBookListener;

    /**
     * Handles a top-of-book update, echoing it to the registered listener.
     * A deep copy might be performed on the input object to ensure data consistency
     * during asynchronous operations.
     *
     * @param topOfBook The incoming top-of-book data to be echoed back.
     */
    @Override
    public void topOfBook(TopOfBook topOfBook) {
        // Check for the ONE__NEW_OBJECT flag (not defined in provided code)
        // and perform a deep copy if it's true to avoid potential data mutations
        if (ONE__NEW_OBJECT)
            topOfBook = topOfBook.deepCopy();

        // Echo the (potentially copied) top-of-book data to the registered listener
        topOfBookListener.topOfBook(topOfBook);
    }

    /**
     * Configures this handler to echo messages to the provided listener.
     *
     * @param topOfBookListener A TopOfBookListener that will consume echoed top-of-book updates.
     * @return This handler instance, allowing method chaining.
     */
    @Override
    public EchoTopOfBookHandler out(TopOfBookListener topOfBookListener) {
        // Assign the provided listener to the member variable
        this.topOfBookListener = topOfBookListener;

        // Return this instance to allow method chaining
        return this;
    }
}
