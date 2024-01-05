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

package run.chronicle.account;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import run.chronicle.account.api.AccountsIn;
import run.chronicle.account.api.AccountsOut;
import run.chronicle.account.dto.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.openhft.chronicle.core.time.SystemTimeProvider.CLOCK;

/**
 * Implementation of AccountsIn interface. It processes account-related operations.
 */
public class AccountsImpl
        extends SelfDescribingMarshallable
        implements AccountsIn {
    private long id;
    private transient final AccountsOut out; // Interface for output operations

    /**
     * Constructor for AccountsImpl.
     *
     * @param out The output interface for account operations.
     */
    public AccountsImpl(AccountsOut out) {
        this.out = out;
    }

    // A map to store account statuses with the account number as the key.
    private final Map<Long, AccountStatus> accountsMap = new LinkedHashMap<>();

    // DTOs for different types of responses.
    private final AccountStatusOK accountStatusOK = new AccountStatusOK();
    private final AccountStatusFailed accountStatusFailed = new AccountStatusFailed();
    private final TransferOK transferOK = new TransferOK();
    private final TransferFailed transferFailed = new TransferFailed();

    /**
     * Sets the identifier for this instance.
     *
     * @param id The identifier to be set.
     * @return The current instance for chaining.
     */
    public AccountsImpl id(long id) {
        this.id = id;
        return this;
    }

    /**
     * Processes an account status request.
     * Validates the request and updates the account status map.
     * Sends appropriate responses based on the validation.
     *
     * @param accountStatus The AccountStatus object containing account details.
     * @throws InvalidMarshallableException If the account status is invalid.
     */
    @Override
    public void accountStatus(AccountStatus accountStatus) throws InvalidMarshallableException {
        // Validate target ID
        if (accountStatus.target() != id) {
            sendAccountStatusFailed(accountStatus, "target mismatch");
            return;
        }
        // Validate amount
        if (!(accountStatus.amount() >= 0)) {
            sendAccountStatusFailed(accountStatus, "invalid amount");
            return;
        }
        // Check for account existence
        Long account = accountStatus.account();
        if (accountsMap.containsKey(account)) {
            sendAccountStatusFailed(accountStatus, "account already exists");
            return;
        }
        // Store a copy of the account status
        accountsMap.put(account, accountStatus.deepCopy());
        // Send success response
        sendAccountStatusOK(accountStatus);
    }

    @Override
    public void transfer(Transfer transfer) {
        // Check if the transfer is intended for this instance
        if (transfer.target() != id) {
            sendTransferFailed(transfer, "target mismatch");
            return;
        }

        // Validate transfer amount
        double amount = transfer.amount();
        if (!(amount > 0)) {
            sendTransferFailed(transfer, "invalid amount");
            return;
        }

        // Validate 'from' account
        AccountStatus fromAccount = accountsMap.get(transfer.from());
        if (fromAccount == null) {
            sendTransferFailed(transfer, "from account doesn't exist");
            return;
        }
        // Check currency compatibility for 'from' account
        if (fromAccount.currency() != transfer.currency()) {
            sendTransferFailed(transfer, "from account currency doesn't match");
            return;
        }
        // Check for sufficient funds in 'from' account
        if (fromAccount.amount() < amount) {
            sendTransferFailed(transfer, "insufficient funds");
            return;
        }

        // Validate 'to' account
        AccountStatus toAccount = accountsMap.get(transfer.to());
        if (toAccount == null) {
            sendTransferFailed(transfer, "to account doesn't exist");
            return;
        }
        // Check currency compatibility for 'to' account
        if (toAccount.currency() != transfer.currency()) {
            sendTransferFailed(transfer, "to account currency doesn't match");
            return;
        }

        // Perform transfer operation, ensuring idempotency
        fromAccount.amount(fromAccount.amount() - amount);
        toAccount.amount(toAccount.amount() + amount);
        sendTransferOK(transfer);
    }

    @Override
    public void checkPoint(CheckPoint checkPoint) {
        // Ignore checkpoint if target doesn't match
        if (checkPoint.target() != id)
            return;

        // Begin checkpoint process
        out.startCheckpoint(checkPoint);

        // Send OK status for each account
        for (AccountStatus accountStatus : accountsMap.values()) {
            sendAccountStatusOK(accountStatus);
        }

        // End checkpoint process
        out.endCheckpoint(checkPoint);
    }

    // Helper methods to send various responses
    private void sendAccountStatusFailed(AccountStatus accountStatus, String reason) {
        // Construct and send a failed account status response
        out.accountStatusFailed(accountStatusFailed
                .sender(id)
                .target(accountStatus.sender())
                .sendingTime(CLOCK.currentTimeNanos())
                .accountStatus(accountStatus)
                .reason(reason));
    }

    private void sendAccountStatusOK(AccountStatus accountStatus) {
        // Construct and send a successful account status response
        out.accountStatusOK(accountStatusOK
                .sender(id)
                .target(accountStatus.sender())
                .sendingTime(CLOCK.currentTimeNanos())
                .accountStatus(accountStatus));
    }

    private void sendTransferFailed(Transfer transfer, String reason) {
        // Construct and send a failed transfer response
        out.transferFailed(transferFailed
                .sender(id)
                .target(transfer.sender())
                .sendingTime(CLOCK.currentTimeNanos())
                .transfer(transfer)
                .reason(reason));
    }

    private void sendTransferOK(Transfer transfer) {
        // Construct and send a successful transfer response
        out.transferOK(transferOK
                .sender(id)
                .target(transfer.sender())
                .sendingTime(CLOCK.currentTimeNanos())
                .transfer(transfer));
    }
}
