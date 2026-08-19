package atlas.application.sharedkernel.logging;

import java.util.Locale;

public enum LogKind {

    COMMAND,
    QUERY;

    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}
