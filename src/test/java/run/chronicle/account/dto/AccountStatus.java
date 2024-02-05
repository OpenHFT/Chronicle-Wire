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
import net.openhft.chronicle.wire.converter.ShortText;

public class AccountStatus extends AbstractEvent<AccountStatus> {
    private String name;
    private long account;
    @ShortText
    private int currency;
    private double amount;

    public long account() {
        return account;
    }

    public AccountStatus account(long account) {
        this.account = account;
        return this;
    }

    public AccountStatus name(String name) {
        this.name = name;
        return this;
    }

    public int currency() {
        return currency;
    }

    public AccountStatus currency(int currency) {
        this.currency = currency;
        return this;
    }

    public double amount() {
        return amount;
    }

    public AccountStatus amount(double amount) {
        this.amount = amount;
        return this;
    }

    @Override
    public void validate() throws InvalidMarshallableException {
        super.validate();
        if (name == null) throw new InvalidMarshallableException("name must be set");
        if (account == 0) throw new InvalidMarshallableException("account must be set");
        if (currency == 0) throw new InvalidMarshallableException("currency must be set");
        if (amount == 0) throw new InvalidMarshallableException("amount must be set");
    }
}
