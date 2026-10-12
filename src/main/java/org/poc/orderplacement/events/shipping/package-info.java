/** Reacts to {@code order}'s {@code OrderPlaced} event; owns its own shipment table. */
@org.springframework.modulith.ApplicationModule(allowedDependencies = "order")
package org.poc.orderplacement.events.shipping;
