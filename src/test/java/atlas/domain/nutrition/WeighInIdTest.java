package atlas.domain.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WeighInIdTest {

    @Test
    void shouldRenderWithItsOwnPrefix() {
        assertThat(WeighInId.of(7)).hasToString("W00000007");
    }

    @Test
    void shouldParseBackTheValueItRendered() {
        assertThat(WeighInId.parse("W00000007")).isEqualTo(WeighInId.of(7));
    }

    @Test
    void shouldTryParseIntoTheSameId() {
        assertThat(WeighInId.tryParse("W00000007")).contains(WeighInId.of(7));
    }

    @ParameterizedTest
    @ValueSource(strings = {"N00000007", "W7", "W000000AA", ""})
    void shouldNotParseSomethingThatIsNotAWeighInId(String text) {
        assertThat(WeighInId.tryParse(text)).isEmpty();
    }
}
