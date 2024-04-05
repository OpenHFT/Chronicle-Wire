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

package run.chronicle.account.dto;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.Validatable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.converter.NanoTime;
import net.openhft.chronicle.wire.converter.ShortText;

public class AbstractEvent<E extends AbstractEvent<E>> extends SelfDescribingMarshallable implements Validatable {
    @ShortText
    private long sender;
    @ShortText
    private long target;

    // Time when the event was sent, annotated with @NanoTime
    @NanoTime
    private long sendingTime;

    // Getter for 'sender'
    public long sender() {
        return sender;
    }

    // Fluent setter for 'sender', returning the generic type 'E'
    public E sender(long sender) {
        this.sender = sender;
        return (E) this;
    }

    // Getter for 'target'
    public long target() {
        return target;
    }

    // Fluent setter for 'target', returning the generic type 'E'
    public E target(long target) {
        this.target = target;
        return (E) this;
    }

    // Fluent setter for 'sendingTime', returning the generic type 'E'
    public E sendingTime(long sendingTime) {
        this.sendingTime = sendingTime;
        return (E) this;
    }

    // Implementation of the 'validate' method from the Validatable interface
    @Override
    public void validate() throws InvalidMarshallableException {
        // Validating that 'sender', 'target', and 'sendingTime' are set
        if (sender == 0) throw new InvalidMarshallableException("sender must be set");
        if (target == 0) throw new InvalidMarshallableException("target must be set");
        if (sendingTime == 0) throw new InvalidMarshallableException("sendingTime must be set");
    }
}
