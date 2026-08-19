package atlas.domain.presence.enums;

public enum SessionStatus {

    ACTIVE,
    EXPIRED,
    CLOSED;

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isExpired() {
        return this == EXPIRED;
    }

    public boolean isClosed() {
        return this == CLOSED;
    }
}
