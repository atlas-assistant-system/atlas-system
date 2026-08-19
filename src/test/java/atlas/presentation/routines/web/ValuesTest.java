package atlas.presentation.routines.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.routines.enums.RecurrencePeriod;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sharedkernel.domain.exceptions.FormatException;

class ValuesTest {

    @Nested
    class Text {

        @Test
        void shouldReturnNullWhenAbsent() {
            assertThat(Values.text(Map.of(), "name")).isNull();
        }

        @Test
        void shouldRejectANonString() {
            assertThatThrownBy(() -> Values.text(Map.of("name", 1), "name"))
                .isInstanceOf(FormatException.class)
                .hasMessageContaining("'name'");
        }
    }

    @Nested
    class Decimal {

        @Test
        void shouldReadANumber() {
            assertThat(Values.decimal(Map.of("target", 1.5), "target")).isEqualByComparingTo("1.5");
        }

        @Test
        void shouldReadANumberWrittenAsText() {
            assertThat(Values.decimal(Map.of("target", "2.25"), "target")).isEqualByComparingTo("2.25");
        }

        @Test
        void shouldReturnNullWhenAbsent() {
            assertThat(Values.decimal(Map.of(), "target")).isNull();
        }

        @Test
        void shouldRejectSomethingThatIsNotANumber() {
            assertThatThrownBy(() -> Values.decimal(Map.of("target", "mucho"), "target"))
                .isInstanceOf(FormatException.class);
            assertThatThrownBy(() -> Values.decimal(Map.of("target", true), "target"))
                .isInstanceOf(FormatException.class);
        }
    }

    @Nested
    class Flag {

        @Test
        void shouldBeFalseWhenAbsent() {
            assertThat(Values.flag(Map.of(), "archived")).isFalse();
        }

        @Test
        void shouldRejectANonBoolean() {
            assertThatThrownBy(() -> Values.flag(Map.of("archived", "yes"), "archived"))
                .isInstanceOf(FormatException.class);
        }
    }

    @Nested
    class Weekdays {

        @Test
        void shouldReadWeekdayNames() {
            var days = Values.weekdays(Map.of("activeDays", List.of("MONDAY", "friday")), "activeDays");

            assertThat(days).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        }

        @Test
        void shouldBeEmptyWhenAbsent() {
            assertThat(Values.weekdays(Map.of(), "activeDays")).isEmpty();
        }

        @Test
        void shouldRejectAnUnknownWeekday() {
            assertThatThrownBy(() -> Values.weekdays(Map.of("activeDays", List.of("LUNES")), "activeDays"))
                .isInstanceOf(FormatException.class);
        }

        @Test
        void shouldRejectSomethingThatIsNotAnArray() {
            assertThatThrownBy(() -> Values.weekdays(Map.of("activeDays", "MONDAY"), "activeDays"))
                .isInstanceOf(FormatException.class);
        }
    }

    @Nested
    class DaysOfMonth {

        @Test
        void shouldReadTheDaysIncludingTheLastDayMarker() {
            assertThat(Values.daysOfMonth(Map.of("daysOfMonth", List.of(0, 15)), "daysOfMonth"))
                .containsExactlyInAnyOrder(0, 15);
        }

        @Test
        void shouldBeEmptyWhenAbsent() {
            assertThat(Values.daysOfMonth(Map.of(), "daysOfMonth")).isEmpty();
        }

        @Test
        void shouldRejectANonNumber() {
            assertThatThrownBy(() -> Values.daysOfMonth(Map.of("daysOfMonth", List.of("ultimo")), "daysOfMonth"))
                .isInstanceOf(FormatException.class);
        }
    }

    @Nested
    class Dates {

        @Test
        void shouldParseAnIsoDate() {
            assertThat(Values.parseDate("2026-02-14", "day")).isEqualTo(LocalDate.of(2026, 2, 14));
        }

        @Test
        void shouldRejectAMissingDate() {
            assertThatThrownBy(() -> Values.parseDate(null, "day"))
                .isInstanceOf(FormatException.class)
                .hasMessageContaining("required");
        }

        @Test
        void shouldRejectAMalformedDate() {
            assertThatThrownBy(() -> Values.parseDate("14/02/2026", "day"))
                .isInstanceOf(FormatException.class)
                .hasMessageContaining("2026-02-14");
        }
    }

    @Nested
    class Enums {

        @Test
        void shouldParseCaseInsensitively() {
            assertThat(Values.parseEnum("week", RecurrencePeriod.class, "period"))
                .isEqualTo(RecurrencePeriod.WEEK);
        }

        @Test
        void shouldListTheAllowedValuesWhenRejecting() {
            assertThatThrownBy(() -> Values.parseEnum("FORTNIGHT", RecurrencePeriod.class, "period"))
                .isInstanceOf(FormatException.class)
                .hasMessageContaining("DAY")
                .hasMessageContaining("MONTH");
        }
    }

    @Nested
    class Flags {

        @Test
        void shouldBeFalseWhenTheQueryParamIsAbsent() {
            assertThat(Values.parseFlag(Optional.empty())).isFalse();
        }

        @Test
        void shouldReadTheQueryParam() {
            assertThat(Values.parseFlag(Optional.of("true"))).isTrue();
            assertThat(Values.parseFlag(Optional.of("nope"))).isFalse();
        }
    }
}
