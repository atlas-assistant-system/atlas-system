package atlas.application.presence.queries.listauthenticationattempts;

import atlas.application.presence.dto.AuthenticationAttemptDto;
import atlas.application.presence.mappers.PresenceMapper;
import atlas.application.presence.ports.AuthenticationAttemptReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.application.sharedkernel.paging.Page;
import atlas.domain.sharedkernel.results.Result;

public final class ListAuthenticationAttemptsQueryHandler
    implements QueryHandler<ListAuthenticationAttemptsQuery, Result<Page<AuthenticationAttemptDto>>> {

    private final AuthenticationAttemptReadModel attempts;

    public ListAuthenticationAttemptsQueryHandler(AuthenticationAttemptReadModel attempts) {
        this.attempts = attempts;
    }

    @Override
    public Result<Page<AuthenticationAttemptDto>> handle(ListAuthenticationAttemptsQuery query) {
        return Result.success(attempts.find(query.pageRequest()).map(PresenceMapper::toDto));
    }
}
