package atlas.domain.presence.vos;

import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.guards.StringGuard;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

public record ChallengeNonce(String value) implements SingleValueObject<String> {

    public static final int NONCE_BYTES = 16;

    private static final Pattern FORMAT = Pattern.compile("[0-9a-f]{32}");

    public ChallengeNonce {
        StringGuard.matches(value, FORMAT, "value");
    }

    public static ChallengeNonce generate(RandomGenerator random) {
        ObjectGuard.notNull(random, "random");

        var bytes = new byte[NONCE_BYTES];
        random.nextBytes(bytes);

        return new ChallengeNonce(HexFormat.of().formatHex(bytes));
    }

    public static ChallengeNonce of(String value) {
        return new ChallengeNonce(value);
    }

    public boolean matches(ChallengeNonce other) {
        ObjectGuard.notNull(other, "other");

        return MessageDigest.isEqual(
            value.getBytes(StandardCharsets.UTF_8), other.value.getBytes(StandardCharsets.UTF_8));
    }
}
