package atlas.application.presence.queries.getauthenticationstate;

import atlas.application.presence.dto.AuthenticationStateDto;
import sharedkernel.application.cqrs.Query;
import sharedkernel.domain.results.Result;

public record GetAuthenticationStateQuery() implements Query<Result<AuthenticationStateDto>> {}
