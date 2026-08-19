package atlas.application.presence.queries.getactivesession;

import atlas.application.presence.dto.SessionDto;
import java.util.Optional;
import sharedkernel.application.cqrs.Query;
import sharedkernel.domain.results.Result;

public record GetActiveSessionQuery() implements Query<Result<Optional<SessionDto>>> {}
