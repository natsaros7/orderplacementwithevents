package org.poc.orderplacement.events.tracking.web;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.poc.orderplacement.events.tracking.OrderTrackingService;
import org.poc.orderplacement.events.tracking.OrderTrackingView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TrackingController.class)
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderTrackingService orderTrackingService;

    @Test
    void should_returnBadRequest_when_orderIdIsNotAUuid() throws Exception {
        mockMvc.perform(get("/orders/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_returnNotFound_when_orderDoesNotExist() throws Exception {
        final UUID orderId = UUID.randomUUID();
        when(orderTrackingService.findTracking(orderId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void should_showOrder_when_orderExists() throws Exception {
        final UUID orderId = UUID.randomUUID();
        final OrderTrackingView tracking = new OrderTrackingView(orderId, "SKU-1", 2, Instant.now(), true, false);
        when(orderTrackingService.findTracking(orderId)).thenReturn(Optional.of(tracking));

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/show"))
                .andExpect(model().attribute("order", tracking))
                .andExpect(content().string(containsString("SKU-1")))
                .andExpect(content().string(containsString("reserved")))
                .andExpect(content().string(containsString("pending")));
    }
}
