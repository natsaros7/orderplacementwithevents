package org.poc.orderplacement.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Safety net alongside {@code spring.modulith.events.republish-outstanding-events-on-restart}: that
 * property only resubmits outstanding publications found at startup, so a listener that fails
 * without the app restarting would otherwise leave its publication incomplete indefinitely. Picks
 * up anything still incomplete after {@link EventResubmissionProperties#minAge()}.
 */
@Component
public class StaleEventPublicationResubmitter {

    private static final Logger log = LoggerFactory.getLogger(StaleEventPublicationResubmitter.class);

    private final IncompleteEventPublications incompleteEventPublications;
    private final EventResubmissionProperties properties;

    public StaleEventPublicationResubmitter(final IncompleteEventPublications incompleteEventPublications,
            final EventResubmissionProperties properties) {
        this.incompleteEventPublications = incompleteEventPublications;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.events.resubmission.interval:PT1M}")
    public void resubmitStalePublications() {
        log.debug("Resubmitting incomplete event publications older than {}", properties.minAge());
        incompleteEventPublications.resubmitIncompletePublicationsOlderThan(properties.minAge());
    }
}
