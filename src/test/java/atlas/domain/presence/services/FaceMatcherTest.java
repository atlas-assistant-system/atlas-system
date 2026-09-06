package atlas.domain.presence.services;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.presence.entities.FaceTemplate;
import atlas.domain.presence.entities.FaceTemplateId;
import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.MatchThreshold;
import atlas.domain.presence.vos.ModelVersion;
import atlas.domain.presence.vos.SimilarityScore;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FaceMatcherTest {

    private static final Instant CAPTURED_AT = Instant.parse("2026-08-18T10:00:00Z");
    private static final ModelVersion MODEL = ModelVersion.of("arcface-r100-v1");
    private static final ModelVersion ANOTHER_MODEL = ModelVersion.of("arcface-r100-v2");
    private static final FaceDescriptor CANDIDATE = FaceDescriptor.of(MODEL, new float[]{1.0f, 0.0f});

    private final FaceMatcher matcher = new FaceMatcher();

    @Test
    void shouldFindNoMatchWhenNothingIsEnrolled() {
        var match = matcher.bestMatch(CANDIDATE, List.of(), MatchThreshold.of(0.6));

        assertThat(match).isEmpty();
    }

    @Test
    void shouldPickTheTemplateWithTheHighestSimilarity() {
        var enrolled = List.of(template(1, 20.0f, 0.0f), template(2, 9.0f, 0.0f), template(3, 1.0f, 0.0f));

        var match = matcher.bestMatch(CANDIDATE, enrolled, MatchThreshold.of(0.6));

        assertThat(match).hasValueSatisfying(best -> {
            assertThat(best.templateId()).isEqualTo(templateId(3));
            assertThat(best.score()).isEqualTo(SimilarityScore.of(1.0));
        });
    }

    @Test
    void shouldMatchWhenTheBestScoreEqualsTheThresholdExactly() {
        var enrolled = List.of(template(1, 9.8f, 0.0f));

        var match = matcher.bestMatch(CANDIDATE, enrolled, MatchThreshold.of(0.6));

        assertThat(match).hasValueSatisfying(best -> {
            assertThat(best.templateId()).isEqualTo(templateId(1));
            assertThat(best.score()).isEqualTo(SimilarityScore.of(0.6));
        });
    }

    @Test
    void shouldFindNoMatchWhenTheBestScoreIsJustBelowTheThreshold() {
        var enrolled = List.of(template(1, 9.9f, 0.0f));

        var match = matcher.bestMatch(CANDIDATE, enrolled, MatchThreshold.of(0.601));

        assertThat(match).isEmpty();
    }

    @Test
    void shouldSkipAnEnrolledTemplateFromAnotherModelVersionInsteadOfFailing() {
        var foreign = FaceTemplate.create(
            templateId(2), FaceDescriptor.of(ANOTHER_MODEL, new float[]{1.0f, 0.0f}), CAPTURED_AT);
        var enrolled = List.of(foreign, template(1, 1.0f, 0.0f));

        var match = matcher.bestMatch(CANDIDATE, enrolled, MatchThreshold.of(0.6));

        assertThat(match).hasValueSatisfying(best -> assertThat(best.templateId()).isEqualTo(templateId(1)));
    }

    @Test
    void shouldSkipAnEnrolledTemplateWithADifferentDimensionInsteadOfFailing() {
        var mismatched = template(1, 1.0f, 0.0f, 0.0f);
        var comparable = template(2, 1.0f, 0.0f);
        var enrolled = List.of(mismatched, comparable);

        var match = matcher.bestMatch(CANDIDATE, enrolled, MatchThreshold.of(0.6));

        assertThat(match).hasValueSatisfying(best -> assertThat(best.templateId()).isEqualTo(templateId(2)));
    }

    @Test
    void shouldFindNoMatchWhenEveryEnrolledTemplateIsIncomparable() {
        var enrolled = List.of(
            FaceTemplate.create(templateId(1), FaceDescriptor.of(ANOTHER_MODEL, new float[]{1.0f, 0.0f}), CAPTURED_AT),
            template(2, 1.0f, 0.0f, 0.0f));

        var match = matcher.bestMatch(CANDIDATE, enrolled, MatchThreshold.of(0.6));

        assertThat(match).isEmpty();
    }

    private static FaceTemplate template(int seed, float... values) {
        return FaceTemplate.create(templateId(seed), FaceDescriptor.of(MODEL, values), CAPTURED_AT);
    }

    private static FaceTemplateId templateId(int seed) {
        return FaceTemplateId.of(new UUID(0, seed));
    }
}
