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

public class AccountsImpl
        extends SelfDescribingMarshallable
        implements AccountsIn {
    private long id;
    private transient final AccountsOut out;
    public AccountsImpl(AccountsOut out) {
        this.out = out;
    }

    // use a primitive long map
    private final Map<Long, AccountStatus> accountsMap = new LinkedHashMap<>();

    // DTOs for events out
    private final AccountStatusOK accountStatusOK = new AccountStatusOK();
    private final AccountStatusFailed accountStatusFailed = new AccountStatusFailed();
    private final TransferOK transferOK = new TransferOK();
    private final TransferFailed transferFailed = new TransferFailed();

    public AccountsImpl id(long id) {
        this.id = id;
        return this;
    }

    @Override
    public void accountStatus(AccountStatus accountStatus) throws InvalidMarshallableException {
        if (accountStatus.target() != id) {
            sendAccountStatusFailed(accountStatus, "target mismatch");
            return;
        }
        if (!(accountStatus.amount() >= 0)) {
            sendAccountStatusFailed(accountStatus, "invalid amount");
            return;
        }
        Long account = accountStatus.account();
        if (accountsMap.containsKey(account)) {
            sendAccountStatusFailed(accountStatus, "account already exists");
            return;
        }
        // must take a copy of any data we want to retain
        accountsMap.put(account, accountStatus.deepCopy());
        sendAccountStatusOK(accountStatus);
    }

    @Override
    public void transfer(Transfer transfer) {
        if (transfer.target() != id) {
            sendTransferFailed(transfer, "target mismatch");
            return;
        }
        double amount = transfer.amount();
        if (!(amount > 0)) {
            sendTransferFailed(transfer, "invalid amount");
            return;
        }

        AccountStatus fromAccount = accountsMap.get(transfer.from());
        if (fromAccount == null) {
            sendTransferFailed(transfer, "from account doesn't exist");
            return;
        }
        if (fromAccount.currency() != transfer.currency()) {
            sendTransferFailed(transfer, "from account currency doesn't match");
            return;
        }
        if (fromAccount.amount() < amount) {
            sendTransferFailed(transfer, "insufficient funds");
            return;
        }
        AccountStatus toAccount = accountsMap.get(transfer.to());
        if (toAccount == null) {
            sendTransferFailed(transfer, "to account doesn't exist");
            return;
        }
        if (toAccount.currency() != transfer.currency()) {
            sendTransferFailed(transfer, "to account currency doesn't match");
            return;
        }
        // these changes need to be idempotent
        fromAccount.amount(fromAccount.amount() - amount);
        toAccount.amount(toAccount.amount() + amount);
        sendTransferOK(transfer);
    }

    @Override
    public void checkPoint(CheckPoint checkPoint) {
        if (checkPoint.target() != id)
            return; // ignored

        out.startCheckpoint(checkPoint);
        for (AccountStatus accountStatus : accountsMap.values()) {
            sendAccountStatusOK(accountStatus);
        }
        out.endCheckpoint(checkPoint);
    }

    private void sendAccountStatusFailed(AccountStatus accountStatus, String reason) {
        out.accountStatusFailed(accountStatusFailed
                .sender(id)
                .target(accountStatus.sender())
                .sendingTime(CLOCK.currentTimeNanos())
                .accountStatus(accountStatus)
                .reason(reason));
    }

    private void sendAccountStatusOK(AccountStatus accountStatus) {
        out.accountStatusOK(accountStatusOK
                .sender(id)
                .target(accountStatus.sender())
                .sendingTime(CLOCK.currentTimeNanos())
                .accountStatus(accountStatus));
    }

    private void sendTransferFailed(Transfer transfer, String reason) {
        out.transferFailed(transferFailed
                .sender(id)
                .target(transfer.sender())
                .sendingTime(CLOCK.currentTimeNanos())
                .transfer(transfer)
                .reason(reason));
    }

    private void sendTransferOK(Transfer transfer) {
        out.transferOK(transferOK
                .sender(id)
                .target(transfer.sender())
                .sendingTime(CLOCK.currentTimeNanos())
                .transfer(transfer));
    }
}
