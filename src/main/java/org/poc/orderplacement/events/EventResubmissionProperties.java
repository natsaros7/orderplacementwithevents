package org.poc.orderplacement.events;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** How old an incomplete event publication must be before {@link StaleEventPublicationResubmitter} retries it. */
@ConfigurationProperties(prefix = "app.events.resubmission")
public record EventResubmissionProperties(@DefaultValue("5m") Duration minAge) {
}
