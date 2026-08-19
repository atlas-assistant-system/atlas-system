package atlas.infrastructure.appointments.persistence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class Timestamps {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    private Timestamps() {}

    static String format(LocalDateTime moment) {
        return FORMAT.format(moment);
    }

    static LocalDateTime parse(String text) {
        return LocalDateTime.parse(text, FORMAT);
    }
}
