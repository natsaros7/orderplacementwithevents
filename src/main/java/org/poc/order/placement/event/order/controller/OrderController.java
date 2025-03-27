package org.poc.order.placement.event.order.controller;

import org.poc.order.placement.event.order.entities.Order;
import org.poc.order.placement.event.order.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Display the order form
    @GetMapping("/placeOrder")
    public String showOrderForm(Model model) {
        model.addAttribute("order", new Order());
        return "placeOrder";
    }

    // Handle the order form submission
    @PostMapping("/orders")
    public String placeOrder(Order order, Model model) {
        Order placedOrder = orderService.placeOrder(order);
        model.addAttribute("orderId", placedOrder.getId());
        return "redirect:/inventory"; // Redirect as per your logic
    }

    // Inventory page
    @GetMapping("/inventory")
    public String showInventoryUpdate(Model model) {
        // Assume the model has been populated with the order ID
        return "inventory";
    }

    // Shipping page
    @GetMapping("/shipping")
    public String showShippingPreparation(Model model) {
        // Assume the model has been populated with the order ID
        return "shipping";
    }
}
