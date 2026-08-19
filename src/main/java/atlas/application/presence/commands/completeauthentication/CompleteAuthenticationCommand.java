package atlas.application.presence.commands.completeauthentication;

import atlas.application.presence.dto.SessionDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.presence.LivenessChallengeId;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;

public record CompleteAuthenticationCommand(
    LivenessChallengeId challengeId,
    String modelVersion,
    float[] descriptor,
    String observedType,
    String nonce,
    Instant capturedAt) implements Command<Result<SessionDto>> {}
