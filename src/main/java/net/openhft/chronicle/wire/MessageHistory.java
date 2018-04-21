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

/*
 * Created by Peter Lawrey on 27/03/16.
 */
public interface MessageHistory extends Marshallable {
    /**
     * Get the MessageHistory to update it or read it.
     *
     * @return the MessageHistory for the current Excerpt.
     */
    static MessageHistory get() {
        return VanillaMessageHistory.getThreadLocal();
    }

    /**
     * You only need to call this if you wish to override it's behaviour.
     *
     * @param md to change to the default implementation for this thread.
     */
    static void set(MessageHistory md) {
        VanillaMessageHistory.setThreadLocal(md);
    }

    int timings();

    long timing(int n);

    int sources();

    int sourceId(int n);

    long sourceIndex(int n);

    /**
     * clear all data
     */
    void reset();

    /**
     * reset for a given source prior to reading a new message in
     *
     */
    void reset(int sourceId, long sourceIndex);

    int lastSourceId();

    long lastSourceIndex();
}
