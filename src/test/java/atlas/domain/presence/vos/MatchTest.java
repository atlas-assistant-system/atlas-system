package atlas.domain.presence.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.presence.entities.FaceTemplateId;
import atlas.domain.sharedkernel.exceptions.GuardException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchTest {

    private static final FaceTemplateId TEMPLATE_ID =
        FaceTemplateId.of(UUID.fromString("6f9b6f0a-8c2d-4e7b-9c1a-2d3e4f5a6b7c"));
    private static final SimilarityScore SCORE = SimilarityScore.of(0.92);

    @Test
    void shouldExposeItsComponents() {
        var match = Match.of(TEMPLATE_ID, SCORE);

        assertThat(match.templateId()).isEqualTo(TEMPLATE_ID);
        assertThat(match.score()).isEqualTo(SCORE);
    }

    @Test
    void shouldThrowWhenTemplateIdIsMissing() {
        assertThatThrownBy(() -> Match.of(null, SCORE)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldThrowWhenScoreIsMissing() {
        assertThatThrownBy(() -> Match.of(TEMPLATE_ID, null)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(Match.of(TEMPLATE_ID, SCORE)).isEqualTo(Match.of(TEMPLATE_ID, SCORE));
    }
}
