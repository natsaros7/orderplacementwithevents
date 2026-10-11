package org.poc.orderplacement.events.order;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OrderNotFoundException(final UUID orderId) {
        super("No order found with id " + orderId);
    }
}
