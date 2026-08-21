package atlas.application.home.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.home.HomeProfile;
import atlas.domain.home.HomeProfileId;

public interface HomeProfileRepository extends Repository<HomeProfile, HomeProfileId> {}
