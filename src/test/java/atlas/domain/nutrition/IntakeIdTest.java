package atlas.domain.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IntakeIdTest {

    @Test
    void shouldRenderWithItsOwnPrefix() {
        assertThat(IntakeId.of(7)).hasToString("I00000007");
    }

    @Test
    void shouldParseBackTheValueItRendered() {
        assertThat(IntakeId.parse("I00000007")).isEqualTo(IntakeId.of(7));
    }

    @Test
    void shouldTryParseIntoTheSameId() {
        assertThat(IntakeId.tryParse("I00000007")).contains(IntakeId.of(7));
    }

    @ParameterizedTest
    @ValueSource(strings = {"M00000007", "I7", "I000000AA", ""})
    void shouldNotParseSomethingThatIsNotAIntakeId(String text) {
        assertThat(IntakeId.tryParse(text)).isEmpty();
    }
}
