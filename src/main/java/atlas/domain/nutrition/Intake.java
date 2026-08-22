package atlas.domain.nutrition;

import atlas.domain.nutrition.events.IntakeCorrectedEvent;
import atlas.domain.nutrition.events.IntakeDeletedEvent;
import atlas.domain.nutrition.events.IntakeRecordedEvent;
import atlas.domain.nutrition.vos.Calories;
import atlas.domain.nutrition.vos.IntakeNote;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public final class Intake extends AggregateRoot<IntakeId> {

    private final LocalDate consumedOn;
    private final Instant recordedAt;

    private Calories calories;
    private Macros macros;
    private Optional<IntakeNote> note;

    private Intake(
        IntakeId id,
        Calories calories,
        Macros macros,
        Optional<IntakeNote> note,
        LocalDate consumedOn,
        Instant recordedAt) {
        super(ObjectGuard.notNull(id, "id"));
        this.calories = ObjectGuard.notNull(calories, "calories");
        this.macros = ObjectGuard.notNull(macros, "macros");
        this.note = ObjectGuard.notNull(note, "note");
        this.consumedOn = ObjectGuard.notNull(consumedOn, "consumedOn");
        this.recordedAt = ObjectGuard.notNull(recordedAt, "recordedAt");
    }

    public static Result<Intake> record(
        IntakeId id,
        Calories calories,
        Macros macros,
        Optional<IntakeNote> note,
        LocalDate consumedOn,
        LocalDate today,
        Instant now) {

        if (calories.isZero()) {
            return Result.failure(IntakeErrors.CALORIES_REQUIRED);
        }

        if (consumedOn.isAfter(today)) {
            return Result.failure(IntakeErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
        }

        var intake = new Intake(id, calories, macros, note, consumedOn, now);
        intake.registerEvent(new IntakeRecordedEvent(id, now));

        return Result.success(intake);
    }

    public static Intake rehydrate(
        IntakeId id,
        Calories calories,
        Macros macros,
        Optional<IntakeNote> note,
        LocalDate consumedOn,
        Instant recordedAt) {

        return new Intake(id, calories, macros, note, consumedOn, recordedAt);
    }

    public Result<Void> correct(
        Calories newCalories, Macros newMacros, Optional<IntakeNote> newNote, Instant now) {

        if (newCalories.isZero()) {
            return Result.failure(IntakeErrors.CALORIES_REQUIRED);
        }

        this.calories = newCalories;
        this.macros = ObjectGuard.notNull(newMacros, "newMacros");
        this.note = ObjectGuard.notNull(newNote, "newNote");
        registerEvent(new IntakeCorrectedEvent(id(), now));

        return Result.success();
    }

    public Result<Void> delete(Instant now) {
        registerEvent(new IntakeDeletedEvent(id(), now));

        return Result.success();
    }

    public Calories calories() {
        return calories;
    }

    public Macros macros() {
        return macros;
    }

    public Optional<IntakeNote> note() {
        return note;
    }

    public LocalDate consumedOn() {
        return consumedOn;
    }

    public Instant recordedAt() {
        return recordedAt;
    }
}
