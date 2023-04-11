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
    private Transfer transfer;
    private String reason;

    public Transfer transfer() {
        return transfer;
    }

    public TransferFailed transfer(Transfer transfer) {
        this.transfer = transfer;
        return this;
    }

    public String reason() {
        return reason;
    }

    public TransferFailed reason(String reason) {
        this.reason = reason;
        return this;
    }

    @Override
    public void validate() throws InvalidMarshallableException {
        super.validate();
        if (transfer == null) throw new InvalidMarshallableException("transfer must be set");
        if (reason == null) throw new InvalidMarshallableException("reason must be set");
    }
}
