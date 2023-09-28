/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.api;

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.wire.utils.dto.ExecutionReport;
import net.openhft.chronicle.wire.utils.dto.OrderCancelReject;

public interface OMSOut {
    @MethodId(11)
    void executionReport(ExecutionReport er);

    @MethodId(12)
    void orderCancelReject(OrderCancelReject ocr);
}
