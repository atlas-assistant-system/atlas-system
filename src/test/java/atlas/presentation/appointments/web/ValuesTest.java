package atlas.presentation.appointments.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.appointments.enums.CalendarPeriod;
import atlas.domain.sharedkernel.exceptions.FormatException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ValuesTest {

    @Test
    void shouldReadTypedFieldsFromABody() {
        var body = Map.<String, Object>of(
            "title", "Dentista", "minutes", 15, "allowOverlap", true, "start", "2026-08-20T11:00");

        assertThat(Values.text(body, "title")).isEqualTo("Dentista");
        assertThat(Values.integer(body, "minutes")).isEqualTo(15);
        assertThat(Values.flag(body, "allowOverlap")).isTrue();
        assertThat(Values.dateTime(body, "start")).isEqualTo(LocalDateTime.of(2026, 8, 20, 11, 0));
    }

    @Test
    void shouldTreatAbsentOptionalFieldsAsDefaults() {
        assertThat(Values.text(Map.of(), "description")).isNull();
        assertThat(Values.flag(Map.of(), "allowOverlap")).isFalse();
        assertThat(Values.integers(Map.of(), "reminderLeadTimesMinutes")).isEmpty();
        assertThat(Values.parseInteger(Optional.empty(), "page", 1)).isEqualTo(1);
    }

    @Test
    void shouldFailWithAFormatErrorWhenADateIsNotIso() {
        assertThatThrownBy(() -> Values.parseDateTime("manana", "start")).isInstanceOf(FormatException.class);
        assertThatThrownBy(() -> Values.dateTime(Map.of(), "start")).isInstanceOf(FormatException.class);
    }

    @Test
    void shouldFailWithAFormatErrorWhenATypeDoesNotMatch() {
        assertThatThrownBy(() -> Values.integer(Map.of("minutes", "quince"), "minutes"))
            .isInstanceOf(FormatException.class);
        assertThatThrownBy(() -> Values.integers(Map.of("values", "no"), "values"))
            .isInstanceOf(FormatException.class);
        assertThatThrownBy(() -> Values.parseInteger(Optional.of("dos"), "page", 1))
            .isInstanceOf(FormatException.class);
    }

    @Test
    void shouldParseEnumsIgnoringCase() {
        assertThat(Values.parseEnum("week", CalendarPeriod.class, "period")).isEqualTo(CalendarPeriod.WEEK);
        assertThatThrownBy(() -> Values.parseEnum("decade", CalendarPeriod.class, "period"))
            .isInstanceOf(FormatException.class);
    }

    @Test
    void shouldParseCalendarValues() {
        assertThat(Values.parseYear("2026", "year").getValue()).isEqualTo(2026);
        assertThat(Values.parseYearMonth("2026-08", "month").getMonthValue()).isEqualTo(8);
        assertThatThrownBy(() -> Values.parseYearMonth("agosto", "month")).isInstanceOf(FormatException.class);
    }
}
