package atlas.domain.economy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BudgetIdTest {

    @Test
    void shouldRenderWithItsOwnPrefix() {
        assertThat(BudgetId.of(3)).hasToString("P00000003");
    }

    @Test
    void shouldParseBackTheValueItRendered() {
        assertThat(BudgetId.parse("P00000003")).isEqualTo(BudgetId.of(3));
    }

    @Test
    void shouldTryParseIntoTheSameId() {
        assertThat(BudgetId.tryParse("P00000003")).contains(BudgetId.of(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"M00000003", "P3", "P0000000A", ""})
    void shouldNotParseSomethingThatIsNotABudgetId(String text) {
        assertThat(BudgetId.tryParse(text)).isEmpty();
    }
}
