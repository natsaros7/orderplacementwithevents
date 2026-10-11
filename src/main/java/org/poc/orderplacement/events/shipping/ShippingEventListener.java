package org.poc.orderplacement.events.shipping;

import org.poc.orderplacement.events.order.OrderPlaced;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ShippingEventListener {

    private final ShippingService shippingService;

    public ShippingEventListener(final ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    /**
     * AFTER_COMMIT: only prepare shipping once the order is durably committed. If the app crashes
     * between commit and this callback running, the shipment is lost — see README "Design notes".
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(final OrderPlaced event) {
        shippingService.prepare(event.orderId());
    }
}
