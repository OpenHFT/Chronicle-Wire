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

import net.openhft.chronicle.wire.utils.ErrorListener;
import run.chronicle.account.dto.*;

public interface AccountsOut extends ErrorListener {
    // Sends a successful account status response.
    void accountStatusOK(AccountStatusOK accountStatusOK);

    // Sends a failed account status response, indicating a problem with the account status request.
    void accountStatusFailed(AccountStatusFailed accountStatusFailed);

    // Sends a successful transfer response, indicating the transfer was completed successfully.
    void transferOK(TransferOK transferOK);

    // Sends a failed transfer response, indicating a problem occurred during the transfer.
    void transferFailed(TransferFailed transferFailed);

    // Marks the start of a checkpoint in the processing sequence.
    void startCheckpoint(CheckPoint checkPoint);

    // Marks the end of a checkpoint in the processing sequence.
    void endCheckpoint(CheckPoint checkPoint);
}
