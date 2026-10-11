package org.poc.orderplacement.events.order.web;

import java.util.UUID;

import org.poc.orderplacement.events.order.OrderNotFoundException;
import org.poc.orderplacement.events.order.OrderService;
import org.poc.orderplacement.events.order.OrderStatusQueryService;
import org.poc.orderplacement.events.order.OrderStatusView;
import org.poc.orderplacement.events.order.PlaceOrderCommand;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final OrderStatusQueryService orderStatusQueryService;

    public OrderController(final OrderService orderService, final OrderStatusQueryService orderStatusQueryService) {
        this.orderService = orderService;
        this.orderStatusQueryService = orderStatusQueryService;
    }

    @GetMapping("/orders/new")
    public String newOrderForm(final Model model) {
        if (!model.containsAttribute("command")) {
            model.addAttribute("command", new PlaceOrderCommand("", 0));
        }
        return "orders/new";
    }

    @PostMapping("/orders")
    public String placeOrder(@Valid @ModelAttribute("command") final PlaceOrderCommand command,
            final BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "orders/new";
        }
        final UUID orderId = orderService.placeOrder(command);
        return "redirect:/orders/" + orderId;
    }

    @GetMapping("/orders/{id}")
    public String showOrder(@PathVariable final UUID id, final Model model) {
        final OrderStatusView status = orderStatusQueryService.findStatus(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        model.addAttribute("order", status);
        return "orders/show";
    }
}
