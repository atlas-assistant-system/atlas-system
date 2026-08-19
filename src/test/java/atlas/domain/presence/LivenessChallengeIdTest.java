package atlas.domain.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import sharedkernel.domain.exceptions.FormatException;
import sharedkernel.domain.exceptions.GuardException;

class LivenessChallengeIdTest {

    @Test
    void shouldRenderAsPrefixedTextWithLeadingZeros() {
        assertThat(LivenessChallengeId.of(7)).hasToString("L00000007");
    }

    @Test
    void shouldParseItsOwnTextForm() {
        assertThat(LivenessChallengeId.parse("L00000007")).isEqualTo(LivenessChallengeId.of(7));
    }

    @Test
    void shouldRejectNegativeValues() {
        assertThatThrownBy(() -> LivenessChallengeId.of(-1)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldFailToParseTextWithTheWrongPrefix() {
        assertThatThrownBy(() -> LivenessChallengeId.parse("S00000007")).isInstanceOf(FormatException.class);
    }

    @Test
    void shouldReturnEmptyWhenTextCannotBeParsed() {
        assertThat(LivenessChallengeId.tryParse("nope")).isEmpty();
    }

    @Test
    void shouldReturnIdWhenTextCanBeParsed() {
        assertThat(LivenessChallengeId.tryParse("L00000007")).contains(LivenessChallengeId.of(7));
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(LivenessChallengeId.of(42)).isEqualTo(LivenessChallengeId.of(42));
    }
}
