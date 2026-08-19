package atlas.architecture.rules;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.UUID;

public final class DomainPurityRules {

    private static final String KERNEL = "..sharedkernel..";

    private DomainPurityRules() {}

    @ArchTest
    public static final ArchRule domainDoesNotLog = noClasses()
        .that()
        .resideInAPackage("..domain..")
        .and()
        .resideOutsideOfPackage(KERNEL)
        .should()
        .callMethod(System.class, "getLogger", String.class)
        .orShould()
        .dependOnClassesThat()
        .resideInAnyPackage("java.util.logging..", "org.slf4j..", "org.apache.logging..", "ch.qos.logback..")
        .because("los fallos de negocio son valores (Result); quien los observa es el decorador del borde")
        .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule domainDoesNotReadTheClock = noClasses()
        .that()
        .resideInAPackage("..domain..")
        .and()
        .resideOutsideOfPackage(KERNEL)
        .should()
        .callMethodWhere(target(name("now")).and(target(owner(resideInAPackage("java.time..")))))
        .orShould()
        .callMethod(System.class, "currentTimeMillis")
        .because("el dominio recibe el Instant como parametro; el Clock lo inyecta Application")
        .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule domainDoesNotUseRandomness = noClasses()
        .that()
        .resideInAPackage("..domain..")
        .and()
        .resideOutsideOfPackage(KERNEL)
        .should()
        .callMethod(UUID.class, "randomUUID")
        .orShould()
        .callMethod(Math.class, "random")
        .because("un dominio con aleatoriedad no es testeable de forma determinista")
        .allowEmptyShould(true);
}
