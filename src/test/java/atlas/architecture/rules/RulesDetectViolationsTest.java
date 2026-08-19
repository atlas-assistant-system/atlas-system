package atlas.architecture.rules;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class RulesDetectViolationsTest {

    private static final JavaClasses OFFENDING = new ClassFileImporter().importPackages("fixtures.bad");

    private static final JavaClasses COMPLIANT = new ClassFileImporter().importPackages("fixtures.good");

    private static void expectViolationIn(ArchRule rule, String offendingClass) {
        assertThatThrownBy(() -> rule.check(OFFENDING))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining(offendingClass)
            .hasMessageNotContaining("failed to check any classes");
    }

    private static void expectNoViolation(ArchRule rule) {
        assertThatCode(() -> rule.check(COMPLIANT)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectDomainThatReachesIntoApplication() {
        expectViolationIn(LayerRules.domainDependsOnNoOuterLayer, "Offender");
    }

    @Test
    void shouldRejectDomainThatLogs() {
        expectViolationIn(DomainPurityRules.domainDoesNotLog, "Offender");
    }

    @Test
    void shouldRejectDomainThatReadsTheClock() {
        expectViolationIn(DomainPurityRules.domainDoesNotReadTheClock, "Offender");
    }

    @Test
    void shouldRejectDomainThatUsesRandomness() {
        expectViolationIn(DomainPurityRules.domainDoesNotUseRandomness, "Offender");
    }

    @Test
    void shouldRejectDomainEventThatIsNotARecord() {
        expectViolationIn(DddRules.domainEventsAreRecords, "TicketOpened");
    }

    @Test
    void shouldRejectValueObjectThatIsNotARecord() {
        expectViolationIn(DddRules.valueObjectsAreRecords, "Amount");
    }

    @Test
    void shouldAcceptCompliantDomainWhenLayersAreRespected() {
        expectNoViolation(LayerRules.domainDependsOnNoOuterLayer);
        expectNoViolation(LayerRules.domainDoesNotUseJdbc);
    }

    @Test
    void shouldAcceptCompliantDomainWhenItIsPure() {
        expectNoViolation(DomainPurityRules.domainDoesNotLog);
        expectNoViolation(DomainPurityRules.domainDoesNotReadTheClock);
        expectNoViolation(DomainPurityRules.domainDoesNotUseRandomness);
    }

    @Test
    void shouldAcceptCompliantDomainWhenBuildingBlocksAreRecords() {
        expectNoViolation(DddRules.domainEventsAreRecords);
        expectNoViolation(DddRules.valueObjectsAreRecords);
    }
}
