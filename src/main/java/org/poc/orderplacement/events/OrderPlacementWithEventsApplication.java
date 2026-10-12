package org.poc.orderplacement.events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OrderPlacementWithEventsApplication {

    public static void main(final String[] args) {
        SpringApplication.run(OrderPlacementWithEventsApplication.class, args);
    }
}
