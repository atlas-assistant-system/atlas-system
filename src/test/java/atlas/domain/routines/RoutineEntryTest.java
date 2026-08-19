package atlas.domain.routines;

import static atlas.support.builders.RoutineFixtures.NOW;
import static atlas.support.builders.RoutineFixtures.ROUTINE_ID;
import static atlas.support.builders.RoutineFixtures.entry;
import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.routines.events.DayClearedEvent;
import atlas.domain.routines.events.ProgressLoggedEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class RoutineEntryTest {

    private static final LocalDate DAY = LocalDate.of(2026, 2, 14);

    @Test
    void shouldRaiseProgressLoggedEventWhenLogged() {
        var entry = RoutineEntry
            .log(RoutineEntryId.of(UUID.randomUUID()), ROUTINE_ID, DAY, new BigDecimal("1.5"), NOW)
            .value();

        assertThat(entry.amount()).isEqualByComparingTo("1.5");
        assertThat(entry.pendingEvents())
            .singleElement()
            .isEqualTo(new ProgressLoggedEvent(ROUTINE_ID, DAY, entry.amount(), NOW));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-1"})
    void shouldFailWhenLoggedAmountIsNotPositive(String amount) {
        var result = RoutineEntry.log(
            RoutineEntryId.of(UUID.randomUUID()),
            ROUTINE_ID,
            DAY,
            amount == null ? null : new BigDecimal(amount),
            NOW);

        assertThat(result.error()).isEqualTo(RoutineErrors.AMOUNT_MUST_BE_POSITIVE);
    }

    @Test
    void shouldAddOnTopOfWhatWasAlreadyLogged() {
        var entry = entry(DAY, 1);
        entry.clearEvents();

        var result = entry.add(new BigDecimal("0.5"), NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(entry.amount()).isEqualByComparingTo("1.5");
    }

    @Test
    void shouldRaiseEventWithTheAddedAmountOnly() {
        var entry = entry(DAY, 2);
        entry.clearEvents();

        entry.add(BigDecimal.ONE, NOW);

        assertThat(entry.pendingEvents())
            .containsExactly(new ProgressLoggedEvent(ROUTINE_ID, DAY, BigDecimal.ONE, NOW));
        assertThat(entry.amount()).isEqualByComparingTo("3");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-2"})
    void shouldFailAndKeepTheAmountWhenAddingSomethingNotPositive(String extra) {
        var entry = entry(DAY, 2);

        var result = entry.add(extra == null ? null : new BigDecimal(extra), NOW);

        assertThat(result.error()).isEqualTo(RoutineErrors.AMOUNT_MUST_BE_POSITIVE);
        assertThat(entry.amount()).isEqualByComparingTo("2");
    }

    @Test
    void shouldRaiseDayClearedEventWhenCleared() {
        var entry = entry(DAY, 1);
        entry.clearEvents();

        entry.clear(NOW);

        assertThat(entry.pendingEvents()).containsExactly(new DayClearedEvent(ROUTINE_ID, DAY, NOW));
    }

    @Test
    void shouldNormalizeScaleSoThatEqualAmountsCompareEqual() {
        var entry = entry(DAY, 2);

        entry.add(new BigDecimal("1.000"), NOW);

        assertThat(entry.amount()).isEqualTo(new BigDecimal("3"));
    }
}
