package org.poc.orderplacement.events.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Web-facing input for placing an order; validated with Bean Validation at the controller boundary. */
public record PlaceOrderCommand(

        @NotBlank(message = "Product code is required")
        String productCode,

        @Positive(message = "Quantity must be greater than zero")
        int quantity) {
}
