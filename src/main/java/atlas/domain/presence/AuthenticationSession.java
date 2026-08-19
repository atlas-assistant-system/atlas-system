package atlas.domain.presence;

import atlas.domain.presence.enums.SessionStatus;
import atlas.domain.presence.events.SessionClosedEvent;
import atlas.domain.presence.events.SessionExpiredEvent;
import atlas.domain.presence.events.SessionOpenedEvent;
import atlas.domain.presence.events.SessionRefreshedEvent;
import atlas.domain.presence.vos.SessionDuration;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;

public final class AuthenticationSession extends AggregateRoot<SessionId> {

    private final BiometricProfileId profileId;
    private final Instant openedAt;
    private Instant lastActivityAt;
    private Instant expiresAt;
    private SessionStatus status;

    private AuthenticationSession(
        SessionId id,
        BiometricProfileId profileId,
        Instant openedAt,
        Instant lastActivityAt,
        Instant expiresAt,
        SessionStatus status) {
        super(id);
        this.profileId = ObjectGuard.notNull(profileId, "profileId");
        this.openedAt = ObjectGuard.notNull(openedAt, "openedAt");
        this.lastActivityAt = ObjectGuard.notNull(lastActivityAt, "lastActivityAt");
        this.expiresAt = ObjectGuard.notNull(expiresAt, "expiresAt");
        this.status = ObjectGuard.notNull(status, "status");
    }

    public static AuthenticationSession open(
        SessionId id, BiometricProfileId profileId, SessionDuration duration, Instant now) {

        var session = new AuthenticationSession(
            id, profileId, now, now, now.plus(duration.value()), SessionStatus.ACTIVE);
        session.registerEvent(new SessionOpenedEvent(id, now));

        return session;
    }

    public static AuthenticationSession rehydrate(
        SessionId id,
        BiometricProfileId profileId,
        Instant openedAt,
        Instant lastActivityAt,
        Instant expiresAt,
        SessionStatus status) {

        return new AuthenticationSession(id, profileId, openedAt, lastActivityAt, expiresAt, status);
    }

    public Result<Void> refresh(SessionDuration duration, Instant now) {
        if (status.isClosed()) {
            return Result.failure(PresenceErrors.SESSION_CLOSED);
        }

        if (status.isExpired() || !now.isBefore(expiresAt)) {
            return Result.failure(PresenceErrors.SESSION_EXPIRED);
        }

        this.lastActivityAt = now;
        this.expiresAt = now.plus(duration.value());
        registerEvent(new SessionRefreshedEvent(id(), now));

        return Result.success();
    }

    public boolean expire(Instant now) {
        if (!status.isActive() || now.isBefore(expiresAt)) {
            return false;
        }

        this.status = SessionStatus.EXPIRED;
        registerEvent(new SessionExpiredEvent(id(), now));

        return true;
    }

    public void close(Instant now) {
        if (status.isClosed()) {
            return;
        }

        this.status = SessionStatus.CLOSED;
        registerEvent(new SessionClosedEvent(id(), now));
    }

    public boolean isActive(Instant now) {
        return status.isActive() && now.isBefore(expiresAt);
    }

    public BiometricProfileId profileId() {
        return profileId;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public Instant lastActivityAt() {
        return lastActivityAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public SessionStatus status() {
        return status;
    }
}
