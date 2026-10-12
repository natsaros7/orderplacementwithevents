/**
 * Assembles the cross-feature read model. The only module allowed to depend on {@code order},
 * {@code inventory} and {@code shipping} at once.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"order", "inventory", "shipping"})
package org.poc.orderplacement.events.tracking;
