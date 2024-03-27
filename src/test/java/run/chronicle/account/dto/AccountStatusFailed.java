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

public class AccountStatusFailed extends AbstractEvent<AccountStatusFailed> {
    // Field to store the associated AccountStatus object
    private AccountStatus accountStatus;

    // Field to store the reason for the failure
    private String reason;

    // Getter for 'accountStatus'
    public AccountStatus accountStatus() {
        return accountStatus;
    }

    // Fluent setter for 'accountStatus', returning the current class instance
    public AccountStatusFailed accountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
        return this;
    }

    // Getter for 'reason'
    public String reason() {
        return reason;
    }

    // Fluent setter for 'reason', returning the current class instance
    public AccountStatusFailed reason(String reason) {
        this.reason = reason;
        return this;
    }

    // Override of the 'validate' method to ensure all fields are properly set
    @Override
    public void validate() throws InvalidMarshallableException {
        super.validate();
        // Validations for each field
        if (accountStatus == null) throw new InvalidMarshallableException("accountStatus must be set");
        if (reason == null) throw new InvalidMarshallableException("reason must be set");
    }
}
