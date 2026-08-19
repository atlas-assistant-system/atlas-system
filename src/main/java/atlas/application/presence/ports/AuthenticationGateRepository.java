package atlas.application.presence.ports;

import atlas.domain.presence.AuthenticationGate;

public interface AuthenticationGateRepository {

    AuthenticationGate get();

    void save(AuthenticationGate gate);
}
