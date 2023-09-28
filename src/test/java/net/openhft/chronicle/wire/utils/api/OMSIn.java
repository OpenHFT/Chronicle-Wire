/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.api;

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.wire.utils.dto.CancelOrderRequest;
import net.openhft.chronicle.wire.utils.dto.NewOrderSingle;

public interface OMSIn {
    @MethodId(1)
    void newOrderSingle(NewOrderSingle nos);

    @MethodId(2)
    void cancelOrderRequest(CancelOrderRequest cor);
}
