package org.poc.orderplacement.events.order.web;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.poc.orderplacement.events.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void should_redirectToOrder_when_placeOrderSucceeds() throws Exception {
        final UUID orderId = UUID.randomUUID();
        when(orderService.placeOrder(any())).thenReturn(orderId);

        mockMvc.perform(post("/orders")
                        .param("productCode", "SKU-1")
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/" + orderId));

        verify(orderService).placeOrder(any());
    }

    @Test
    void should_rerenderFormWithErrors_when_placeOrderFailsValidation() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("productCode", "")
                        .param("quantity", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/new"))
                .andExpect(model().attributeHasFieldErrors("command", "productCode", "quantity"));

        verifyNoInteractions(orderService);
    }

    @Test
    void should_rerenderFormWithErrors_when_productCodeExceedsMaxLength() throws Exception {
        final String tooLong = "A".repeat(65);

        mockMvc.perform(post("/orders")
                        .param("productCode", tooLong)
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/new"))
                .andExpect(model().attributeHasFieldErrors("command", "productCode"));

        verifyNoInteractions(orderService);
    }
}
