package atlas.domain.presence.services;

import atlas.domain.presence.entities.FaceTemplate;
import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.Match;
import atlas.domain.presence.vos.MatchThreshold;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class FaceMatcher {

    
    public Optional<Match> bestMatch(
        FaceDescriptor candidate, List<FaceTemplate> enrolled, MatchThreshold threshold) {

        return enrolled.stream()
            .filter(template -> template.descriptor().isComparableWith(candidate))
            .map(template -> Match.of(template.id(), candidate.similarity(template.descriptor())))
            .max(Comparator.comparing(match -> match.score().value()))
            .filter(match -> match.score().meets(threshold));
    }
}
