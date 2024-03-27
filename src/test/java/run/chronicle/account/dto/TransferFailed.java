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

public class TransferFailed extends AbstractEvent<TransferFailed> {
    // Field to hold the Transfer object that failed
    private Transfer transfer;
    // Field to hold the reason for the failure
    private String reason;

    // Getter for the 'transfer' field
    public Transfer transfer() {
        return transfer;
    }

    // Setter for the 'transfer' field, allowing method chaining
    public TransferFailed transfer(Transfer transfer) {
        this.transfer = transfer;
        return this;
    }

    // Getter for the 'reason' field
    public String reason() {
        return reason;
    }

    // Setter for the 'reason' field, allowing method chaining
    public TransferFailed reason(String reason) {
        this.reason = reason;
        return this;
    }

    // Method to validate if all necessary fields are set correctly
    @Override
    public void validate() throws InvalidMarshallableException {
        super.validate();
        if (transfer == null) throw new InvalidMarshallableException("transfer must be set");
        if (reason == null) throw new InvalidMarshallableException("reason must be set");
    }
}
