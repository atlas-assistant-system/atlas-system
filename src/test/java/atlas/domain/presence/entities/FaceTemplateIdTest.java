package atlas.domain.presence.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import sharedkernel.domain.exceptions.GuardException;

class FaceTemplateIdTest {

    private static final UUID VALUE = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void shouldRenderAsItsUuidText() {
        assertThat(FaceTemplateId.of(VALUE)).hasToString("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void shouldExposeItsValue() {
        assertThat(FaceTemplateId.of(VALUE).value()).isEqualTo(VALUE);
    }

    @Test
    void shouldRejectNullValues() {
        assertThatThrownBy(() -> FaceTemplateId.of(null)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(FaceTemplateId.of(VALUE)).isEqualTo(FaceTemplateId.of(VALUE));
    }
}
