package sharedkernel.domain.types;

import java.util.Optional;
import sharedkernel.domain.exceptions.FormatException;
import sharedkernel.domain.exceptions.GuardException;

public final class PrefixedIds {

    private PrefixedIds() {}

    public static String format(char prefix, int numericLength, long value) {
        if (value < 0) {
            throw GuardException.forParameter("value", "cannot be negative");
        }

        long maxValue = (long) Math.pow(10, numericLength) - 1;
        if (value > maxValue) {
            throw GuardException.forParameter("value", "exceeds maximum (" + maxValue + ")");
        }

        return prefix + String.format("%0" + numericLength + "d", value);
    }

    public static long parse(char prefix, int numericLength, String text) {
        int expectedLength = 1 + numericLength;
        if (text == null || text.length() != expectedLength) {
            throw new FormatException("ID must be " + expectedLength + " characters long.");
        }

        if (text.charAt(0) != prefix) {
            throw new FormatException("ID must start with '" + prefix + "'.");
        }

        try {
            return Long.parseLong(text.substring(1));
        } catch (NumberFormatException e) {
            throw new FormatException("ID must contain only digits after the prefix.");
        }
    }

    public static Optional<Long> tryParse(char prefix, int numericLength, String text) {
        try {
            return Optional.of(parse(prefix, numericLength, text));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
