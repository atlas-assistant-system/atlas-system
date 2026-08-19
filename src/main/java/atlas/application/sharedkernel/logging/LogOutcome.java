package atlas.application.sharedkernel.logging;

import java.util.Locale;

public enum LogOutcome {

    SUCCESS,
    FAILURE,
    ERROR;

    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}
