package atlas.domain.appointments;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record AppointmentId(long value) {

    private static final char PREFIX = 'A';
    private static final int NUMERIC_LENGTH = 8;

    public AppointmentId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static AppointmentId of(long value) {
        return new AppointmentId(value);
    }

    public static AppointmentId parse(String text) {
        return new AppointmentId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<AppointmentId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(AppointmentId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
