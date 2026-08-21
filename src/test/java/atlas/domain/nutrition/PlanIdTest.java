package atlas.domain.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlanIdTest {

    @Test
    void shouldRenderWithItsOwnPrefix() {
        assertThat(PlanId.of(7)).hasToString("N00000007");
    }

    @Test
    void shouldParseBackTheValueItRendered() {
        assertThat(PlanId.parse("N00000007")).isEqualTo(PlanId.of(7));
    }

    @Test
    void shouldTryParseIntoTheSameId() {
        assertThat(PlanId.tryParse("N00000007")).contains(PlanId.of(7));
    }

    @ParameterizedTest
    @ValueSource(strings = {"M00000007", "N7", "N000000AA", ""})
    void shouldNotParseSomethingThatIsNotAPlanId(String text) {
        assertThat(PlanId.tryParse(text)).isEmpty();
    }
}
