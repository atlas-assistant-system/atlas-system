package sharedkernel.archunit;

import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import sharedkernel.application.cqrs.Command;
import sharedkernel.application.cqrs.CommandHandler;
import sharedkernel.application.cqrs.Query;
import sharedkernel.application.cqrs.QueryHandler;
import sharedkernel.domain.ddd.ValueObject;
import sharedkernel.domain.events.DomainEvent;

public final class DddRules {

    private static final String KERNEL = "sharedkernel..";

    private DddRules() {}

    @ArchTest
    public static final ArchRule domainEventsAreRecords = classes()
        .that()
        .implement(DomainEvent.class)
        .and()
        .doNotHaveModifier(ABSTRACT)
        .and()
        .resideOutsideOfPackage(KERNEL)
        .should()
        .beRecords()
        .because("un evento es un hecho inmutable ya ocurrido")
        .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule valueObjectsAreRecords = classes()
        .that()
        .resideInAPackage("..domain..vos..")
        .and()
        .doNotHaveModifier(ABSTRACT)
        .and()
        .areNotInterfaces()
        .should()
        .beRecords()
        .andShould()
        .implement(ValueObject.class)
        .because("los Value Objects son records con igualdad estructural, marcados para poder verificarlos")
        .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule commandsAreRecords = classes()
        .that()
        .implement(Command.class)
        .and()
        .doNotHaveModifier(ABSTRACT)
        .and()
        .resideOutsideOfPackage(KERNEL)
        .should()
        .beRecords()
        .because("un Command es un mensaje inmutable")
        .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule queriesAreRecords = classes()
        .that()
        .implement(Query.class)
        .and()
        .doNotHaveModifier(ABSTRACT)
        .and()
        .resideOutsideOfPackage(KERNEL)
        .should()
        .beRecords()
        .because("una Query es un mensaje inmutable")
        .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule commandHandlersAreNamedConsistently = classes()
        .that()
        .implement(CommandHandler.class)
        .and()
        .resideOutsideOfPackage(KERNEL)
        .should()
        .haveSimpleNameEndingWith("CommandHandler")
        .because("el nombre debe delatar el rol sin abrir el fichero")
        .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule queryHandlersAreNamedConsistently = classes()
        .that()
        .implement(QueryHandler.class)
        .and()
        .resideOutsideOfPackage(KERNEL)
        .should()
        .haveSimpleNameEndingWith("QueryHandler")
        .because("el nombre debe delatar el rol sin abrir el fichero")
        .allowEmptyShould(true);
}
