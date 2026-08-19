package atlas.application.presence.ports;

import sharedkernel.application.paging.Page;
import sharedkernel.application.paging.PageRequest;

public interface AuthenticationAttemptReadModel {

    Page<AuthenticationAttempt> find(PageRequest page);
}
