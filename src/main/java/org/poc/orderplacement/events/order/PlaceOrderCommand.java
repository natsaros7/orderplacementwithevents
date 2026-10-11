package org.poc.orderplacement.events.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Web-facing input for placing an order; validated with Bean Validation at the controller boundary. */
public record PlaceOrderCommand(

        @NotBlank(message = "Product code is required")
        @Size(max = 64, message = "Product code must be at most 64 characters")
        String productCode,

        @Positive(message = "Quantity must be greater than zero")
        @Max(value = 10_000, message = "Quantity must be at most 10000")
        int quantity) {
}
