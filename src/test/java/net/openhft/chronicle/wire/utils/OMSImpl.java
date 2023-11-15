/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.wire.utils.api.OMSIn;
import net.openhft.chronicle.wire.utils.api.OMSOut;
import net.openhft.chronicle.wire.utils.dto.CancelOrderRequest;
import net.openhft.chronicle.wire.utils.dto.ExecutionReport;
import net.openhft.chronicle.wire.utils.dto.NewOrderSingle;
import net.openhft.chronicle.wire.utils.dto.OrderCancelReject;

public class OMSImpl implements OMSIn {
    // OMSOut instance to send output messages
    private final OMSOut out;
    // ExecutionReport instance reused for sending new order responses
    private final ExecutionReport er = new ExecutionReport();
    // OrderCancelReject instance reused for sending cancel order responses
    private final OrderCancelReject ocr = new OrderCancelReject();

    // Constructor initializing with an OMSOut instance
    public OMSImpl(OMSOut out) {
        this.out = out;
    }

    // Handles incoming new order single requests
    @Override
    public void newOrderSingle(NewOrderSingle nos) {
        er.reset(); // Reset the execution report state
        // Generate a unique order ID based on the current time
        final long orderID = SystemTimeProvider.CLOCK.currentTimeNanos();
        // Populate the execution report with order details
        er.sender(nos.target())
                .target(nos.sender())
                .symbol(nos.symbol())
                .clOrdID(nos.clOrdID())
                .ordType(nos.ordType())
                .orderQty(nos.orderQty())
                .price(nos.price())
                .side(nos.side())
                .sendingTime(nos.sendingTime())
                .transactTime(nos.transactTime())
                .leavesQty(0) // Quantity remaining after the execution
                .orderID(orderID) // Unique order ID
                .text("Not ready"); // Status message
        out.executionReport(er); // Send the execution report to the output
    }

    // Handles incoming cancel order requests
    @Override
    public void cancelOrderRequest(CancelOrderRequest cor) {
        // Populate the order cancel reject message
        ocr.sender(cor.target())
                .target(cor.sender())
                .symbol(cor.symbol())
                .clOrdID(cor.clOrdID())
                .sendingTime(cor.sendingTime())
                .reason("No such order"); // Reason for order cancellation rejection
        out.orderCancelReject(ocr); // Send the cancel reject message to the output
    }
}
