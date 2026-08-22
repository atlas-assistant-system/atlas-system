package atlas.domain.training;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.training.entities.PlannedExercise;
import atlas.domain.training.entities.PlannedExerciseId;
import atlas.domain.training.events.WorkoutArchivedEvent;
import atlas.domain.training.events.WorkoutDefinedEvent;
import atlas.domain.training.events.WorkoutPlanChangedEvent;
import atlas.domain.training.events.WorkoutRenamedEvent;
import atlas.domain.training.vos.Effort;
import atlas.domain.training.vos.PlannedLine;
import atlas.domain.training.vos.PlannedSet;
import atlas.domain.training.vos.SetCount;
import atlas.domain.training.vos.WorkoutName;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkoutTest {

    private static final WorkoutId ID = WorkoutId.of(1);
    private static final ExerciseId PRESS = ExerciseId.of(10);
    private static final ExerciseId DIPS = ExerciseId.of(11);
    private static final Instant NOW = Instant.parse("2026-08-22T18:00:00Z");
    private static final Effort EIGHT_AT_SEVENTY = new Effort(70_000, 8, 0, 0);
    private static final Effort TEN_BODYWEIGHT = new Effort(0, 10, 0, 0);

    @Test
    void shouldDefineAnEmptyWorkout() {
        var workout = define();

        assertThat(workout.name().value()).isEqualTo("Dia de empuje");
        assertThat(workout.plan()).isEmpty();
        assertThat(workout.isArchived()).isFalse();
        assertThat(workout.pendingEvents()).containsExactly(new WorkoutDefinedEvent(ID, NOW));
    }

    @Test
    void shouldRenameAndSaySo() {
        var workout = define();
        workout.clearEvents();

        var result = workout.rename(new WorkoutName("Empuje A"), NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(workout.name().value()).isEqualTo("Empuje A");
        assertThat(workout.pendingEvents()).containsExactly(new WorkoutRenamedEvent(ID, NOW));
    }

    @Test
    void shouldKeepTheLinesInTheOrderTheyWereGiven() {
        var workout = define();

        workout.setPlan(List.of(line(PRESS, 4, EIGHT_AT_SEVENTY), line(DIPS, 3, TEN_BODYWEIGHT)), NOW);

        assertThat(workout.plan()).extracting(planned -> planned.exerciseId()).containsExactly(PRESS, DIPS);
        assertThat(workout.plan()).extracting(planned -> planned.position()).containsExactly(0, 1);
    }

    @Test
    void shouldReplaceTheWholePlanInsteadOfMergingIt() {
        var workout = define();
        workout.setPlan(List.of(line(PRESS, 4, EIGHT_AT_SEVENTY), line(DIPS, 3, TEN_BODYWEIGHT)), NOW);

        workout.setPlan(List.of(line(DIPS, 5, TEN_BODYWEIGHT)), NOW);

        assertThat(workout.plan()).hasSize(1);
        assertThat(workout.plan().getFirst().exerciseId()).isEqualTo(DIPS);
        assertThat(workout.plan().getFirst().sets().value()).isEqualTo(5);
    }

    @Test
    void shouldReorderByGivingTheLinesInAnotherOrder() {
        var workout = define();
        workout.setPlan(List.of(line(PRESS, 4, EIGHT_AT_SEVENTY), line(DIPS, 3, TEN_BODYWEIGHT)), NOW);

        workout.setPlan(List.of(line(DIPS, 3, TEN_BODYWEIGHT), line(PRESS, 4, EIGHT_AT_SEVENTY)), NOW);

        assertThat(workout.plan()).extracting(planned -> planned.exerciseId()).containsExactly(DIPS, PRESS);
    }

    @Test
    void shouldAllowTheSameExerciseTwiceBecausePressingAtBothEndsIsReal() {
        var workout = define();

        var result = workout.setPlan(
            List.of(line(PRESS, 4, EIGHT_AT_SEVENTY), line(PRESS, 1, EIGHT_AT_SEVENTY)), NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(workout.plan()).hasSize(2);
    }

    @Test
    void shouldRaisePlanChangedEventWhenThePlanIsSet() {
        var workout = define();
        workout.clearEvents();

        workout.setPlan(List.of(line(PRESS, 4, EIGHT_AT_SEVENTY)), NOW);

        assertThat(workout.pendingEvents()).containsExactly(new WorkoutPlanChangedEvent(ID, NOW));
    }

    @Test
    void shouldAcceptAnEmptyPlanBecauseEmptyingItIsHowYouStartOver() {
        var workout = define();
        workout.setPlan(List.of(line(PRESS, 4, EIGHT_AT_SEVENTY)), NOW);

        var result = workout.setPlan(List.of(), NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(workout.plan()).isEmpty();
    }

    @Test
    void shouldExpandEachLineIntoOneSetPerRepetitionOfIt() {
        var workout = define();
        workout.setPlan(List.of(line(PRESS, 4, EIGHT_AT_SEVENTY), line(DIPS, 2, TEN_BODYWEIGHT)), NOW);

        assertThat(workout.expand()).containsExactly(
            new PlannedSet(PRESS, EIGHT_AT_SEVENTY),
            new PlannedSet(PRESS, EIGHT_AT_SEVENTY),
            new PlannedSet(PRESS, EIGHT_AT_SEVENTY),
            new PlannedSet(PRESS, EIGHT_AT_SEVENTY),
            new PlannedSet(DIPS, TEN_BODYWEIGHT),
            new PlannedSet(DIPS, TEN_BODYWEIGHT));
    }

    @Test
    void shouldExpandAnEmptyPlanIntoNothing() {
        assertThat(define().expand()).isEmpty();
    }

    @Test
    void shouldArchiveAndSaySo() {
        var workout = define();
        workout.clearEvents();

        var result = workout.archive(NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(workout.isArchived()).isTrue();
        assertThat(workout.pendingEvents()).containsExactly(new WorkoutArchivedEvent(ID, NOW));
    }

    @Test
    void shouldNotArchiveTwice() {
        var workout = define();
        workout.archive(NOW);

        assertThat(workout.archive(NOW).error()).isEqualTo(WorkoutErrors.ALREADY_ARCHIVED);
    }

    @Test
    void shouldRefuseEveryChangeOnceArchived() {
        var workout = define();
        workout.archive(NOW);

        assertThat(workout.rename(new WorkoutName("Otro"), NOW).error())
            .isEqualTo(WorkoutErrors.ALREADY_ARCHIVED);
        assertThat(workout.setPlan(List.of(line(PRESS, 4, EIGHT_AT_SEVENTY)), NOW).error())
            .isEqualTo(WorkoutErrors.ALREADY_ARCHIVED);
    }

    @Test
    void shouldNotLetTheOutsideChangeThePlanThroughTheListItHandedOver() {
        var workout = define();
        var lines = new ArrayList<>(List.of(line(PRESS, 4, EIGHT_AT_SEVENTY)));
        workout.setPlan(lines, NOW);

        lines.clear();

        assertThat(workout.plan()).hasSize(1);
    }

    @Test
    void shouldRehydrateItsPlanWithoutRaisingAnyEvent() {
        var line = new PlannedExercise(
            PlannedExerciseId.of(UUID.randomUUID()), PRESS, 0, new SetCount(4), EIGHT_AT_SEVENTY);

        var workout = Workout.rehydrate(ID, new WorkoutName("Empuje"), List.of(line), true);

        assertThat(workout.plan()).containsExactly(line);
        assertThat(workout.isArchived()).isTrue();
        assertThat(workout.pendingEvents()).isEmpty();
    }

    private static Workout define() {
        return Workout.define(ID, new WorkoutName("Dia de empuje"), NOW).value();
    }

    private static PlannedLine line(ExerciseId exerciseId, int sets, Effort target) {
        return new PlannedLine(
            PlannedExerciseId.of(UUID.randomUUID()), exerciseId, new SetCount(sets), target);
    }
}
