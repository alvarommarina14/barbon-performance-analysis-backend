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

    private static final String CONTROLLER = BASE + "..controller..";
    private static final String DTO = BASE + "..dto..";
    private static final String APPLICATION = BASE + "..application..";
    private static final String PERSISTENCE = BASE + "..persistence..";
    private static final String SECURITY = BASE + "..security..";
    private static final String DOMAIN = BASE + "..domain..";

    @ArchTest
    static final ArchRule domainKnowsNothingAboutTheLayersAroundIt = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat()
            .resideInAnyPackage(CONTROLLER, DTO, APPLICATION, PERSISTENCE, SECURITY);

    @ArchTest
    static final ArchRule useCasesDoNotReachIntoAdapters = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(CONTROLLER, PERSISTENCE);

    @ArchTest
    static final ArchRule controllersGoThroughTheApplicationLayer = noClasses()
            .that().resideInAPackage(CONTROLLER)
            .should().dependOnClassesThat().resideInAPackage(PERSISTENCE);

    @ArchTest
    static final ArchRule persistenceDoesNotLookUpwards = noClasses()
            .that().resideInAPackage(PERSISTENCE)
            .should().dependOnClassesThat().resideInAnyPackage(CONTROLLER, DTO, APPLICATION);

    // The whole point of the ports: the rest of the codebase talks to interfaces it owns, and
    // only the adapters know Spring Data exists.
    @ArchTest
    static final ArchRule springDataStaysInsidePersistence = noClasses()
            .that().resideOutsideOfPackage(PERSISTENCE)
            .should().dependOnClassesThat().resideInAPackage("org.springframework.data..");

    @ArchTest
    static final ArchRule modulesAreFreeOfCycles = slices()
            .matching(BASE + ".(*)..")
            .should().beFreeOfCycles();
}
