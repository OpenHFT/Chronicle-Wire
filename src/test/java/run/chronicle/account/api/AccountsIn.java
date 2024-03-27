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

package run.chronicle.account.api;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import run.chronicle.account.dto.AccountStatus;
import run.chronicle.account.dto.CheckPoint;
import run.chronicle.account.dto.Transfer;

public interface AccountsIn {
    // Processes an account status request, potentially throwing an exception if the request is invalid.
    void accountStatus(AccountStatus accountStatus) throws InvalidMarshallableException;

    // Processes a transfer request between accounts.
    void transfer(Transfer transfer);

    // Processes a checkpoint event, used to mark a specific point in the processing sequence.
    void checkPoint(CheckPoint checkPoint);
}
