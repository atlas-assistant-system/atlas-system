package atlas.application.presence.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.presence.BiometricProfile;
import atlas.domain.presence.BiometricProfileId;

public interface BiometricProfileRepository extends Repository<BiometricProfile, BiometricProfileId> {

    BiometricProfileId nextId();
}
