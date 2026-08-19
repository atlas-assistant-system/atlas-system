package atlas.application.presence.commands.completeauthentication;

import atlas.application.presence.dto.SessionDto;
import atlas.application.presence.mappers.PresenceMapper;
import atlas.application.presence.ports.LivenessChallengeRepository;
import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.presence.AuthenticationGate;
import atlas.domain.presence.BiometricProfile;
import atlas.domain.presence.PresenceErrors;
import atlas.domain.presence.enums.LivenessChallengeType;
import atlas.domain.presence.enums.VerificationOutcome;
import atlas.domain.presence.services.FaceMatcher;
import atlas.domain.presence.vos.ChallengeNonce;
import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.LivenessEvidence;
import atlas.domain.presence.vos.MatchThreshold;
import atlas.domain.presence.vos.ModelVersion;
import atlas.domain.presence.vos.SessionDuration;
import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.sharedkernel.results.Error;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.Instant;

public final class CompleteAuthenticationCommandHandler
    implements CommandHandler<CompleteAuthenticationCommand, Result<SessionDto>> {

    private final PresenceUnitOfWork unitOfWork;
    private final LivenessChallengeRepository challenges;
    private final FaceMatcher faceMatcher;
    private final MatchThreshold threshold;
    private final SessionDuration sessionDuration;
    private final Clock clock;

    public CompleteAuthenticationCommandHandler(
        PresenceUnitOfWork unitOfWork,
        LivenessChallengeRepository challenges,
        FaceMatcher faceMatcher,
        MatchThreshold threshold,
        SessionDuration sessionDuration,
        Clock clock) {
        this.unitOfWork = unitOfWork;
        this.challenges = challenges;
        this.faceMatcher = faceMatcher;
        this.threshold = threshold;
        this.sessionDuration = sessionDuration;
        this.clock = clock;
    }

    @Override
    public Result<SessionDto> handle(CompleteAuthenticationCommand command) {
        var now = clock.instant();
        return unitOfWork.execute(() -> new TransactionOutcome(complete(command, now))).result();
    }

    private Result<SessionDto> complete(CompleteAuthenticationCommand command, Instant now) {
        var gates = unitOfWork.gate();
        var gate = gates.get();
        var challenge = challenges.get(command.challengeId());
        if (challenge.isEmpty()) {
            return Result.failure(PresenceErrors.challengeNotFound(command.challengeId()));
        }

        try {
            var modelResult = ModelVersion.create(command.modelVersion());
            if (modelResult.isFailure()) {
                return Result.failure(modelResult.error());
            }

            var descriptorResult = FaceDescriptor.create(modelResult.value(), command.descriptor());
            if (descriptorResult.isFailure()) {
                return Result.failure(descriptorResult.error());
            }

            var evidenceResult = evidence(command);
            if (evidenceResult.isFailure()) {
                return reject(gate, VerificationOutcome.LIVENESS_FAILED, evidenceResult.error(), now);
            }

            var consumed = challenge.get().consume(evidenceResult.value(), now);
            if (consumed.isFailure()) {
                return reject(gate, VerificationOutcome.LIVENESS_FAILED, consumed.error(), now);
            }

            var profiles = unitOfWork.profiles().getAll();
            if (profiles.isEmpty()) {
                return Result.failure(PresenceErrors.NO_PROFILES_ENROLLED);
            }

            var templates = profiles.stream().flatMap(profile -> profile.templates().stream()).toList();
            var match = faceMatcher.bestMatch(descriptorResult.value(), templates, threshold);
            if (match.isEmpty()) {
                return reject(gate, VerificationOutcome.NO_MATCH, PresenceErrors.VERIFICATION_NO_MATCH, now);
            }

            var profile = profiles.stream()
                .filter(candidate -> candidate.templates().stream()
                    .anyMatch(template -> template.id().equals(match.get().templateId())))
                .findFirst()
                .orElseThrow();

            return openSession(gate, profile, now);
        } finally {
            challenges.remove(command.challengeId());
        }
    }

    private Result<SessionDto> openSession(AuthenticationGate gate, BiometricProfile profile, Instant now) {
        var sessions = unitOfWork.sessions();
        var session = atlas.domain.presence.AuthenticationSession.open(
            sessions.nextId(), profile.id(), sessionDuration, now);
        sessions.create(session);

        gate.registerSuccess(profile.id(), session.id(), now);
        unitOfWork.gate().save(gate);

        return Result.success(PresenceMapper.toDto(session));
    }

    private Result<SessionDto> reject(
        AuthenticationGate gate, VerificationOutcome outcome, Error error, Instant now) {
        gate.registerFailure(outcome, now);
        unitOfWork.gate().save(gate);

        return Result.failure(error);
    }

    private static Result<LivenessEvidence> evidence(CompleteAuthenticationCommand command) {
        try {
            return Result.success(LivenessEvidence.of(
                ChallengeNonce.of(command.nonce()),
                LivenessChallengeType.valueOf(command.observedType()),
                command.capturedAt()));
        } catch (IllegalArgumentException | GuardException exception) {
            return Result.failure(PresenceErrors.LIVENESS_FAILED);
        }
    }

    private record TransactionOutcome(Result<SessionDto> result) {}
}
