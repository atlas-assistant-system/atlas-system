package atlas.domain.economy;

import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import atlas.domain.economy.events.MovementCorrectedEvent;
import atlas.domain.economy.events.MovementDeletedEvent;
import atlas.domain.economy.events.MovementRecategorizedEvent;
import atlas.domain.economy.events.MovementRecordedEvent;
import atlas.domain.economy.vos.Money;
import atlas.domain.economy.vos.MovementNote;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public final class Movement extends AggregateRoot<MovementId> {

    private final MovementKind kind;
    private final Instant recordedAt;

    private Money amount;
    private Category category;
    private Optional<MovementNote> note;
    private LocalDate occurredOn;

    private Movement(
        MovementId id,
        MovementKind kind,
        Money amount,
        Category category,
        Optional<MovementNote> note,
        LocalDate occurredOn,
        Instant recordedAt) {
        super(ObjectGuard.notNull(id, "id"));
        this.kind = ObjectGuard.notNull(kind, "kind");
        this.amount = ObjectGuard.notNull(amount, "amount");
        this.category = ObjectGuard.notNull(category, "category");
        this.note = ObjectGuard.notNull(note, "note");
        this.occurredOn = ObjectGuard.notNull(occurredOn, "occurredOn");
        this.recordedAt = ObjectGuard.notNull(recordedAt, "recordedAt");
    }

    public static Result<Movement> record(
        MovementId id,
        MovementKind kind,
        Money amount,
        Category category,
        Optional<MovementNote> note,
        LocalDate occurredOn,
        LocalDate today,
        Instant now) {

        if (!category.matches(kind)) {
            return Result.failure(MovementErrors.CATEGORY_DOES_NOT_MATCH_KIND);
        }

        if (occurredOn.isAfter(today)) {
            return Result.failure(MovementErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
        }

        var movement = new Movement(id, kind, amount, category, note, occurredOn, now);
        movement.registerEvent(new MovementRecordedEvent(id, now));

        return Result.success(movement);
    }

    public static Movement rehydrate(
        MovementId id,
        MovementKind kind,
        Money amount,
        Category category,
        Optional<MovementNote> note,
        LocalDate occurredOn,
        Instant recordedAt) {

        return new Movement(id, kind, amount, category, note, occurredOn, recordedAt);
    }

    public Result<Void> correct(
        Money newAmount, Optional<MovementNote> newNote, LocalDate newDate, LocalDate today, Instant now) {

        if (newDate.isAfter(today)) {
            return Result.failure(MovementErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
        }

        this.amount = ObjectGuard.notNull(newAmount, "newAmount");
        this.note = ObjectGuard.notNull(newNote, "newNote");
        this.occurredOn = newDate;
        registerEvent(new MovementCorrectedEvent(id(), now));

        return Result.success();
    }

    public Result<Void> recategorize(Category newCategory, Instant now) {
        if (!newCategory.matches(kind)) {
            return Result.failure(MovementErrors.CATEGORY_DOES_NOT_MATCH_KIND);
        }

        this.category = newCategory;
        registerEvent(new MovementRecategorizedEvent(id(), now));

        return Result.success();
    }

    public Result<Void> delete(Instant now) {
        registerEvent(new MovementDeletedEvent(id(), now));

        return Result.success();
    }

    public MovementKind kind() {
        return kind;
    }

    public Money amount() {
        return amount;
    }

    public Category category() {
        return category;
    }

    public Optional<MovementNote> note() {
        return note;
    }

    public LocalDate occurredOn() {
        return occurredOn;
    }

    public Instant recordedAt() {
        return recordedAt;
    }
}
