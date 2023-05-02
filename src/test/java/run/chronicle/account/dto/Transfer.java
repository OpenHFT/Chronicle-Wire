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
import net.openhft.chronicle.wire.converter.Base85;

public class Transfer extends AbstractEvent<Transfer> {
    private long from, to;
    @Base85
    private int currency;
    private double amount;
    private Bytes reference = Bytes.allocateElasticOnHeap();

    public long from() {
        return from;
    }

    public Transfer from(long from) {
        this.from = from;
        return this;
    }

    public long to() {
        return to;
    }

    public Transfer to(long to) {
        this.to = to;
        return this;
    }

    public int currency() {
        return currency;
    }

    public Transfer currency(int currency) {
        this.currency = currency;
        return this;
    }

    public double amount() {
        return amount;
    }

    public Transfer amount(double amount) {
        this.amount = amount;
        return this;
    }

    public Transfer reference(Bytes reference) {
        this.reference.clear().append(reference);
        return this;
    }

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
