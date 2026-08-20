package atlas.domain.economy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SavingsGoalIdTest {

    @Test
    void shouldRenderWithItsOwnPrefix() {
        assertThat(SavingsGoalId.of(5)).hasToString("O00000005");
    }

    @Test
    void shouldParseBackTheValueItRendered() {
        assertThat(SavingsGoalId.parse("O00000005")).isEqualTo(SavingsGoalId.of(5));
    }

    @Test
    void shouldTryParseIntoTheSameId() {
        assertThat(SavingsGoalId.tryParse("O00000005")).contains(SavingsGoalId.of(5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"P00000005", "O5", "O0000000A", ""})
    void shouldNotParseSomethingThatIsNotASavingsGoalId(String text) {
        assertThat(SavingsGoalId.tryParse(text)).isEmpty();
    }
}
