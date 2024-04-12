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
    // Field for account name
    private String name;

    // Field for account number
    private long account;
    @ShortText
    private int currency;

    // Field for amount in the account
    private double amount;

    // Getter for 'account'
    public long account() {
        return account;
    }

    // Fluent setter for 'account', returning the current class instance
    public AccountStatus account(long account) {
        this.account = account;
        return this;
    }

    // Fluent setter for 'name', returning the current class instance
    public AccountStatus name(String name) {
        this.name = name;
        return this;
    }

    // Getter for 'currency'
    public int currency() {
        return currency;
    }

    // Fluent setter for 'currency', returning the current class instance
    public AccountStatus currency(int currency) {
        this.currency = currency;
        return this;
    }

    // Getter for 'amount'
    public double amount() {
        return amount;
    }

    // Fluent setter for 'amount', returning the current class instance
    public AccountStatus amount(double amount) {
        this.amount = amount;
        return this;
    }

    // Override of the 'validate' method to ensure all fields are properly set
    @Override
    public void validate() throws InvalidMarshallableException {
        super.validate();
        // Validations for each field
        if (name == null) throw new InvalidMarshallableException("name must be set");
        if (account == 0) throw new InvalidMarshallableException("account must be set");
        if (currency == 0) throw new InvalidMarshallableException("currency must be set");
        if (amount == 0) throw new InvalidMarshallableException("amount must be set");
    }
}
