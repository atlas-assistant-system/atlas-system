package atlas.application.presence.commands.beginauthentication;

import atlas.application.presence.dto.ChallengeDto;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record BeginAuthenticationCommand() implements Command<Result<ChallengeDto>> {}
