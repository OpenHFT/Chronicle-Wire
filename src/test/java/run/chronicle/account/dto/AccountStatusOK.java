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

public class AccountStatusOK extends AbstractEvent<AccountStatusOK> {
    // Field to hold account status information
    private AccountStatus accountStatus;

    // Getter method for account status
    public AccountStatus accountStatus() {
        return accountStatus;
    }

    // Setter method for account status with a fluent interface
    public AccountStatusOK accountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
        return this;
    }

    // Validates the object ensuring all required fields are set
    @Override
    public void validate() throws InvalidMarshallableException {
        // Call the validate method of the parent class
        super.validate();

        if (accountStatus == null) throw new InvalidMarshallableException("accountStatus must be set");
    }
}
