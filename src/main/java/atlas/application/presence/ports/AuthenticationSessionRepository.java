package atlas.application.presence.ports;

import atlas.domain.presence.AuthenticationSession;
import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.SessionId;
import java.util.List;
import java.util.Optional;
import sharedkernel.application.ports.Repository;

public interface AuthenticationSessionRepository extends Repository<AuthenticationSession, SessionId> {

    SessionId nextId();

    List<AuthenticationSession> findActive();

    Optional<AuthenticationSession> findActiveByProfile(BiometricProfileId profileId);
}
