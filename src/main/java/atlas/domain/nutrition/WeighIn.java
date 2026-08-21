package atlas.domain.nutrition;

import atlas.domain.nutrition.events.WeighInCorrectedEvent;
import atlas.domain.nutrition.events.WeighInDeletedEvent;
import atlas.domain.nutrition.events.WeighInRecordedEvent;
import atlas.domain.nutrition.vos.Weight;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.time.LocalDate;

public final class WeighIn extends AggregateRoot<WeighInId> {

    private final LocalDate measuredOn;
    private final Instant recordedAt;

    private Weight weight;

    private WeighIn(WeighInId id, Weight weight, LocalDate measuredOn, Instant recordedAt) {
        super(ObjectGuard.notNull(id, "id"));
        this.weight = ObjectGuard.notNull(weight, "weight");
        this.measuredOn = ObjectGuard.notNull(measuredOn, "measuredOn");
        this.recordedAt = ObjectGuard.notNull(recordedAt, "recordedAt");
    }

    public static Result<WeighIn> record(
        WeighInId id, Weight weight, LocalDate measuredOn, LocalDate today, Instant now) {

        if (measuredOn.isAfter(today)) {
            return Result.failure(WeighInErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
        }

        var weighIn = new WeighIn(id, weight, measuredOn, now);
        weighIn.registerEvent(new WeighInRecordedEvent(id, now));

        return Result.success(weighIn);
    }

    public static WeighIn rehydrate(
        WeighInId id, Weight weight, LocalDate measuredOn, Instant recordedAt) {

        return new WeighIn(id, weight, measuredOn, recordedAt);
    }

    public Result<Void> correct(Weight newWeight, Instant now) {
        this.weight = ObjectGuard.notNull(newWeight, "newWeight");
        registerEvent(new WeighInCorrectedEvent(id(), now));

        return Result.success();
    }

    public Result<Void> delete(Instant now) {
        registerEvent(new WeighInDeletedEvent(id(), now));

        return Result.success();
    }

    public Weight weight() {
        return weight;
    }

    public LocalDate measuredOn() {
        return measuredOn;
    }

    public Instant recordedAt() {
        return recordedAt;
    }
}
