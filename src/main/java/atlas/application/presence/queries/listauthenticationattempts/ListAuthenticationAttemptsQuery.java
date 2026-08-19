package atlas.application.presence.queries.listauthenticationattempts;

import atlas.application.presence.dto.AuthenticationAttemptDto;
import sharedkernel.application.cqrs.Query;
import sharedkernel.application.paging.Page;
import sharedkernel.application.paging.PageRequest;
import sharedkernel.domain.results.Result;

public record ListAuthenticationAttemptsQuery(PageRequest pageRequest)
    implements Query<Result<Page<AuthenticationAttemptDto>>> {}
