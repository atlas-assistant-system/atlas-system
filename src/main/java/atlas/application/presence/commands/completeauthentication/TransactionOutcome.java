package atlas.application.presence.commands.completeauthentication;

import atlas.application.presence.dto.SessionDto;
import atlas.domain.sharedkernel.results.Result;

record TransactionOutcome(Result<SessionDto> result) {}
