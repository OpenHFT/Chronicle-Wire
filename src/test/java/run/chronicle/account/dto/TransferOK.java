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

public class TransferOK extends AbstractEvent<TransferOK> {
    // Field to store transfer details
    private Transfer transfer;

    // Getter for transfer details
    public Transfer transfer() {
        return transfer;
    }

    // Setter for transfer details, returns the updated object for method chaining
    public TransferOK transfer(Transfer transfer) {
        this.transfer = transfer;
        return this;
    }

    // Override the validate method to ensure the transfer details are properly set
    @Override
    public void validate() throws InvalidMarshallableException {
        super.validate(); // Calling validate method of the parent class
        if (transfer == null)
            // Throw exception if transfer details are not set
            throw new InvalidMarshallableException("transfer must be set");
    }
}
