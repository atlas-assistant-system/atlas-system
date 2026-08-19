package sharedkernel.archunit;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

public final class LayerRules {

    private static final String KERNEL = "sharedkernel..";

    private LayerRules() {}

    private static DescribedPredicate<JavaClass> outsideKernelIn(String... packages) {
        return resideInAnyPackage(packages).and(not(resideInAPackage(KERNEL)));
    }

    @ArchTest
    public static final ArchRule domainDependsOnNoOuterLayer = noClasses()
        .that()
        .resideInAPackage("..domain..")
        .and()
        .resideOutsideOfPackage(KERNEL)
        .should()
        .dependOnClassesThat(outsideKernelIn("..application..", "..infrastructure..", "..presentation.."))
        .because("el dominio es el anillo interior: no conoce casos de uso, persistencia ni HTTP")
        .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule applicationDependsOnNoOuterLayer = noClasses()
        .that()
        .resideInAPackage("..application..")
        .and()
        .resideOutsideOfPackage(KERNEL)
        .should()
        .dependOnClassesThat(outsideKernelIn("..infrastructure..", "..presentation.."))
        .because("Application define puertos; quien los implementa es Infrastructure, nunca al reves")
        .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule infrastructureDoesNotDependOnPresentation = noClasses()
        .that()
        .resideInAPackage("..infrastructure..")
        .and()
        .resideOutsideOfPackage(KERNEL)
        .should()
        .dependOnClassesThat(outsideKernelIn("..presentation.."))
        .because("la persistencia no sabe que existe una interfaz de usuario")
        .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule domainDoesNotUseJdbc = noClasses()
        .that()
        .resideInAPackage("..domain..")
        .and()
        .resideOutsideOfPackage(KERNEL)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("java.sql..", "javax.sql..")
        .because("el dominio no conoce la tecnologia de persistencia")
        .allowEmptyShould(true);
}
