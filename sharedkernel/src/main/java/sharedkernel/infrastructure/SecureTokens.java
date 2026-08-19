package sharedkernel.infrastructure;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import sharedkernel.domain.exceptions.GuardException;

public final class SecureTokens {

    public static final int DEFAULT_LENGTH_IN_BYTES = 32;
    public static final int MINIMUM_LENGTH_IN_BYTES = 16;
    public static final int CORRELATION_LENGTH_IN_BYTES = 4;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private SecureTokens() {}

    public static String newToken() {
        return newToken(DEFAULT_LENGTH_IN_BYTES);
    }

    public static String newToken(int lengthInBytes) {
        if (lengthInBytes < MINIMUM_LENGTH_IN_BYTES) {
            throw GuardException.forParameter(
                "lengthInBytes", "must be at least " + MINIMUM_LENGTH_IN_BYTES + " bytes for a secure token");
        }

        return ENCODER.encodeToString(randomBytes(lengthInBytes));
    }

    public static String newCorrelationId() {
        return HexFormat.of().formatHex(randomBytes(CORRELATION_LENGTH_IN_BYTES));
    }

    private static byte[] randomBytes(int length) {
        var bytes = new byte[length];
        RANDOM.nextBytes(bytes);

        return bytes;
    }
}
