package atlas.infrastructure.sharedkernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.GuardException;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class SecureTokensTest {

    @Test
    void shouldBeUrlSafeWhenGenerated() {
        assertThat(SecureTokens.newToken()).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void shouldNotRepeatWhenGeneratedManyTimes() {
        var seen = new HashSet<String>();
        for (int i = 0; i < 1000; i++) {
            seen.add(SecureTokens.newToken());
        }

        assertThat(seen).hasSize(1000);
    }

    @Test
    void shouldRejectShortLengthWhenBelowMinimum() {
        assertThatThrownBy(() -> SecureTokens.newToken(SecureTokens.MINIMUM_LENGTH_IN_BYTES - 1))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("at least");
    }

    @Test
    void shouldBeShortAndHexadecimalWhenCorrelationId() {
        assertThat(SecureTokens.newCorrelationId()).matches("[0-9a-f]{8}");
    }
}
