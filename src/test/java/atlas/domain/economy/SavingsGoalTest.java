package atlas.domain.economy;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.economy.events.SavingsGoalAbandonedEvent;
import atlas.domain.economy.events.SavingsGoalChangedEvent;
import atlas.domain.economy.events.SavingsGoalSetEvent;
import atlas.domain.economy.vos.GoalName;
import atlas.domain.economy.vos.Money;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SavingsGoalTest {

    private static final SavingsGoalId ID = SavingsGoalId.of(5);
    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final LocalDate NEXT_JUNE = LocalDate.of(2027, 6, 30);
    private static final Money THREE_THOUSAND = Money.ofCents(300000).value();

    @Test
    void shouldSetTheGoalWithEverythingItWasGiven() {
        var goal = aGoal();

        assertThat(goal.id()).isEqualTo(ID);
        assertThat(goal.name().value()).isEqualTo("Viaje a Japon");
        assertThat(goal.target()).isEqualTo(THREE_THOUSAND);
        assertThat(goal.deadline()).isEqualTo(NEXT_JUNE);
        assertThat(goal.pendingEvents()).containsExactly(new SavingsGoalSetEvent(ID, NOW));
    }

    @Test
    void shouldRefuseADeadlineThatHasAlreadyPassed() {
        var result = SavingsGoal.set(ID, aName(), THREE_THOUSAND, TODAY.minusDays(1), TODAY, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(SavingsGoalErrors.DEADLINE_MUST_BE_AHEAD);
    }

    @Test
    void shouldRefuseADeadlineOfToday() {
        var result = SavingsGoal.set(ID, aName(), THREE_THOUSAND, TODAY, TODAY, NOW);

        assertThat(result.error()).isEqualTo(SavingsGoalErrors.DEADLINE_MUST_BE_AHEAD);
    }

    @Test
    void shouldChangeNameTargetAndDeadline() {
        var goal = aGoal();
        goal.clearEvents();
        var raised = Money.ofCents(500000).value();
        var later = LocalDate.of(2027, 12, 31);

        var result = goal.change(GoalName.create("Viaje a Corea").value(), raised, later, TODAY, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(goal.name().value()).isEqualTo("Viaje a Corea");
        assertThat(goal.target()).isEqualTo(raised);
        assertThat(goal.deadline()).isEqualTo(later);
        assertThat(goal.pendingEvents()).containsExactly(new SavingsGoalChangedEvent(ID, NOW));
    }

    @Test
    void shouldRefuseToMoveTheDeadlineIntoThePast() {
        var goal = aGoal();

        var result = goal.change(aName(), THREE_THOUSAND, TODAY.minusDays(1), TODAY, NOW);

        assertThat(result.error()).isEqualTo(SavingsGoalErrors.DEADLINE_MUST_BE_AHEAD);
        assertThat(goal.deadline()).isEqualTo(NEXT_JUNE);
    }

    @Test
    void shouldRegisterAbandonedEventWhenAbandoned() {
        var goal = aGoal();
        goal.clearEvents();

        var result = goal.abandon(NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(goal.pendingEvents()).containsExactly(new SavingsGoalAbandonedEvent(ID, NOW));
    }

    @Test
    void shouldNotRegisterAnyEventWhenRehydratedFromStorage() {
        var goal = SavingsGoal.rehydrate(ID, aName(), THREE_THOUSAND, NEXT_JUNE);

        assertThat(goal.pendingEvents()).isEmpty();
        assertThat(goal.deadline()).isEqualTo(NEXT_JUNE);
    }

    private static SavingsGoal aGoal() {
        return SavingsGoal.set(ID, aName(), THREE_THOUSAND, NEXT_JUNE, TODAY, NOW).value();
    }

    private static GoalName aName() {
        return GoalName.create("Viaje a Japon").value();
    }
}
