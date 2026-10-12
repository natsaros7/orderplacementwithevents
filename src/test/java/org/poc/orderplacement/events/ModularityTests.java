package org.poc.orderplacement.events;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Replaces the hand-rolled ArchUnit {@code ArchitectureTest}: {@link ApplicationModules#verify()}
 * checks the same invariants (no package cycles; each module's dependencies match its
 * {@code @ApplicationModule(allowedDependencies = ...)} declaration — see each module's
 * {@code package-info.java}) directly from the module metadata, instead of a separately
 * maintained set of rules that could drift from it.
 */
class ModularityTests {

    private static final ApplicationModules MODULES = ApplicationModules.of(OrderPlacementWithEventsApplication.class);

    @Test
    void should_verifyModuleStructure_when_analyzed() {
        MODULES.verify();
    }

    @Test
    void should_writeModuleDocumentation_when_analyzed() {
        // Diagrams/canvases under target/ (gitignored) — not committed; regenerate with this test.
        new Documenter(MODULES).writeDocumentation();
    }
}
