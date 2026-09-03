package com.barbonperformance.courtproof.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = ArchitectureTest.BASE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String BASE = "com.barbonperformance.courtproof";

    private static final String DOMAIN = BASE + "..domain..";
    private static final String APPLICATION = BASE + "..application..";
    private static final String INFRASTRUCTURE = BASE + "..infrastructure..";
    private static final String WEB = BASE + "..infrastructure.web..";
    private static final String PERSISTENCE = BASE + "..infrastructure.persistence..";

    @ArchTest
    static final ArchRule domainKnowsNothingAboutTheLayersAroundIt = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE);

    @ArchTest
    static final ArchRule useCasesNeverTouchAnAdapter = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE);

    // Driven adapters are called through a port; only the driving side (web) calls a use case.
    // Written as "everything under infrastructure except web" so a new adapter package —
    // gateway/ in phase 2 — is covered without touching this rule.
    @ArchTest
    static final ArchRule drivenAdaptersImplementPortsInsteadOfCallingUseCases = noClasses()
            .that().resideInAPackage(INFRASTRUCTURE).and().resideOutsideOfPackage(WEB)
            .should().dependOnClassesThat().resideInAPackage(APPLICATION);

    @ArchTest
    static final ArchRule webAdaptersGoThroughTheApplicationLayer = noClasses()
            .that().resideInAPackage(WEB)
            .should().dependOnClassesThat().resideInAPackage(PERSISTENCE);

    @ArchTest
    static final ArchRule springDataStaysInsidePersistence = noClasses()
            .that().resideOutsideOfPackage(PERSISTENCE)
            .should().dependOnClassesThat().resideInAPackage("org.springframework.data..");

    @ArchTest
    static final ArchRule modulesAreFreeOfCycles = slices()
            .matching(BASE + ".(*)..")
            .should().beFreeOfCycles();
}
