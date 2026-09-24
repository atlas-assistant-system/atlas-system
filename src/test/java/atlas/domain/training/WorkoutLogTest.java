package atlas.domain.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.training.entities.SetLog;
import atlas.domain.training.entities.SetLogId;
import atlas.domain.training.events.SetRecordedEvent;
import atlas.domain.training.events.SetRemovedEvent;
import atlas.domain.training.events.WorkoutLogDiscardedEvent;
import atlas.domain.training.events.WorkoutStartedEvent;
import atlas.domain.training.vos.Effort;
import atlas.domain.training.vos.PlannedSet;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class WorkoutLogTest {

    private static final WorkoutLogId ID = WorkoutLogId.of(1);
    private static final WorkoutId WORKOUT = WorkoutId.of(5);
    private static final ExerciseId PRESS = ExerciseId.of(10);
    private static final ExerciseId DIPS = ExerciseId.of(11);
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final Instant NOW = Instant.parse("2026-08-22T18:00:00Z");
    private static final Effort EIGHT_AT_SEVENTY = new Effort(70_000, 8, 0, 0);
    private static final Effort SIX_AT_SEVENTY = new Effort(70_000, 6, 0, 0);

    @Test
    void shouldExpandThePlanIntoPendingSets() {
        var log = startFrom(List.of(
            new PlannedSet(PRESS, EIGHT_AT_SEVENTY), new PlannedSet(DIPS, EIGHT_AT_SEVENTY)));

        assertThat(log.sets()).hasSize(2);
        assertThat(log.sets().getFirst().planned()).contains(EIGHT_AT_SEVENTY);
        assertThat(log.sets().getFirst().actual()).isEmpty();
        assertThat(log.sets()).extracting(set -> set.position()).containsExactly(0, 1);
    }

    @Test
    void shouldRaiseStartedEventAndRememberWhichWorkoutItCameFrom() {
        var log = startFrom(List.of(new PlannedSet(PRESS, EIGHT_AT_SEVENTY)));

        assertThat(log.workoutId()).contains(WORKOUT);
        assertThat(log.performedOn()).isEqualTo(TODAY);
        assertThat(log.startedAt()).isEqualTo(NOW);
        assertThat(log.pendingEvents()).containsExactly(new WorkoutStartedEvent(ID, NOW));
    }

    @Test
    void shouldStartAFreeWorkoutWithNoPlanAtAll() {
        var result = WorkoutLog.start(ID, Optional.empty(), List.of(), ids(), TODAY, TODAY, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().workoutId()).isEmpty();
        assertThat(result.value().sets()).isEmpty();
    }

    @Test
    void shouldNotBeDatedInTheFuture() {
        var result = WorkoutLog.start(
            ID, Optional.empty(), List.of(), ids(), TODAY.plusDays(1), TODAY, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(WorkoutLogErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
    }

    @Test
    void shouldAcceptAWorkoutFromYesterdayBecauseYouApuntasDespues() {
        var result = WorkoutLog.start(
            ID, Optional.empty(), List.of(), ids(), TODAY.minusDays(1), TODAY, NOW);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldFillInWhatWasActuallyLiftedWithoutTouchingThePlan() {
        var log = startFrom(List.of(new PlannedSet(PRESS, EIGHT_AT_SEVENTY)));
        var setId = log.sets().getFirst().id();
        log.clearEvents();

        var result = log.recordSet(setId, SIX_AT_SEVENTY, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(log.sets().getFirst().actual()).contains(SIX_AT_SEVENTY);
        assertThat(log.sets().getFirst().planned()).contains(EIGHT_AT_SEVENTY);
        assertThat(log.pendingEvents()).containsExactly(new SetRecordedEvent(ID, NOW));
    }

    @Test
    void shouldCorrectASetThatWasAlreadyRecorded() {
        var log = startFrom(List.of(new PlannedSet(PRESS, EIGHT_AT_SEVENTY)));
        var setId = log.sets().getFirst().id();
        log.recordSet(setId, SIX_AT_SEVENTY, NOW);

        log.recordSet(setId, EIGHT_AT_SEVENTY, NOW);

        assertThat(log.sets().getFirst().actual()).contains(EIGHT_AT_SEVENTY);
    }

    @Test
    void shouldRefuseASetThatMeasuresNothingAtAll() {
        var log = startFrom(List.of(new PlannedSet(PRESS, EIGHT_AT_SEVENTY)));

        var result = log.recordSet(log.sets().getFirst().id(), Effort.NONE, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(WorkoutLogErrors.SET_MEASURES_NOTHING);
    }

    @Test
    void shouldReportAnUnknownSetAsNotFound() {
        var log = startFrom(List.of(new PlannedSet(PRESS, EIGHT_AT_SEVENTY)));
        var stranger = SetLogId.of(UUID.randomUUID());

        assertThat(log.recordSet(stranger, SIX_AT_SEVENTY, NOW).error())
            .isEqualTo(WorkoutLogErrors.SET_NOT_FOUND);
        assertThat(log.removeSet(stranger, NOW).error())
            .isEqualTo(WorkoutLogErrors.SET_NOT_FOUND);
    }

    @Test
    void shouldAddASetOutsideThePlanWithNoPlannedEffort() {
        var log = startFrom(List.of(new PlannedSet(PRESS, EIGHT_AT_SEVENTY)));
        log.clearEvents();

        var result = log.addSet(SetLogId.of(UUID.randomUUID()), DIPS, SIX_AT_SEVENTY, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(log.sets()).hasSize(2);
        assertThat(log.sets().getLast().planned()).isEmpty();
        assertThat(log.sets().getLast().actual()).contains(SIX_AT_SEVENTY);
        assertThat(log.sets().getLast().position()).isEqualTo(1);
        assertThat(log.pendingEvents()).containsExactly(new SetRecordedEvent(ID, NOW));
    }

    @Test
    void shouldRefuseAnExtraSetThatMeasuresNothing() {
        var log = startFrom(List.of());

        var result = log.addSet(SetLogId.of(UUID.randomUUID()), DIPS, Effort.NONE, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(WorkoutLogErrors.SET_MEASURES_NOTHING);
    }

    @Test
    void shouldRemoveASetAndSaySo() {
        var log = startFrom(List.of(
            new PlannedSet(PRESS, EIGHT_AT_SEVENTY), new PlannedSet(DIPS, EIGHT_AT_SEVENTY)));
        var setId = log.sets().getFirst().id();
        log.clearEvents();

        var result = log.removeSet(setId, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(log.sets()).hasSize(1);
        assertThat(log.sets().getFirst().exerciseId()).isEqualTo(DIPS);
        assertThat(log.pendingEvents()).containsExactly(new SetRemovedEvent(ID, NOW));
    }

    @Test
    void shouldCloseTheGapInPositionsAfterRemovingASet() {
        var log = startFrom(List.of(
            new PlannedSet(PRESS, EIGHT_AT_SEVENTY),
            new PlannedSet(DIPS, EIGHT_AT_SEVENTY),
            new PlannedSet(PRESS, EIGHT_AT_SEVENTY)));

        log.removeSet(log.sets().getFirst().id(), NOW);

        assertThat(log.sets()).extracting(set -> set.position()).containsExactly(0, 1);
    }

    @Test
    void shouldSaySoWhenTheWholeWorkoutIsDiscarded() {
        var log = startFrom(List.of(new PlannedSet(PRESS, EIGHT_AT_SEVENTY)));
        log.clearEvents();

        var result = log.discard(NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(log.pendingEvents()).containsExactly(new WorkoutLogDiscardedEvent(ID, NOW));
    }

    @Test
    void shouldNotLetTheOutsideChangeTheSetsThroughTheListItGaveBack() {
        var log = startFrom(List.of(new PlannedSet(PRESS, EIGHT_AT_SEVENTY)));
        var handedOver = log.sets();

        assertThatThrownBy(handedOver::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThat(log.sets()).hasSize(1);
    }

    @Test
    void shouldRehydrateItsSetsWithoutRaisingAnyEvent() {
        var set = new SetLog(
            SetLogId.of(UUID.randomUUID()), PRESS, 0,
            Optional.of(EIGHT_AT_SEVENTY), Optional.of(SIX_AT_SEVENTY));

        var log = WorkoutLog.rehydrate(ID, Optional.of(WORKOUT), TODAY, NOW, List.of(set));

        assertThat(log.sets()).containsExactly(set);
        assertThat(log.startedAt()).isEqualTo(NOW);
        assertThat(log.pendingEvents()).isEmpty();
    }

    private static WorkoutLog startFrom(List<PlannedSet> plan) {
        return WorkoutLog.start(ID, Optional.of(WORKOUT), plan, ids(), TODAY, TODAY, NOW).value();
    }

    private static Supplier<SetLogId> ids() {
        var counter = new AtomicLong();

        return () -> SetLogId.of(new UUID(0, counter.incrementAndGet()));
    }
}
