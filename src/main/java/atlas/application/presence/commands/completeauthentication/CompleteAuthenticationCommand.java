package atlas.application.presence.commands.completeauthentication;

import atlas.application.presence.dto.SessionDto;
import atlas.domain.presence.LivenessChallengeId;
import java.time.Instant;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record CompleteAuthenticationCommand(
    LivenessChallengeId challengeId,
    String modelVersion,
    float[] descriptor,
    String observedType,
    String nonce,
    Instant capturedAt) implements Command<Result<SessionDto>> {}
