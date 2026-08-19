package atlas.domain.presence.services;

import atlas.domain.presence.entities.FaceTemplate;
import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.Match;
import atlas.domain.presence.vos.MatchThreshold;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class FaceMatcher {

    /**
     * Identification is 1:N: {@code enrolled} may span every profile in the system, and their
     * templates need not share a single {@link atlas.domain.presence.vos.ModelVersion} — a
     * profile that has not been re-enrolled after a model upgrade is expected, not corrupt.
     * Templates that are not comparable with the candidate are silently excluded from
     * consideration rather than aborting the whole match: one stale profile must never crash
     * authentication for every other enrolled person.
     */
    public Optional<Match> bestMatch(
        FaceDescriptor candidate, List<FaceTemplate> enrolled, MatchThreshold threshold) {

        return enrolled.stream()
            .filter(template -> template.descriptor().isComparableWith(candidate))
            .map(template -> Match.of(template.id(), candidate.similarity(template.descriptor())))
            .max(Comparator.comparing(match -> match.score().value()))
            .filter(match -> match.score().meets(threshold));
    }
}
