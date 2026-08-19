package atlas.domain.routines.vos;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.routines.RoutineErrors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class RoutineTextValueObjectsTest {

    @Nested
    class Name {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   ", "\t\n"})
        void shouldFailWhenBlank(String value) {
            var result = RoutineName.create(value);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.error()).isEqualTo(RoutineErrors.NAME_REQUIRED);
        }

        @Test
        void shouldTrimSurroundingWhitespace() {
            assertThat(RoutineName.create("  Tomar pastillas  ").value().value()).isEqualTo("Tomar pastillas");
        }

        @Test
        void shouldAcceptExactlyTheMaximumLength() {
            assertThat(RoutineName.create("a".repeat(RoutineName.MAX_LENGTH)).isSuccess()).isTrue();
        }

        @Test
        void shouldFailWhenLongerThanTheMaximum() {
            var result = RoutineName.create("a".repeat(RoutineName.MAX_LENGTH + 1));

            assertThat(result.error()).isEqualTo(RoutineErrors.NAME_TOO_LONG);
        }

        @Test
        void shouldMeasureLengthAfterTrimming() {
            assertThat(RoutineName.create("  " + "a".repeat(RoutineName.MAX_LENGTH) + "  ").isSuccess()).isTrue();
        }
    }

    @Nested
    class Description {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void shouldBeAbsentWhenBlank(String value) {
            var result = RoutineDescription.create(value);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.value()).isEmpty();
        }

        @Test
        void shouldTrimSurroundingWhitespace() {
            assertThat(RoutineDescription.create("  Con el desayuno  ").value())
                .map(RoutineDescription::value)
                .contains("Con el desayuno");
        }

        @Test
        void shouldAcceptExactlyTheMaximumLength() {
            assertThat(RoutineDescription.create("a".repeat(RoutineDescription.MAX_LENGTH)).isSuccess()).isTrue();
        }

        @Test
        void shouldFailWhenLongerThanTheMaximum() {
            var result = RoutineDescription.create("a".repeat(RoutineDescription.MAX_LENGTH + 1));

            assertThat(result.error()).isEqualTo(RoutineErrors.DESCRIPTION_TOO_LONG);
        }
    }

    @Nested
    class TargetUnit {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "  "})
        void shouldBeAbsentWhenBlank(String value) {
            assertThat(Unit.create(value).value()).isEmpty();
        }

        @Test
        void shouldAcceptTheUsualUnits() {
            assertThat(Unit.create("L").value()).map(Unit::value).contains("L");
            assertThat(Unit.create("paginas").value()).isPresent();
            assertThat(Unit.create("veces").value()).isPresent();
        }

        @Test
        void shouldFailWhenLongerThanTheMaximum() {
            var result = Unit.create("a".repeat(Unit.MAX_LENGTH + 1));

            assertThat(result.error()).isEqualTo(RoutineErrors.UNIT_TOO_LONG);
        }
    }
}
