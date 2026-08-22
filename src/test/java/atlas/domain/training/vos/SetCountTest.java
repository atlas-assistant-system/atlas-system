package atlas.domain.training.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.training.WorkoutErrors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SetCountTest {

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 21, 48})
    void shouldFailWhenTheCountIsNotSomethingAnybodyDoes(int value) {
        var result = SetCount.create(value);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(WorkoutErrors.SET_COUNT_OUT_OF_RANGE);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 20})
    void shouldAcceptACountInsideTheRange(int value) {
        assertThat(SetCount.create(value).value().value()).isEqualTo(value);
    }

    @Test
    void shouldRejectACountOutOfRangeBuiltDirectly() {
        assertThatThrownBy(() -> new SetCount(0)).isInstanceOf(GuardException.class);
    }
}
