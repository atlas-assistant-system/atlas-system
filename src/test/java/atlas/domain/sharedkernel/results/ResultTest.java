package atlas.domain.sharedkernel.results;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ResultTest {

    private static final Error SOME_ERROR = Error.validation("Test.Invalid", "Invalid value.");

    @Test
    void shouldExposeValueWhenSuccessful() {
        var result = Result.success(42);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.value()).isEqualTo(42);
    }

    @Test
    void shouldExposeErrorWhenFailed() {
        var result = Result.failure(SOME_ERROR);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.error()).isEqualTo(SOME_ERROR);
    }

    @Test
    void shouldAllowNullValueWhenSuccessWithoutValue() {
        var result = Result.success();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isNull();
    }

    @Test
    void shouldThrowWhenAccessingValueOfFailure() {
        var result = Result.failure(SOME_ERROR);

        assertThatThrownBy(result::value)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Test.Invalid");
    }

    @Test
    void shouldThrowWhenAccessingErrorOfSuccess() {
        var result = Result.success(42);

        assertThatThrownBy(result::error)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldSupportPatternMatchingWhenSwitchingOverResult() {
        Result<Integer> result = Result.failure(SOME_ERROR);

        var description = switch (result) {
            case Result.Success<Integer> success -> "ok:" + success.value();
            case Result.Failure<Integer> failure -> "error:" + failure.error().code();
        };

        assertThat(description).isEqualTo("error:Test.Invalid");
    }
}
