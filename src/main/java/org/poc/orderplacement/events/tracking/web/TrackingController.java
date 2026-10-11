package org.poc.orderplacement.events.tracking.web;

import java.util.UUID;

import org.poc.orderplacement.events.tracking.OrderNotFoundException;
import org.poc.orderplacement.events.tracking.OrderTrackingService;
import org.poc.orderplacement.events.tracking.OrderTrackingView;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TrackingController {

    private final OrderTrackingService orderTrackingService;

    public TrackingController(final OrderTrackingService orderTrackingService) {
        this.orderTrackingService = orderTrackingService;
    }

    @GetMapping("/orders/{id}")
    public String showOrder(@PathVariable final UUID id, final Model model) {
        final OrderTrackingView tracking = orderTrackingService.findTracking(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        model.addAttribute("order", tracking);
        return "orders/show";
    }
}
