package atlas.application.presence.queries.getauthenticationstate;

import atlas.application.presence.dto.AuthenticationStateDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;

public record GetAuthenticationStateQuery() implements Query<Result<AuthenticationStateDto>> {}
