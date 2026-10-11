package org.poc.orderplacement.events.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Guards the package-by-feature boundary: features may only talk to each other through the
 * narrow query services and the {@code OrderPlaced} event, never cyclically, and {@code order}
 * must stay unaware that inventory/shipping/tracking exist.
 */
@AnalyzeClasses(packages = "org.poc.orderplacement.events")
class ArchitectureTest {

    @ArchTest
    static final ArchRule features_should_be_free_of_cycles =
            slices().matching("org.poc.orderplacement.events.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule order_should_not_depend_on_other_features =
            noClasses().that().resideInAPackage("..order..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..inventory..", "..shipping..", "..tracking..");
}
