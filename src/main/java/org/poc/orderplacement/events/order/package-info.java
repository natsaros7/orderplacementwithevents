/**
 * Places orders and publishes {@link org.poc.orderplacement.events.order.OrderPlaced}. Knows
 * nothing about inventory, shipping or tracking — enforced below, not just by convention.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {})
package org.poc.orderplacement.events.order;
