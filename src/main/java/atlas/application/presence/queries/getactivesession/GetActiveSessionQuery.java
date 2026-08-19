package atlas.application.presence.queries.getactivesession;

import atlas.application.presence.dto.SessionDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.util.Optional;

public record GetActiveSessionQuery() implements Query<Result<Optional<SessionDto>>> {}
