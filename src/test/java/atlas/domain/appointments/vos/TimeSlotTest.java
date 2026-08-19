package atlas.domain.appointments.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.sharedkernel.exceptions.GuardException;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TimeSlotTest {

    private static final LocalDateTime TEN = LocalDateTime.of(2026, 8, 17, 10, 0);
    private static final LocalDateTime HALF_PAST_TEN = LocalDateTime.of(2026, 8, 17, 10, 30);
    private static final LocalDateTime ELEVEN = LocalDateTime.of(2026, 8, 17, 11, 0);
    private static final LocalDateTime TWELVE = LocalDateTime.of(2026, 8, 17, 12, 0);
    private static final LocalDateTime ONE = LocalDateTime.of(2026, 8, 17, 13, 0);
    private static final LocalDateTime TWO = LocalDateTime.of(2026, 8, 17, 14, 0);

    @Test
    void shouldCreateSlotWhenEndIsAfterStart() {
        var result = TimeSlot.create(TEN, ELEVEN);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().start()).isEqualTo(TEN);
        assertThat(result.value().end()).isEqualTo(ELEVEN);
    }

    @Test
    void shouldFailWhenEndIsBeforeStart() {
        var result = TimeSlot.create(ELEVEN, TEN);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.INVALID_TIME_SLOT);
    }

    @Test
    void shouldFailWhenEndEqualsStart() {
        var result = TimeSlot.create(TEN, TEN);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.INVALID_TIME_SLOT);
    }

    @Test
    void shouldFailWhenStartIsMissing() {
        assertThat(TimeSlot.create(null, ELEVEN).error()).isEqualTo(AppointmentErrors.TIME_SLOT_REQUIRED);
    }

    @Test
    void shouldFailWhenEndIsMissing() {
        assertThat(TimeSlot.create(TEN, null).error()).isEqualTo(AppointmentErrors.TIME_SLOT_REQUIRED);
    }

    @Test
    void shouldThrowWhenInternallyBuiltSlotIsInvalid() {
        assertThatThrownBy(() -> TimeSlot.of(ELEVEN, TEN)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldNotOverlapWhenSlotsAreBackToBack() {
        var morning = TimeSlot.of(TEN, ELEVEN);
        var noon = TimeSlot.of(ELEVEN, TWELVE);

        assertThat(morning.overlaps(noon)).isFalse();
        assertThat(noon.overlaps(morning)).isFalse();
    }

    @Test
    void shouldOverlapWhenSlotsShareTime() {
        var morning = TimeSlot.of(TEN, TWELVE);
        var afternoon = TimeSlot.of(ELEVEN, ONE);

        assertThat(morning.overlaps(afternoon)).isTrue();
        assertThat(afternoon.overlaps(morning)).isTrue();
    }

    @Test
    void shouldOverlapWhenOneSlotContainsTheOther() {
        var outer = TimeSlot.of(TEN, TWO);
        var inner = TimeSlot.of(ELEVEN, TWELVE);

        assertThat(outer.overlaps(inner)).isTrue();
        assertThat(inner.overlaps(outer)).isTrue();
    }

    @Test
    void shouldContainItsStartButNotItsEnd() {
        var slot = TimeSlot.of(TEN, ELEVEN);

        assertThat(slot.contains(TEN)).isTrue();
        assertThat(slot.contains(ELEVEN)).isFalse();
    }

    @Test
    void shouldBePastOnlyOnceItEnded() {
        var slot = TimeSlot.of(TEN, ELEVEN);

        assertThat(slot.isPast(HALF_PAST_TEN)).isFalse();
        assertThat(slot.isPast(ELEVEN)).isTrue();
        assertThat(slot.isPast(TWELVE)).isTrue();
    }

    @Test
    void shouldDetectAStartInThePast() {
        var slot = TimeSlot.of(TEN, ELEVEN);

        assertThat(slot.startsInThePast(TEN)).isFalse();
        assertThat(slot.startsInThePast(ELEVEN)).isTrue();
    }

    @Test
    void shouldReportItsDuration() {
        assertThat(TimeSlot.of(TEN, TWELVE).duration()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(TimeSlot.of(TEN, ELEVEN)).isEqualTo(TimeSlot.of(TEN, ELEVEN));
    }
}
