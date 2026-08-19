package atlas.domain.presence.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SessionStatusTest {

    @Test
    void shouldConsiderOnlyActiveAsActive() {
        assertThat(SessionStatus.ACTIVE.isActive()).isTrue();
        assertThat(SessionStatus.EXPIRED.isActive()).isFalse();
        assertThat(SessionStatus.CLOSED.isActive()).isFalse();
    }

    @Test
    void shouldConsiderOnlyExpiredAsExpired() {
        assertThat(SessionStatus.EXPIRED.isExpired()).isTrue();
        assertThat(SessionStatus.ACTIVE.isExpired()).isFalse();
        assertThat(SessionStatus.CLOSED.isExpired()).isFalse();
    }

    @Test
    void shouldConsiderOnlyClosedAsClosed() {
        assertThat(SessionStatus.CLOSED.isClosed()).isTrue();
        assertThat(SessionStatus.ACTIVE.isClosed()).isFalse();
        assertThat(SessionStatus.EXPIRED.isClosed()).isFalse();
    }

    @Test
    void shouldRoundTripThroughItsName() {
        assertThat(SessionStatus.valueOf("ACTIVE")).isEqualTo(SessionStatus.ACTIVE);
        assertThat(SessionStatus.CLOSED.name()).isEqualTo("CLOSED");
    }
}
