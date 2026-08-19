package atlas.application.presence.ports;

import atlas.domain.presence.BiometricProfile;
import atlas.domain.presence.BiometricProfileId;
import sharedkernel.application.ports.Repository;

public interface BiometricProfileRepository extends Repository<BiometricProfile, BiometricProfileId> {

    BiometricProfileId nextId();
}
