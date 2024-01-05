/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.api;

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.wire.utils.dto.ExecutionReport;
import net.openhft.chronicle.wire.utils.dto.OrderCancelReject;

/**
 * Interface representing the output operations for an Order Management System (OMS).
 * It defines methods for sending execution reports and order cancel rejections.
 */
public interface OMSOut {
    /**
     * Sends an execution report.
     *
     * @param er The ExecutionReport object containing details of the executed order.
     */
    @MethodId(11)
    void executionReport(ExecutionReport er);

    /**
     * Sends an order cancel rejection.
     *
     * @param ocr The OrderCancelReject object containing details of the rejected cancellation.
     */
    @MethodId(12)
    void orderCancelReject(OrderCancelReject ocr);
}
