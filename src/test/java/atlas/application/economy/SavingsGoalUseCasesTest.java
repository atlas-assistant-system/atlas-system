package atlas.application.economy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.economy.commands.abandonsavingsgoal.AbandonSavingsGoalCommand;
import atlas.application.economy.commands.abandonsavingsgoal.AbandonSavingsGoalCommandHandler;
import atlas.application.economy.commands.changesavingsgoal.ChangeSavingsGoalCommand;
import atlas.application.economy.commands.changesavingsgoal.ChangeSavingsGoalCommandHandler;
import atlas.application.economy.commands.setsavingsgoal.SetSavingsGoalCommand;
import atlas.application.economy.commands.setsavingsgoal.SetSavingsGoalCommandHandler;
import atlas.application.economy.ports.EconomyUnitOfWork;
import atlas.application.economy.ports.MovementReadModel;
import atlas.application.economy.ports.SavingsGoalReadModel;
import atlas.application.economy.ports.SavingsGoalRepository;
import atlas.application.economy.queries.listsavingsgoals.ListSavingsGoalsQuery;
import atlas.application.economy.queries.listsavingsgoals.ListSavingsGoalsQueryHandler;
import atlas.domain.economy.MovementErrors;
import atlas.domain.economy.SavingsGoal;
import atlas.domain.economy.SavingsGoalErrors;
import atlas.domain.economy.SavingsGoalId;
import atlas.domain.economy.events.SavingsGoalAbandonedEvent;
import atlas.domain.economy.events.SavingsGoalSetEvent;
import atlas.domain.economy.services.SavingsProjection;
import atlas.domain.economy.vos.GoalName;
import atlas.domain.economy.vos.Money;
import atlas.support.builders.UnitOfWorkStub;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SavingsGoalUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final SavingsGoalId ID = SavingsGoalId.of(5);
    private static final LocalDate NEXT_FEBRUARY = LocalDate.of(2027, 2, 28);

    private final EconomyUnitOfWork unitOfWork = mock(EconomyUnitOfWork.class);
    private final SavingsGoalRepository goals = mock(SavingsGoalRepository.class);
    private final SavingsGoalReadModel readModel = mock(SavingsGoalReadModel.class);
    private final MovementReadModel movements = mock(MovementReadModel.class);

    private final SetSavingsGoalCommandHandler set = new SetSavingsGoalCommandHandler(unitOfWork, CLOCK);
    private final ChangeSavingsGoalCommandHandler change = new ChangeSavingsGoalCommandHandler(unitOfWork, CLOCK);
    private final AbandonSavingsGoalCommandHandler abandon = new AbandonSavingsGoalCommandHandler(unitOfWork, CLOCK);
    private final ListSavingsGoalsQueryHandler list =
        new ListSavingsGoalsQueryHandler(readModel, movements, new SavingsProjection(), CLOCK);

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.withGoals(unitOfWork, goals);
        when(goals.nextId()).thenReturn(ID);
    }

    @Test
    void shouldSetTheGoalAndReturnIt() {
        var result = set.handle(aCommand("Viaje a Japon", "3000.00", NEXT_FEBRUARY));

        assertThat(result.value().id()).isEqualTo("O00000005");
        assertThat(result.value().name()).isEqualTo("Viaje a Japon");
        assertThat(result.value().target()).isEqualByComparingTo("3000.00");
        assertThat(result.value().deadline()).isEqualTo(NEXT_FEBRUARY);

        var saved = ArgumentCaptor.forClass(SavingsGoal.class);
        verify(goals).create(saved.capture());
        assertThat(saved.getValue().pendingEvents()).containsExactly(new SavingsGoalSetEvent(ID, NOW));
    }

    @Test
    void shouldFailWhenTheGoalHasNoName() {
        var result = set.handle(aCommand("  ", "3000.00", NEXT_FEBRUARY));

        assertThat(result.error()).isEqualTo(SavingsGoalErrors.NAME_REQUIRED);
        verify(goals, never()).create(any());
    }

    @Test
    void shouldFailWhenTheTargetIsNotPositive() {
        var result = set.handle(aCommand("Viaje a Japon", "0", NEXT_FEBRUARY));

        assertThat(result.error()).isEqualTo(MovementErrors.AMOUNT_MUST_BE_POSITIVE);
        verify(goals, never()).create(any());
    }

    @Test
    void shouldFailWhenTheDeadlineHasPassed() {
        var result = set.handle(aCommand("Viaje a Japon", "3000.00", LocalDate.of(2026, 8, 1)));

        assertThat(result.error()).isEqualTo(SavingsGoalErrors.DEADLINE_MUST_BE_AHEAD);
        verify(goals, never()).create(any());
    }

    @Test
    void shouldChangeTheGoal() {
        when(goals.get(ID)).thenReturn(Optional.of(aGoal()));

        var result = change.handle(new ChangeSavingsGoalCommand(
            ID, "Viaje a Corea", new BigDecimal("5000.00"), NEXT_FEBRUARY));

        assertThat(result.value().name()).isEqualTo("Viaje a Corea");
        assertThat(result.value().target()).isEqualByComparingTo("5000.00");
        verify(goals).update(any(SavingsGoal.class));
    }

    @Test
    void shouldFailWhenChangingAGoalThatDoesNotExist() {
        when(goals.get(ID)).thenReturn(Optional.empty());

        var result = change.handle(new ChangeSavingsGoalCommand(
            ID, "Viaje a Corea", new BigDecimal("5000.00"), NEXT_FEBRUARY));

        assertThat(result.error()).isEqualTo(SavingsGoalErrors.notFound(ID));
        verify(goals, never()).update(any());
    }

    @Test
    void shouldAbandonTheGoal() {
        var goal = aGoal();
        when(goals.get(ID)).thenReturn(Optional.of(goal));

        var result = abandon.handle(new AbandonSavingsGoalCommand(ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(goal.pendingEvents()).containsExactly(new SavingsGoalAbandonedEvent(ID, NOW));
        verify(goals).delete(goal);
    }

    @Test
    void shouldFailWhenAbandoningAGoalThatDoesNotExist() {
        when(goals.get(ID)).thenReturn(Optional.empty());

        assertThat(abandon.handle(new AbandonSavingsGoalCommand(ID)).error())
            .isEqualTo(SavingsGoalErrors.notFound(ID));
        verify(goals, never()).delete(any());
    }

    @Test
    void shouldProjectEachGoalFromTheLastSixWholeMonths() {
        when(readModel.findAll()).thenReturn(List.of(aGoal()));
        when(movements.balanceBetween(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 7, 31)))
            .thenReturn(new MovementReadModel.Balance(1200000, 600000));

        var result = list.handle(new ListSavingsGoalsQuery());

        assertThat(result.value()).singleElement().satisfies(goal -> {
            assertThat(goal.name()).isEqualTo("Viaje a Japon");
            assertThat(goal.monthlySaving()).isEqualByComparingTo("1000.00");
            assertThat(goal.monthsRemaining()).isEqualTo(6);
            assertThat(goal.projected()).isEqualByComparingTo("6000.00");
            assertThat(goal.reachable()).isTrue();
        });
    }

    @Test
    void shouldSayHowMuchTheRateWouldHaveToRiseWhenTheGoalIsOutOfReach() {
        when(readModel.findAll()).thenReturn(List.of(aGoal()));
        when(movements.balanceBetween(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 7, 31)))
            .thenReturn(new MovementReadModel.Balance(600000, 540000));

        var result = list.handle(new ListSavingsGoalsQuery());

        assertThat(result.value()).singleElement().satisfies(goal -> {
            assertThat(goal.monthlySaving()).isEqualByComparingTo("100.00");
            assertThat(goal.projected()).isEqualByComparingTo("600.00");
            assertThat(goal.reachable()).isFalse();
            assertThat(goal.requiredMonthly()).isEqualByComparingTo("500.00");
        });
    }

    @Test
    void shouldPutTheClosestDeadlineFirst() {
        when(readModel.findAll()).thenReturn(List.of(
            goalNamed("Coche", LocalDate.of(2028, 1, 31)),
            goalNamed("Viaje", LocalDate.of(2027, 1, 31))));
        when(movements.balanceBetween(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 7, 31)))
            .thenReturn(new MovementReadModel.Balance(0, 0));

        var result = list.handle(new ListSavingsGoalsQuery());

        assertThat(result.value()).extracting("name").containsExactly("Viaje", "Coche");
    }

    @Test
    void shouldNotAskForTheBalanceWhenThereIsNoGoal() {
        when(readModel.findAll()).thenReturn(List.of());

        assertThat(list.handle(new ListSavingsGoalsQuery()).value()).isEmpty();
        verify(movements, never()).balanceBetween(any(), any());
    }

    private static SetSavingsGoalCommand aCommand(String name, String target, LocalDate deadline) {
        return new SetSavingsGoalCommand(name, new BigDecimal(target), deadline);
    }

    private static SavingsGoal aGoal() {
        return goalNamed("Viaje a Japon", NEXT_FEBRUARY);
    }

    private static SavingsGoal goalNamed(String name, LocalDate deadline) {
        return SavingsGoal.rehydrate(
            ID, GoalName.create(name).value(), Money.ofCents(300000).value(), deadline);
    }
}
