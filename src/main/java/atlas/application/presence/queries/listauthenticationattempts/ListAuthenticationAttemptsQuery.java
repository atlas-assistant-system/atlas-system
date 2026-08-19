package atlas.application.presence.queries.listauthenticationattempts;

import atlas.application.presence.dto.AuthenticationAttemptDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.application.sharedkernel.paging.Page;
import atlas.application.sharedkernel.paging.PageRequest;
import atlas.domain.sharedkernel.results.Result;

public record ListAuthenticationAttemptsQuery(PageRequest pageRequest)
    implements Query<Result<Page<AuthenticationAttemptDto>>> {}
