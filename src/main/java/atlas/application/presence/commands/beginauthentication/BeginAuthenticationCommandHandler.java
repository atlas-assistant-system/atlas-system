package atlas.application.presence.commands.beginauthentication;

import atlas.application.presence.dto.ChallengeDto;
import atlas.application.presence.mappers.PresenceMapper;
import atlas.application.presence.ports.LivenessChallengeRepository;
import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.domain.presence.PresenceErrors;
import atlas.domain.presence.services.LivenessPolicy;
import java.time.Clock;
import java.util.random.RandomGenerator;
import sharedkernel.application.cqrs.CommandHandler;
import sharedkernel.domain.results.Result;

public final class BeginAuthenticationCommandHandler
    implements CommandHandler<BeginAuthenticationCommand, Result<ChallengeDto>> {

    private final PresenceUnitOfWork unitOfWork;
    private final LivenessChallengeRepository challenges;
    private final LivenessPolicy livenessPolicy;
    private final RandomGenerator random;
    private final Clock clock;

    public BeginAuthenticationCommandHandler(
        PresenceUnitOfWork unitOfWork,
        LivenessChallengeRepository challenges,
        LivenessPolicy livenessPolicy,
        RandomGenerator random,
        Clock clock) {
        this.unitOfWork = unitOfWork;
        this.challenges = challenges;
        this.livenessPolicy = livenessPolicy;
        this.random = random;
        this.clock = clock;
    }

    @Override
    public Result<ChallengeDto> handle(BeginAuthenticationCommand command) {
        var now = clock.instant();
        return unitOfWork.execute(() -> {
            if (unitOfWork.profiles().getAll().isEmpty()) {
                return Result.failure(PresenceErrors.NO_PROFILES_ENROLLED);
            }

            var challenge = livenessPolicy.issue(challenges.nextId(), random, now);
            challenges.save(challenge);

            return Result.success(PresenceMapper.toDto(challenge));
        });
    }
}
