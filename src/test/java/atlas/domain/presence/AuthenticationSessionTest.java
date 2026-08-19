package atlas.domain.presence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.presence.enums.SessionStatus;
import atlas.domain.presence.events.SessionClosedEvent;
import atlas.domain.presence.events.SessionExpiredEvent;
import atlas.domain.presence.events.SessionOpenedEvent;
import atlas.domain.presence.events.SessionRefreshedEvent;
import atlas.domain.presence.vos.SessionDuration;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuthenticationSessionTest {

    private static final SessionId ID = SessionId.of(1);
    private static final BiometricProfileId PROFILE_ID = BiometricProfileId.of(1);
    private static final SessionDuration THIRTY_MINUTES = SessionDuration.of(Duration.ofMinutes(30));

    private static final Instant OPENED_AT = Instant.parse("2026-08-17T10:00:00Z");
    private static final Instant DURING_THE_SESSION = Instant.parse("2026-08-17T10:10:00Z");
    private static final Instant JUST_BEFORE_EXPIRY = Instant.parse("2026-08-17T10:29:59Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-08-17T10:30:00Z");
    private static final Instant AFTER_EXPIRY = Instant.parse("2026-08-17T11:00:00Z");

    @Test
    void shouldOpenAnActiveSessionCoveringTheWholeDuration() {
        var session = AuthenticationSession.open(ID, PROFILE_ID, THIRTY_MINUTES, OPENED_AT);

        assertThat(session.id()).isEqualTo(ID);
        assertThat(session.profileId()).isEqualTo(PROFILE_ID);
        assertThat(session.status()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.openedAt()).isEqualTo(OPENED_AT);
        assertThat(session.lastActivityAt()).isEqualTo(OPENED_AT);
        assertThat(session.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void shouldRaiseOpenedEventWhenSessionIsOpened() {
        var session = AuthenticationSession.open(ID, PROFILE_ID, THIRTY_MINUTES, OPENED_AT);

        assertThat(session.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(SessionOpenedEvent.class, event -> {
                assertThat(event.sessionId()).isEqualTo(ID);
                assertThat(event.occurredOn()).isEqualTo(OPENED_AT);
            });
    }

    @Test
    void shouldRefreshExtendingExpiryFromTheRefreshInstant() {
        var session = opened();

        var result = session.refresh(THIRTY_MINUTES, DURING_THE_SESSION);

        assertThat(result.isSuccess()).isTrue();
        assertThat(session.lastActivityAt()).isEqualTo(DURING_THE_SESSION);
        assertThat(session.expiresAt()).isEqualTo(Instant.parse("2026-08-17T10:40:00Z"));
        assertThat(session.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(SessionRefreshedEvent.class, event -> {
                assertThat(event.sessionId()).isEqualTo(ID);
                assertThat(event.occurredOn()).isEqualTo(DURING_THE_SESSION);
            });
    }

    @Test
    void shouldFailWithoutMutatingWhenRefreshingExactlyAtExpiry() {
        var session = opened();

        var result = session.refresh(THIRTY_MINUTES, EXPIRES_AT);

        assertThat(result.error()).isEqualTo(PresenceErrors.SESSION_EXPIRED);
        assertThat(session.lastActivityAt()).isEqualTo(OPENED_AT);
        assertThat(session.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(session.pendingEvents()).isEmpty();
    }

    @Test
    void shouldFailWhenRefreshingAClosedSession() {
        var result = closed().refresh(THIRTY_MINUTES, DURING_THE_SESSION);

        assertThat(result.error()).isEqualTo(PresenceErrors.SESSION_CLOSED);
    }

    @Test
    void shouldNeverReviveASessionOnceItExpired() {
        var session = expired();

        var result = session.refresh(THIRTY_MINUTES, AFTER_EXPIRY);

        assertThat(result.error()).isEqualTo(PresenceErrors.SESSION_EXPIRED);
        assertThat(session.status()).isEqualTo(SessionStatus.EXPIRED);
        assertThat(session.pendingEvents()).isEmpty();
    }

    @Test
    void shouldFailWhenRefreshingAnExpiredSessionEvenBeforeItsExpiryInstant() {
        var session = AuthenticationSession.rehydrate(
            ID, PROFILE_ID, OPENED_AT, OPENED_AT, AFTER_EXPIRY, SessionStatus.EXPIRED);

        var result = session.refresh(THIRTY_MINUTES, DURING_THE_SESSION);

        assertThat(result.error()).isEqualTo(PresenceErrors.SESSION_EXPIRED);
    }

    @Test
    void shouldExpireExactlyAtItsExpiryInstant() {
        var session = opened();

        var transitioned = session.expire(EXPIRES_AT);

        assertThat(transitioned).isTrue();
        assertThat(session.status()).isEqualTo(SessionStatus.EXPIRED);
        assertThat(session.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(SessionExpiredEvent.class, event -> {
                assertThat(event.sessionId()).isEqualTo(ID);
                assertThat(event.occurredOn()).isEqualTo(EXPIRES_AT);
            });
    }

    @Test
    void shouldNotExpireWhileTheSessionIsStillAlive() {
        var session = opened();

        var transitioned = session.expire(JUST_BEFORE_EXPIRY);

        assertThat(transitioned).isFalse();
        assertThat(session.status()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.pendingEvents()).isEmpty();
    }

    @Test
    void shouldNotAnnounceAgainWhenExpiredTwice() {
        var session = opened();
        session.expire(EXPIRES_AT);

        var transitioned = session.expire(AFTER_EXPIRY);

        assertThat(transitioned).isFalse();
        assertThat(session.pendingEvents()).hasSize(1);
    }

    @Test
    void shouldNotExpireAClosedSession() {
        var session = closed();

        var transitioned = session.expire(AFTER_EXPIRY);

        assertThat(transitioned).isFalse();
        assertThat(session.status()).isEqualTo(SessionStatus.CLOSED);
        assertThat(session.pendingEvents()).isEmpty();
    }

    @Test
    void shouldCloseAndAnnounceIt() {
        var session = opened();

        session.close(DURING_THE_SESSION);

        assertThat(session.status()).isEqualTo(SessionStatus.CLOSED);
        assertThat(session.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(SessionClosedEvent.class, event -> {
                assertThat(event.sessionId()).isEqualTo(ID);
                assertThat(event.occurredOn()).isEqualTo(DURING_THE_SESSION);
            });
    }

    @Test
    void shouldNotAnnounceAgainWhenClosedTwice() {
        var session = opened();
        session.close(DURING_THE_SESSION);

        session.close(AFTER_EXPIRY);

        assertThat(session.status()).isEqualTo(SessionStatus.CLOSED);
        assertThat(session.pendingEvents()).hasSize(1);
    }

    @Test
    void shouldEmitWhenClosingAnExpiredSession() {
        var session = expired();

        session.close(AFTER_EXPIRY);

        assertThat(session.status()).isEqualTo(SessionStatus.CLOSED);
        assertThat(session.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(SessionClosedEvent.class, event -> {
                assertThat(event.sessionId()).isEqualTo(ID);
                assertThat(event.occurredOn()).isEqualTo(AFTER_EXPIRY);
            });
    }

    @Test
    void shouldBeActiveJustBeforeExpiry() {
        assertThat(opened().isActive(JUST_BEFORE_EXPIRY)).isTrue();
    }

    @Test
    void shouldNotBeActiveExactlyAtItsExpiryInstant() {
        assertThat(opened().isActive(EXPIRES_AT)).isFalse();
    }

    @Test
    void shouldNotBeActiveOnceExpired() {
        assertThat(expired().isActive(DURING_THE_SESSION)).isFalse();
    }

    @Test
    void shouldNotBeActiveOnceClosed() {
        assertThat(closed().isActive(DURING_THE_SESSION)).isFalse();
    }

    @Test
    void shouldRestoreItsStateWhenRehydrated() {
        var session = AuthenticationSession.rehydrate(
            ID, PROFILE_ID, OPENED_AT, DURING_THE_SESSION, EXPIRES_AT, SessionStatus.CLOSED);

        assertThat(session.id()).isEqualTo(ID);
        assertThat(session.profileId()).isEqualTo(PROFILE_ID);
        assertThat(session.openedAt()).isEqualTo(OPENED_AT);
        assertThat(session.lastActivityAt()).isEqualTo(DURING_THE_SESSION);
        assertThat(session.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(session.status()).isEqualTo(SessionStatus.CLOSED);
    }

    @Test
    void shouldNotRaiseAnyEventWhenRehydrated() {
        var session = AuthenticationSession.rehydrate(
            ID, PROFILE_ID, OPENED_AT, OPENED_AT, EXPIRES_AT, SessionStatus.ACTIVE);

        assertThat(session.pendingEvents()).isEmpty();
    }

    private static AuthenticationSession opened() {
        var session = AuthenticationSession.open(ID, PROFILE_ID, THIRTY_MINUTES, OPENED_AT);
        session.clearEvents();

        return session;
    }

    private static AuthenticationSession expired() {
        var session = opened();
        session.expire(EXPIRES_AT);
        session.clearEvents();

        return session;
    }

    private static AuthenticationSession closed() {
        var session = opened();
        session.close(DURING_THE_SESSION);
        session.clearEvents();

        return session;
    }
}
