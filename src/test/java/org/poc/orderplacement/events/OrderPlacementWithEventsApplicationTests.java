package org.poc.orderplacement.events;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderPlacementWithEventsApplicationTests {

    @Test
    void should_loadContext_when_applicationStarts() {
    }
}
