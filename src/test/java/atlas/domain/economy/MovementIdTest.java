package atlas.domain.economy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MovementIdTest {

    @Test
    void shouldRenderWithItsOwnPrefix() {
        assertThat(MovementId.of(7)).hasToString("M00000007");
    }

    @Test
    void shouldParseBackTheValueItRendered() {
        assertThat(MovementId.parse("M00000007")).isEqualTo(MovementId.of(7));
    }

    @ParameterizedTest
    @ValueSource(strings = {"R00000007", "M7", "M0000000A", ""})
    void shouldNotParseSomethingThatIsNotAMovementId(String text) {
        assertThat(MovementId.tryParse(text)).isEmpty();
    }
}
