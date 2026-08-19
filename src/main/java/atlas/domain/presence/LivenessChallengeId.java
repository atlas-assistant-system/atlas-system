package atlas.domain.presence;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record LivenessChallengeId(long value) {

    private static final char PREFIX = 'L';
    private static final int NUMERIC_LENGTH = 8;

    public LivenessChallengeId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static LivenessChallengeId of(long value) {
        return new LivenessChallengeId(value);
    }

    public static LivenessChallengeId parse(String text) {
        return new LivenessChallengeId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<LivenessChallengeId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(LivenessChallengeId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
