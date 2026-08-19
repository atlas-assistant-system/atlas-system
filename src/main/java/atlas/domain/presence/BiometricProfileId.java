package atlas.domain.presence;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record BiometricProfileId(long value) {

    private static final char PREFIX = 'B';
    private static final int NUMERIC_LENGTH = 8;

    public BiometricProfileId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static BiometricProfileId of(long value) {
        return new BiometricProfileId(value);
    }

    public static BiometricProfileId parse(String text) {
        return new BiometricProfileId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<BiometricProfileId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(BiometricProfileId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
