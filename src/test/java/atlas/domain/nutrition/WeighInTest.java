package atlas.domain.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.nutrition.events.WeighInCorrectedEvent;
import atlas.domain.nutrition.events.WeighInDeletedEvent;
import atlas.domain.nutrition.events.WeighInRecordedEvent;
import atlas.domain.nutrition.vos.Weight;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class WeighInTest {

    private static final WeighInId ID = WeighInId.of(1);
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final Instant NOW = Instant.parse("2026-08-22T07:00:00Z");
    private static final Weight WEIGHT = new Weight(82_400);

    @Test
    void shouldRecordTheReadingAndRaiseItsEvent() {
        var result = record(WEIGHT, TODAY);

        assertThat(result.isSuccess()).isTrue();

        var weighIn = result.value();
        assertThat(weighIn.weight()).isEqualTo(WEIGHT);
        assertThat(weighIn.measuredOn()).isEqualTo(TODAY);
        assertThat(weighIn.recordedAt()).isEqualTo(NOW);
        assertThat(weighIn.pendingEvents()).containsExactly(new WeighInRecordedEvent(ID, NOW));
    }

    @Test
    void shouldFailWhenItIsDatedInTheFuture() {
        var result = record(WEIGHT, TODAY.plusDays(1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(WeighInErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
    }

    @Test
    void shouldAcceptAReadingFromAnEarlierDay() {
        assertThat(record(WEIGHT, TODAY.minusDays(10)).isSuccess()).isTrue();
    }

    @Test
    void shouldCorrectTheReadingAndRaiseItsEvent() {
        var weighIn = record(WEIGHT, TODAY).value();
        var corrected = new Weight(82_100);

        var result = weighIn.correct(corrected, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(weighIn.weight()).isEqualTo(corrected);
        assertThat(weighIn.pendingEvents()).contains(new WeighInCorrectedEvent(ID, NOW));
    }

    @Test
    void shouldKeepTheDayWhenTheReadingIsCorrected() {
        var weighIn = record(WEIGHT, TODAY.minusDays(1)).value();

        weighIn.correct(new Weight(82_100), NOW);

        assertThat(weighIn.measuredOn()).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    void shouldRaiseItsEventWhenDeleted() {
        var weighIn = record(WEIGHT, TODAY).value();

        var result = weighIn.delete(NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(weighIn.pendingEvents()).contains(new WeighInDeletedEvent(ID, NOW));
    }

    @Test
    void shouldRehydrateWithoutRaisingEvents() {
        var weighIn = WeighIn.rehydrate(ID, WEIGHT, TODAY, NOW);

        assertThat(weighIn.weight()).isEqualTo(WEIGHT);
        assertThat(weighIn.pendingEvents()).isEmpty();
    }

    private static Result<WeighIn> record(Weight weight, LocalDate measuredOn) {
        return WeighIn.record(ID, weight, measuredOn, TODAY, NOW);
    }
}
