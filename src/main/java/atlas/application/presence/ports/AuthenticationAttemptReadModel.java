package atlas.application.presence.ports;

import atlas.application.sharedkernel.paging.Page;
import atlas.application.sharedkernel.paging.PageRequest;

public interface AuthenticationAttemptReadModel {

    Page<AuthenticationAttempt> find(PageRequest page);
}
