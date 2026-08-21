package atlas.application.nutrition.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.nutrition.commands.adjustplan.AdjustPlanCommand;
import atlas.application.nutrition.commands.adjustplan.AdjustPlanCommandHandler;
import atlas.application.nutrition.commands.archiveplan.ArchivePlanCommand;
import atlas.application.nutrition.commands.archiveplan.ArchivePlanCommandHandler;
import atlas.application.nutrition.commands.defineplan.DefinePlanCommand;
import atlas.application.nutrition.commands.defineplan.DefinePlanCommandHandler;
import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.application.nutrition.ports.PlanRepository;
import atlas.domain.nutrition.NutritionErrors;
import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.PlanErrors;
import atlas.domain.nutrition.PlanId;
import atlas.domain.nutrition.enums.PlanStatus;
import atlas.domain.nutrition.events.PlanArchivedEvent;
import atlas.domain.nutrition.events.PlanDefinedEvent;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.nutrition.vos.Weight;
import atlas.support.builders.UnitOfWorkStub;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PlanCommandHandlersTest {

    private static final Instant NOW = Instant.parse("2026-08-22T07:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final PlanId ID = PlanId.of(7);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final NutritionUnitOfWork unitOfWork = mock(NutritionUnitOfWork.class);
    private final PlanRepository plans = mock(PlanRepository.class);

    private final DefinePlanCommandHandler define = new DefinePlanCommandHandler(unitOfWork, CLOCK);
    private final AdjustPlanCommandHandler adjust = new AdjustPlanCommandHandler(unitOfWork, CLOCK);
    private final ArchivePlanCommandHandler archive = new ArchivePlanCommandHandler(unitOfWork, CLOCK);

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.with(unitOfWork, plans);
        when(plans.nextId()).thenReturn(ID);
        when(plans.findActive()).thenReturn(Optional.empty());
    }

    @Test
    void shouldPersistThePlanAndReturnItWithItsDerivedFields() {
        var result = define.handle(aPlanOf("84.0", "78.0", 150, 200, 60));

        assertThat(result.isSuccess()).isTrue();

        var plan = result.value();
        assertThat(plan.id()).isEqualTo("N00000007");
        assertThat(plan.startWeight()).isEqualByComparingTo("84.0");
        assertThat(plan.targetWeight()).isEqualByComparingTo("78.0");
        assertThat(plan.goal()).isEqualTo("LOSE");
        assertThat(plan.goalLabel()).isEqualTo("Perder peso");
        assertThat(plan.dailyMacros().calories()).isEqualTo(1_940);
        assertThat(plan.status()).isEqualTo("ACTIVE");
        assertThat(plan.startedOn()).isEqualTo(TODAY);

        verify(plans).create(any(Plan.class));
    }

    @Test
    void shouldRegisterTheDefinedEvent() {
        define.handle(aPlanOf("84.0", "78.0", 150, 200, 60));

        var saved = ArgumentCaptor.forClass(Plan.class);
        verify(plans).create(saved.capture());

        assertThat(saved.getValue().pendingEvents()).containsExactly(new PlanDefinedEvent(ID, NOW));
    }

    @Test
    void shouldStartThePlanTodayWhenNoDateIsGiven() {
        var command = new DefinePlanCommand(
            new BigDecimal("84.0"), new BigDecimal("78.0"), 150, 200, 60, null);

        assertThat(define.handle(command).value().startedOn()).isEqualTo(TODAY);
    }

    @Test
    void shouldArchiveThePreviousPlanInTheSameUnitOfWork() {
        var previous = activePlan();
        when(plans.findActive()).thenReturn(Optional.of(previous));

        var result = define.handle(aPlanOf("84.0", "78.0", 150, 200, 60));

        assertThat(result.isSuccess()).isTrue();
        assertThat(previous.status()).isEqualTo(PlanStatus.ARCHIVED);
        assertThat(previous.pendingEvents()).contains(new PlanArchivedEvent(PlanId.of(1), NOW));

        verify(plans).update(previous);
        verify(plans).create(any(Plan.class));
    }

    @Test
    void shouldFailWhenTheStartingWeightIsNotBelievable() {
        var result = define.handle(aPlanOf("4.0", "78.0", 150, 200, 60));

        assertThat(result.error()).isEqualTo(NutritionErrors.WEIGHT_OUT_OF_RANGE);
        verify(plans, never()).create(any());
    }

    @Test
    void shouldFailWhenTheTargetWeightIsNotBelievable() {
        var result = define.handle(aPlanOf("84.0", "900.0", 150, 200, 60));

        assertThat(result.error()).isEqualTo(NutritionErrors.WEIGHT_OUT_OF_RANGE);
        verify(plans, never()).create(any());
    }

    @Test
    void shouldFailWhenAMacroIsNegative() {
        var result = define.handle(aPlanOf("84.0", "78.0", 150, -1, 60));

        assertThat(result.error()).isEqualTo(NutritionErrors.MACROS_MUST_NOT_BE_NEGATIVE);
        verify(plans, never()).create(any());
    }

    @Test
    void shouldFailWhenTheQuotaIsEmpty() {
        var result = define.handle(aPlanOf("84.0", "78.0", 0, 0, 0));

        assertThat(result.error()).isEqualTo(PlanErrors.MACROS_REQUIRED);
        verify(plans, never()).create(any());
    }

    @Test
    void shouldFailWhenThePlanStartsInTheFuture() {
        var command = new DefinePlanCommand(
            new BigDecimal("84.0"), new BigDecimal("78.0"), 150, 200, 60, TODAY.plusDays(1));

        assertThat(define.handle(command).error()).isEqualTo(PlanErrors.CANNOT_START_IN_THE_FUTURE);
        verify(plans, never()).create(any());
    }

    @Test
    void shouldAdjustTheActivePlan() {
        var plan = activePlan();
        when(plans.findActive()).thenReturn(Optional.of(plan));

        var result = adjust.handle(new AdjustPlanCommand(new BigDecimal("76.0"), 160, 180, 55));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().targetWeight()).isEqualByComparingTo("76.0");
        assertThat(result.value().dailyMacros().protein()).isEqualTo(160);

        verify(plans).update(plan);
    }

    @Test
    void shouldFailToAdjustWhenThereIsNoActivePlan() {
        var result = adjust.handle(new AdjustPlanCommand(new BigDecimal("76.0"), 160, 180, 55));

        assertThat(result.error()).isEqualTo(PlanErrors.NONE_ACTIVE);
        verify(plans, never()).update(any());
    }

    @Test
    void shouldFailToAdjustWithAnEmptyQuota() {
        when(plans.findActive()).thenReturn(Optional.of(activePlan()));

        var result = adjust.handle(new AdjustPlanCommand(new BigDecimal("76.0"), 0, 0, 0));

        assertThat(result.error()).isEqualTo(PlanErrors.MACROS_REQUIRED);
        verify(plans, never()).update(any());
    }

    @Test
    void shouldArchiveTheActivePlan() {
        var plan = activePlan();
        when(plans.findActive()).thenReturn(Optional.of(plan));

        var result = archive.handle(new ArchivePlanCommand());

        assertThat(result.isSuccess()).isTrue();
        assertThat(plan.status()).isEqualTo(PlanStatus.ARCHIVED);

        verify(plans).update(plan);
    }

    @Test
    void shouldFailToArchiveWhenThereIsNoActivePlan() {
        var result = archive.handle(new ArchivePlanCommand());

        assertThat(result.error()).isEqualTo(PlanErrors.NONE_ACTIVE);
        verify(plans, never()).update(any());
    }

    private static Plan activePlan() {
        return Plan.rehydrate(
            PlanId.of(1),
            new Weight(84_000),
            new Weight(78_000),
            new Macros(150, 200, 60),
            PlanStatus.ACTIVE,
            TODAY.minusDays(30),
            NOW);
    }

    private static DefinePlanCommand aPlanOf(
        String startWeight, String targetWeight, int protein, int carbs, int fat) {

        return new DefinePlanCommand(
            new BigDecimal(startWeight), new BigDecimal(targetWeight), protein, carbs, fat, TODAY);
    }
}
