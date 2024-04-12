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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.converter.ShortText;

public class Transfer extends AbstractEvent<Transfer> {
    // Fields representing transaction details
    private long from, to;
    @ShortText
    private int currency;
    private double amount;
    private Bytes<byte[]> reference = Bytes.allocateElasticOnHeap();

    // Getter for 'from' field
    public long from() {
        return from;
    }

    // Setter for 'from' field, allowing method chaining
    public Transfer from(long from) {
        this.from = from;
        return this;
    }

    // Getter for 'to' field
    public long to() {
        return to;
    }

    // Setter for 'to' field, allowing method chaining
    public Transfer to(long to) {
        this.to = to;
        return this;
    }

    // Getter for 'currency' field
    public int currency() {
        return currency;
    }

    // Setter for 'currency' field, allowing method chaining
    public Transfer currency(int currency) {
        this.currency = currency;
        return this;
    }

    // Getter for 'amount' field
    public double amount() {
        return amount;
    }

    // Setter for 'amount' field, allowing method chaining
    public Transfer amount(double amount) {
        this.amount = amount;
        return this;
    }

    public Transfer reference(Bytes<?> reference) {
        this.reference.clear().append(reference);
        return this;
    }

    // Method to validate if all necessary fields are set correctly
    @Override
    public void validate() throws InvalidMarshallableException {
        super.validate();
        if (from == 0) throw new InvalidMarshallableException("from must be set");
        if (to == 0) throw new InvalidMarshallableException("to must be set");
        if (currency == 0) throw new InvalidMarshallableException("currency must be set");
        if (amount == 0) throw new InvalidMarshallableException("amount must be set");
        if (reference == null) throw new InvalidMarshallableException("reference must be set");
    }
}
