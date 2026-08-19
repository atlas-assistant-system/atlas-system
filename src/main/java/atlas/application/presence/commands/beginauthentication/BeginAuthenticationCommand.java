package atlas.application.presence.commands.beginauthentication;

import atlas.application.presence.dto.ChallengeDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;

public record BeginAuthenticationCommand() implements Command<Result<ChallengeDto>> {}
