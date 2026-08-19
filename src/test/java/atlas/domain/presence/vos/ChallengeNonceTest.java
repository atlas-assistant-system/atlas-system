package atlas.domain.presence.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import sharedkernel.domain.exceptions.GuardException;

class ChallengeNonceTest {

    private static final String SEED_42_NONCE = "359d41baf78afe0de1bbe7ae28c0450c";
    private static final String SEED_7_NONCE = "99170fbb183477a35a94c9bf390b7702";

    @Test
    void shouldGenerateDeterministicNonceFromSeededRandom() {
        var nonce = ChallengeNonce.generate(new Random(42));

        assertThat(nonce.value()).isEqualTo(SEED_42_NONCE);
    }

    @Test
    void shouldGenerateSameNonceForSameSeed() {
        assertThat(ChallengeNonce.generate(new Random(42))).isEqualTo(ChallengeNonce.generate(new Random(42)));
    }

    @Test
    void shouldGenerateDifferentNoncesForDifferentSeeds() {
        var nonce = ChallengeNonce.generate(new Random(7));

        assertThat(nonce.value()).isEqualTo(SEED_7_NONCE);
        assertThat(nonce).isNotEqualTo(ChallengeNonce.generate(new Random(42)));
    }

    @Test
    void shouldGenerateThirtyTwoLowercaseHexCharacters() {
        var nonce = ChallengeNonce.generate(new Random(42));

        assertThat(nonce.value()).hasSize(32);
        assertThat(nonce.value()).matches("[0-9a-f]{32}");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "",
        "   ",
        "359D41BAF78AFE0DE1BBE7AE28C0450C",
        "359d41baf78afe0de1bbe7ae28c0450",
        "359d41baf78afe0de1bbe7ae28c0450c0",
        "gggggggggggggggggggggggggggggggg"
    })
    void shouldThrowWhenValueIsNotThirtyTwoLowercaseHexCharacters(String candidate) {
        assertThatThrownBy(() -> ChallengeNonce.of(candidate)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldThrowWhenRandomGeneratorIsMissing() {
        assertThatThrownBy(() -> ChallengeNonce.generate(null)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldMatchWhenNoncesCarryTheSameValue() {
        assertThat(ChallengeNonce.of(SEED_42_NONCE).matches(ChallengeNonce.of(SEED_42_NONCE))).isTrue();
    }

    @Test
    void shouldNotMatchWhenNoncesDiffer() {
        assertThat(ChallengeNonce.of(SEED_42_NONCE).matches(ChallengeNonce.of(SEED_7_NONCE))).isFalse();
    }

    @Test
    void shouldThrowWhenMatchingAgainstNull() {
        var nonce = ChallengeNonce.of(SEED_42_NONCE);

        assertThatThrownBy(() -> nonce.matches(null)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(ChallengeNonce.of(SEED_42_NONCE)).isEqualTo(ChallengeNonce.of(SEED_42_NONCE));
    }
}
